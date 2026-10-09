package me.rerere.matrix.api.notification;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.license.identity.Author;

/** Immutable value object for a Discord embed. */
public final class DiscordEmbed {
    private final String type;
    private final String title;
    private final String description;
    private final int color;
    private final String url;
    private final Author author;
    private final List<?> fields;

    public DiscordEmbed(String type, String title, String description, int color,
                        String url, Author author,
                        List<?> fields) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.color = color;
        this.url = url;
        this.author = author;
        this.fields = fields;
    }

    public String type() { return type; }
    public String title() { return title; }
    public String description() { return description; }
    public int color() { return color; }
    public String url() { return url; }
    public Author author() { return author; }
    public List<?> fields() { return fields; }

    public String component1() { return type; }
    public String component2() { return title; }
    public String component3() { return description; }
    public int component4() { return color; }
    public String component5() { return url; }
    public Author component6() { return author; }
    public List<?> component7() { return fields; }

    public DiscordEmbed copy(String type, String title, String description, int color,
                             String url, Author author,
                             List<?> fields) {
        return new DiscordEmbed(type, title, description, color, url, author, fields);
    }

    public static DiscordEmbed create(String type, String title, String description, int color,
                                      String url, Author author,
                                      List<?> fields) {
        return new DiscordEmbed(type, title, description, color, url, author, fields);
    }

    private static String format(String type, String title, String description, int color,
                                 String url, Author author,
                                 List<?> fields) {
        return "DiscordEmbed(type=" + type + ", title=" + title + ", description=" + description
                + ", color=" + color + ", url=" + url + ", author=" + author
                + ", fields=" + fields + ")";
    }

    public static DiscordEmbed copy$default(DiscordEmbed self, String type, String title,
            String description, int color, String url,
            Author author, List<?> fields, int mask, Object marker) {
        if ((mask & 1) != 0) type = self.type;
        if ((mask & 2) != 0) title = self.title;
        if ((mask & 4) != 0) description = self.description;
        if ((mask & 8) != 0) color = self.color;
        if ((mask & 16) != 0) url = self.url;
        if ((mask & 32) != 0) author = self.author;
        if ((mask & 64) != 0) fields = self.fields;
        return self.copy(type, title, description, color, url, author, fields);
    }

    public DiscordEmbed(String type, String title, String description, int color,
            String url, Author author, List<?> fields,
            int mask, me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) {
        this(type, title, description, color, (mask & 16) != 0 ? null : url,
                (mask & 32) != 0 ? null : author, (mask & 64) != 0 ? null : fields);
    }

    @Override public String toString() {
        return format(type, title, description, color, url, author, fields);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DiscordEmbed)) return false;
        DiscordEmbed otherEmbed = (DiscordEmbed) other;
        return Intrinsics.areEqual(type, otherEmbed.type)
                && Intrinsics.areEqual(title, otherEmbed.title)
                && Intrinsics.areEqual(description, otherEmbed.description)
                && color == otherEmbed.color
                && Intrinsics.areEqual(url, otherEmbed.url)
                && Intrinsics.areEqual(author, otherEmbed.author)
                && Intrinsics.areEqual(fields, otherEmbed.fields);
    }

    @Override public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + Integer.hashCode(color);
        result = 31 * result + (url == null ? 0 : url.hashCode());
        result = 31 * result + (author == null ? 0 : author.hashCode());
        result = 31 * result + (fields == null ? 0 : fields.hashCode());
        return result;
    }
}
