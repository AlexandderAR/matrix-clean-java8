package me.rerere.matrix.discord;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.api.notification.DiscordEmbed;

public final class DiscordMessage {
    private final String content;
    private final List<DiscordEmbed> embeds;

    public DiscordMessage(String content, List<DiscordEmbed> embeds) {
        this.content = content;
        this.embeds = embeds;
    }

    public DiscordMessage(String content, List<DiscordEmbed> embeds,
                          int defaultMask, DefaultConstructorMarker unusedMarker) {
        this((defaultMask & 1) != 0 ? null : content, embeds);
    }

    public final String getContent() {
        return content;
    }

    public final List<DiscordEmbed> getEmbeds() {
        return embeds;
    }

    public final String componentContent() {
        return content;
    }

    public final List<DiscordEmbed> componentEmbeds() {
        return embeds;
    }

    public final DiscordMessage copy(String content, List<DiscordEmbed> embeds) {
        return new DiscordMessage(content, embeds);
    }

    public static DiscordMessage copyDefaults(DiscordMessage message, String content,
                                              List<DiscordEmbed> embeds, int defaultMask, Object unusedMarker) {
        if ((defaultMask & 1) != 0) {
            content = message.content;
        }
        if ((defaultMask & 2) != 0) {
            embeds = message.embeds;
        }
        return message.copy(content, embeds);
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) {
            return true;
        }
        if (!(candidate instanceof DiscordMessage)) {
            return false;
        }
        DiscordMessage other = (DiscordMessage) candidate;
        if (!Intrinsics.areEqual(content, other.content)) {
            return false;
        }
        return Intrinsics.areEqual(embeds, other.embeds);
    }

    @Override
    public int hashCode() {
        int result = content == null ? 0 : content.hashCode();
        result = result * 31 + (embeds == null ? 0 : embeds.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder("DiscordMessage(content=")
                .append(content)
                .append(", embeds=")
                .append(embeds)
                .append(')')
                .toString();
    }
}
