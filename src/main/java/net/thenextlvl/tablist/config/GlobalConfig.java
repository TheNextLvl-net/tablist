package net.thenextlvl.tablist.config;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * @param server the server config
 * @param serverGroups the server groups
 * @param groups the tablist groups
 */
public record GlobalConfig(
        @SerializedName("server") ServerConfig server,
        @SerializedName("global-player-list") PlayerListConfig globalPlayerList,
        @SerializedName("server-groups") Map<String, String> serverGroups,
        @SerializedName("server-names") Map<String, String> serverNames,
        @SerializedName("groups") Map<String, TablistConfig> groups,
        @SerializedName("refresh-time") long refreshTime
) {
    /**
     * The global server group name
     */
    public static final String GROUP = "global";
}
