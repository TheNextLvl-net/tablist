package net.thenextlvl.tablist.config;

import com.google.gson.annotations.SerializedName;

public record PlayerListConfig(
        @SerializedName("enabled") boolean enabled,
        @SerializedName("transparent") boolean transparent,
        @SerializedName("format") String format
) {
}
