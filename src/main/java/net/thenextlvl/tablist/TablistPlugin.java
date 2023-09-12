package net.thenextlvl.tablist;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPermsProvider;
import net.thenextlvl.tablist.config.GlobalConfig;
import net.thenextlvl.tablist.config.PlayerListConfig;
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
@Plugin(id = "tablist",
        name = "Tablist",
        authors = "NonSwag",
        url = "https://thenextlvl.net",
        // version = "${version}", // figure out how to do that
        version = "1.1.2",
        dependencies = {
                @Dependency(
                        id = "luckperms",
                        optional = true
                )
        })
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
                getDefaultPlayerListConfig(),
                getDefaultServerGroups(),
                getDefaultServerNames(),
                getDefaultGroup(),
                TimeUnit.SECONDS.toMillis(5)
        )) {{
            if (!getFile().exists()) save();
        }}.getRoot();
        this.miniMessage = MiniMessage.builder().tags(TagResolver.builder()
                .tag("global_online", (argument, context) -> Tag.preProcessParsed(String.valueOf(server().getPlayerCount())))
                .tag("global_max", Tag.preProcessParsed(String.valueOf(server().getConfiguration().getShowMaxPlayers())))
                .tag("server", Tag.preProcessParsed(config().server().name()))
                .tag("domain", Tag.preProcessParsed(config().server().domain()))
                .tag("discord", Tag.preProcessParsed(config().server().discord()))
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
                    updateLocalPlayerList(player, tablist);
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

    private void updateLocalPlayerList(Player player, TablistConfig tablist) {
        if (tablist.playerFormat() == null || tablist.playerFormat().isBlank()) return;
        server().getAllPlayers().forEach(all -> {
            addLocalPlayerListEntry(player, all, tablist.playerFormat());
            if (!all.equals(player)) addLocalPlayerListEntry(all, player, tablist.playerFormat());
        });
    }

    private void addLocalPlayerListEntry(Player player, Player viewed, String format) {
        player.getTabList().getEntry(viewed.getUniqueId()).orElse(TabListEntry.builder()
                        .profile(viewed.getGameProfile())
                        .tabList(player.getTabList())
                        .build())
                .setDisplayName(miniMessage().deserialize(format, tagResolver(viewed)))
                .setLatency((int) Math.max(0, viewed.getPing()));
    }

    private void updateGlobalPlayerList(Player player, ServerInfo server) {
        if (config().globalPlayerList().enabled()) server().getAllPlayers().forEach(all -> {
            if (all.equals(player)) return;
            all.getCurrentServer().map(ServerConnection::getServerInfo).ifPresent(info -> {
                if (!server.equals(info)) getTablist(info).ifPresent(tab -> {
                    if (!tab.hidePlayers()) addGlobalListEntry(player, all);
                    addGlobalListEntry(all, player);
                });
            });
        });
    }

    private void addGlobalListEntry(Player player, Player viewed) {
        player.getTabList().getEntry(viewed.getUniqueId())
                .orElse(TabListEntry.builder()
                        .profile(viewed.getGameProfile())
                        .tabList(player.getTabList())
                        .build())
                .setDisplayName(miniMessage().deserialize(
                        config().globalPlayerList().format(),
                        tagResolver(viewed)
                ))
                .setGameMode(config().globalPlayerList().transparent() ? 3 : 2)
                .setLatency((int) Math.max(0, viewed.getPing()));
    }

    private ServerConfig getDefaultServerConfig() {
        return new ServerConfig(
                "Example",
                "example.com",
                "example.com/discord"
        );
    }

    private PlayerListConfig getDefaultPlayerListConfig() {
        return new PlayerListConfig(
                true, true, "<gray><player> <dark_gray>» <gray><current_server>"
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
                "<prefix><player><suffix>",
                false
        ));
    }

    private TagResolver tagResolver(Player player) {
        return luckResolver(player).resolvers(playerResolver(player)).build();
    }

    private TagResolver playerResolver(Player player) {
        var connectedServer = player.getCurrentServer().map(ServerConnection::getServer);
        var connectedGroup = connectedServer.flatMap(server -> getGroup(server.getServerInfo()));
        return TagResolver.builder().resolvers(
                TagResolver.resolver("current_server_online", Tag.preProcessParsed(String.valueOf(connectedServer
                        .map(server -> server.getPlayersConnected().size())
                        .orElse(0)))),
                TagResolver.resolver("current_server", Tag.preProcessParsed(connectedServer
                        .map(server -> config().serverNames().get(server.getServerInfo().getName()))
                        .orElse("..."))),
                TagResolver.resolver("current_group_online", Tag.preProcessParsed(String.valueOf(connectedGroup
                        .map(this::getGroupOnlineCount)
                        .orElse(0)))),
                TagResolver.resolver("current_group", Tag.preProcessParsed(connectedGroup.orElse(""))),
                TagResolver.resolver("player", Tag.preProcessParsed(player.getUsername())),
                TagResolver.resolver("ping", Tag.preProcessParsed(String.valueOf(Math.max(0, player.getPing()))))
        ).build();
    }

    private TagResolver.Builder luckResolver(Player player) {
        if (!server().getPluginManager().isLoaded("luckperms")) return TagResolver.builder();
        var user = LuckPermsProvider.get().getPlayerAdapter(Player.class).getUser(player);
        var group = LuckPermsProvider.get().getGroupManager().getGroup(user.getPrimaryGroup());
        var meta = user.getCachedData().getMetaData(user.getQueryOptions());
        var groupName = group != null ? group.getDisplayName() != null ? group.getDisplayName() : group.getName() : "";
        var prefix = meta.getPrefix() != null ? meta.getPrefix() : "";
        var suffix = meta.getSuffix() != null ? meta.getSuffix() : "";
        return TagResolver.builder().resolvers(
                TagResolver.resolver("group", Tag.preProcessParsed(groupName)),
                TagResolver.resolver("prefix", Tag.preProcessParsed(prefix)),
                TagResolver.resolver("suffix", Tag.preProcessParsed(suffix))
        );
    }
}
