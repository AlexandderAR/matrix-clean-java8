package me.rerere.matrix.license.identity;

import java.util.Objects;

/** Immutable author name and icon URL value reconstructed from the class methods. */
public final class Author {
    private final String name;
    private final String iconUrl;

    public Author(String name, String iconUrl) {
        this.name = name;
        this.iconUrl = iconUrl;
    }

    public final String getName() { return name; }
    public final String getIconUrl() { return iconUrl; }
    public final String component1() { return name; }
    public final String component2() { return iconUrl; }

    /** Kotlin data-class copy semantics; mask bit 0/1 selects the existing field. */
    public final Author copy(String name, String iconUrl) { return new Author(name, iconUrl); }
    public static Author copy$default(Author self, String name, String iconUrl, int mask, Object marker) {
        if ((mask & 1) != 0) name = self.name;
        if ((mask & 2) != 0) iconUrl = self.iconUrl;
        return self.copy(name, iconUrl);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Author)) return false;
        Author that = (Author) other;
        return Objects.equals(name, that.name) && Objects.equals(iconUrl, that.iconUrl);
    }

    @Override public int hashCode() { return 31 * name.hashCode() + iconUrl.hashCode(); }
    @Override public String toString() { return formatAuthor(name, iconUrl); }
    private static String formatAuthor(String name, String iconUrl) {
        return "Author(name=" + name + ", icon_url=" + iconUrl + ")";
    }
}
