package net.thenextlvl.tablist;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import core.api.file.format.GsonFile;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.thenextlvl.tablist.config.GlobalConfig;
import net.thenextlvl.tablist.config.ServerConfig;
import net.thenextlvl.tablist.config.TablistConfig;
import net.thenextlvl.tablist.listener.ConnectionListener;
import org.slf4j.Logger;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Accessors(fluent = true)
@Plugin(id = "tablist", name = "Tablist", authors = "NonSwag", url = "https://thenextlvl.net", version = "1.0.0")
public class TablistPlugin {
    private final MiniMessage miniMessage;
    private final GlobalConfig config;
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public TablistPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.config = new GsonFile<>(new File("plugins/Tablist", "config.json"), new GlobalConfig(
                getDefaultServerConfig(),
                getDefaultServerGroups(),
                getDefaultServerNames(),
                getDefaultGroup(),
                true,
                TimeUnit.SECONDS.toMillis(5)
        )) {{
            if (!getFile().exists()) save();
        }}.getRoot();
        this.miniMessage = MiniMessage.builder().tags(TagResolver.builder()
                .tag("global_online", (argument, context) -> Tag.inserting(Component.text(server().getPlayerCount())))
                .tag("global_max", Tag.inserting(Component.text(server().getConfiguration().getShowMaxPlayers())))
                .tag("server", Tag.inserting(Component.text(config().server().name())))
                .tag("domain", Tag.inserting(Component.text(config().server().domain())))
                .tag("discord", Tag.inserting(Component.text(config().server().discord())))
                .resolver(TagResolver.standard())
                .build()
        ).build();
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        server().getScheduler().buildTask(this, () ->
                        server().getAllPlayers().forEach(this::updateTablist))
                .repeat(Duration.ofMillis(config().refreshTime()))
                .delay(Duration.ofMillis(config().refreshTime()))
                .schedule();
        server().getEventManager().register(this, new ConnectionListener(this));
    }

    public void updateTablist(Player player) {
        player.getCurrentServer().map(ServerConnection::getServerInfo).ifPresent(server ->
                getTablist(server).ifPresent(tablist -> {
                    var tagResolver = tagResolver(player);
                    var header = miniMessage().deserialize(tablist.header(), tagResolver);
                    var footer = miniMessage().deserialize(tablist.footer(), tagResolver);
                    player.sendPlayerListHeaderAndFooter(header, footer);
                    if (!tablist.hidePlayers()) updateGlobalPlayerList(player, server);
                }));
    }

    private Optional<String> getGroup(ServerInfo server) {
        return Optional.ofNullable(config().serverGroups().get(server.getName()));
    }

    private Optional<TablistConfig> getTablist(ServerInfo server) {
        return getGroup(server).map(group -> config().groups().get(group));
    }

    private List<RegisteredServer> getServers(String group) {
        return server().getAllServers().stream()
                .filter(server -> getGroup(server.getServerInfo())
                        .map(group::equals)
                        .orElse(false))
                .toList();
    }

    public int getGroupOnlineCount(String group) {
        var online = new AtomicInteger();
        getServers(group).stream()
                .mapToInt(registeredServer -> registeredServer.getPlayersConnected().size())
                .forEach(online::addAndGet);
        return online.get();
    }

    private void updateGlobalPlayerList(Player player, ServerInfo server) {
        if (config().globalPlayerList()) server().getAllPlayers().forEach(all -> {
            if (all.equals(player)) return;
            all.getCurrentServer().map(ServerConnection::getServerInfo).ifPresent(info -> {
                if (!server.equals(info)) getTablist(info).ifPresent(tab -> {
                    if (!tab.hidePlayers()) addGlobalListEntry(player, all, info.getName());
                    addGlobalListEntry(all, player, server.getName());
                });
            });
        });
    }

    private void addGlobalListEntry(Player player, Player viewed, String server) {
        player.getTabList().addEntry(TabListEntry.builder()
                .displayName(Component.text(viewed.getUsername()).color(NamedTextColor.GRAY)
                        .append(Component.text(" » ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(config().serverNames().getOrDefault(server, server))
                                .color(NamedTextColor.GRAY)))
                .profile(viewed.getGameProfile())
                .tabList(player.getTabList())
                .gameMode(3)
                .build());
    }

    private ServerConfig getDefaultServerConfig() {
        return new ServerConfig(
                "Example",
                "example.com",
                "example.com/discord"
        );
    }

    private Map<String, String> getDefaultServerGroups() {
        return server().getAllServers().stream()
                .map(RegisteredServer::getServerInfo)
                .collect(Collectors.toMap(
                        ServerInfo::getName,
                        info -> GlobalConfig.GROUP
                ));
    }

    private Map<String, String> getDefaultServerNames() {
        return server().getAllServers().stream()
                .map(RegisteredServer::getServerInfo)
                .collect(Collectors.toMap(
                        ServerInfo::getName,
                        ServerInfo::getName
                ));
    }

    /*
    global_online           -> all players connected to the proxy
    global_max              -> max player count of the proxy

    current_server_online   -> all players connected to the current server
    current_server          -> name of the current server

    current_group_online    -> all players connected to the current server group
    current_group           -> name of the current server group

    ping                    -> the current ping of the player

    server                  -> the name of the server
    domain                  -> the domain of the server
    discord                 -> the discord of the server
    */
    private Map<String, TablistConfig> getDefaultGroup() {
        return Map.of(GlobalConfig.GROUP, new TablistConfig(
                "<newline><dark_gray>   )<gray><strikethrough>                <reset><dark_gray>[ <gray>• <white>" +
                        "<server> <gray>• <dark_gray>]<gray><strikethrough>                <reset><dark_gray>(   " +
                        "<newline><newline><gray>Server <dark_gray>» <aqua><current_server><newline><gray>Players <dark_gray>» " +
                        "<aqua><global_online><gray>/<aqua><global_max> <dark_gray>• <gray>Ping <dark_gray>» <aqua><ping>ms<newline><green>",
                "<green><newline><gray>Website <dark_gray>» <aqua><domain><dark_gray><newline><gray>Discord " +
                        "<dark_gray>» <aqua><discord><newline><dark_gray><newline><dark_gray>)<gray><strikethrough>" +
                        "                <reset><dark_gray>[ <gray>• <white><server> <gray>• <dark_gray>" +
                        "]<gray><strikethrough>                <reset><dark_gray>(<newline>",
                false
        ));
    }

    private TagResolver tagResolver(Player player) {
        var connectedServer = player.getCurrentServer().map(ServerConnection::getServer);
        var connectedGroup = connectedServer.flatMap(server -> getGroup(server.getServerInfo()));
        return TagResolver.resolver(
                TagResolver.resolver("current_server_online", Tag.inserting(Component.text(connectedServer
                        .map(server -> server.getPlayersConnected().size())
                        .orElse(0)))),
                TagResolver.resolver("current_server", Tag.inserting(Component.text(connectedServer
                        .map(server -> config().serverNames().get(server.getServerInfo().getName()))
                        .orElse("...")))),
                TagResolver.resolver("current_group_online", Tag.inserting(Component.text(connectedGroup
                        .map(this::getGroupOnlineCount)
                        .orElse(0)))),
                TagResolver.resolver("current_group", Tag.inserting(Component.text(connectedGroup
                        .orElse("")))),
                TagResolver.resolver("ping", Tag.inserting(Component.text(Math.max(0, player.getPing()))))
        );
    }
}
