package net.thenextlvl.tablist.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import net.thenextlvl.tablist.TablistPlugin;

import static com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN;

public class ConnectionListener {
    private final TablistPlugin plugin;

    public ConnectionListener(TablistPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerConnect(ServerConnectedEvent event) {
        plugin.server().getAllPlayers().forEach(plugin::updateTablist);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (!event.getLoginStatus().equals(SUCCESSFUL_LOGIN)) return;
        plugin.server().getAllPlayers().forEach(player -> {
            plugin.updateTablist(player);
            if (!plugin.config().globalPlayerList().enabled()) return;
            player.getTabList().removeEntry(event.getPlayer().getUniqueId());
        });
    }
}
