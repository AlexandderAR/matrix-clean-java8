package me.rerere.matrix.api;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;
import me.rerere.matrix.config.ProductVersion;

/** Read-only product information returned by Matrix's API. */
public final class ApiProductInfo {
    @NotNull
    private final String description;
    @NotNull
    private final String name;
    @Nullable
    private final ProductVersion latestVersion;
    private final int id;

    public ApiProductInfo(int id, @NotNull String name, @NotNull String description,
                          @Nullable ProductVersion latestVersion) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.latestVersion = latestVersion;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (!(other instanceof ApiProductInfo)) return false;
        ApiProductInfo that = (ApiProductInfo) other;
        return id == that.id
                && Intrinsics.areEqual(name, that.name)
                && Intrinsics.areEqual(description, that.description)
                && Intrinsics.areEqual(latestVersion, that.latestVersion);
    }

    @Override
    @NotNull
    public String toString() {
        return format(id, name, description, latestVersion);
    }

    @NotNull
    public final String description() { return description; }
    @NotNull
    public final String nameValue() { return name; }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + (latestVersion == null ? 0 : latestVersion.hashCode());
        return result;
    }

    public static ApiProductInfo copyDefaults(ApiProductInfo source, int id, String name,
                                               String description, ProductVersion latestVersion,
                                               int defaultMask, Object defaultMarker) {
        if ((defaultMask & 1) != 0) id = source.id;
        if ((defaultMask & 2) != 0) name = source.name;
        if ((defaultMask & 4) != 0) description = source.description;
        if ((defaultMask & 8) != 0) latestVersion = source.latestVersion;
        return source.copy(id, name, description, latestVersion);
    }

    public final int id() { return id; }
    @Nullable
    public final ProductVersion latestVersionValue() { return latestVersion; }
    @NotNull
    public final String name() { return name; }

    private static String format(int id, String name, String description,
                                 ProductVersion latestVersion) {
        return new StringBuilder("APIProductInfoRO(id=")
                .append(id)
                .append(", name=")
                .append(name)
                .append(", description=")
                .append(description)
                .append(", latestVersion=")
                .append((Object) latestVersion)
                .append(")")
                .toString();
    }

    @Nullable
    public final ProductVersion version() { return latestVersion; }
    @NotNull
    public final String descriptionValue() { return description; }
    public final int identifier() { return id; }

    @NotNull
    public final ApiProductInfo copy(int id, @NotNull String name, @NotNull String description,
                                     @Nullable ProductVersion latestVersion) {
        return new ApiProductInfo(id, name, description, latestVersion);
    }
}
