package net.thenextlvl.tablist.config;

import com.google.gson.annotations.SerializedName;

/**
 * @param header the header of the tablist
 * @param footer the footer of the tablist
 * @param hidePlayers whether to hide the players of this tablist from the global one
 */
public record TablistConfig(
        @SerializedName("header") String header,
        @SerializedName("footer") String footer,
        @SerializedName("hide-players") boolean hidePlayers
) {
}
