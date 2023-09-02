package net.thenextlvl.tablist.config;

import com.google.gson.annotations.SerializedName;

/**
 * @param name the name of the server
 * @param domain the domain of the server
 * @param discord the discord of the server
 */
public record ServerConfig(
        @SerializedName("name") String name,
        @SerializedName("domain") String domain,
        @SerializedName("discord") String discord
) {
}
