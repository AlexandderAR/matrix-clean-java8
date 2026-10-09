package me.rerere.matrix.config;

import java.util.Objects;

/** Immutable product version record. */
public final class ProductVersion {
    private final long id;
    private final int productId;
    private final String version;
    private final String description;
    private final long date;
    private final int downloadCount;
    private final Long file;

    public ProductVersion(long id, int productId, String version, String description,
                          long date, int downloadCount, Long file) {
        this.id = id;
        this.productId = productId;
        this.version = version;
        this.description = description;
        this.date = date;
        this.downloadCount = downloadCount;
        this.file = file;
    }

    public final int productId() { return productId; }
    public final long id() { return id; }
    public final long date() { return date; }
    public final int downloadCount() { return downloadCount; }
    public final Long file() { return file; }
    public final String version() { return version; }
    public final String description() { return description; }
    public final long idAlias() { return id; }
    public final long dateAlias() { return date; }
    public final int productIdAlias() { return productId; }
    public final ProductVersion copy(long id, int productId, String version, String description,
                                     long date, int downloadCount, Long file) {
        return new ProductVersion(id, productId, version, description, date, downloadCount, file);
    }
    public final int downloadCountAlias() { return downloadCount; }
    public final String descriptionAlias() { return description; }
    public final Long fileAlias() { return file; }
    public final String versionAlias() { return version; }

    @Override public int hashCode() {
        int result = Long.hashCode(id);
        result = 31 * result + Integer.hashCode(productId);
        result = 31 * result + version.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + Long.hashCode(date);
        result = 31 * result + Integer.hashCode(downloadCount);
        result = 31 * result + Objects.hashCode(file);
        return result;
    }

    public static ProductVersion copyWithDefaults(
            ProductVersion self, long id, int productId, String version, String description,
            long date, int downloadCount, Long file, int mask, Object ignoredMarker) {
        if ((mask & 1) != 0) id = self.id;
        if ((mask & 2) != 0) productId = self.productId;
        if ((mask & 4) != 0) version = self.version;
        if ((mask & 8) != 0) description = self.description;
        if ((mask & 16) != 0) date = self.date;
        if ((mask & 32) != 0) downloadCount = self.downloadCount;
        if ((mask & 64) != 0) file = self.file;
        return self.copy(id, productId, version, description, date, downloadCount, file);
    }

    @Override public String toString() {
        return formatRecord(id, productId, version, description, date, downloadCount, file);
    }

    private static String formatRecord(long id, int productId, String version, String description,
                                       long date, int downloadCount, Long file) {
        return new StringBuilder("ProductVersion(id=").append(id)
                .append(", productId=").append(productId)
                .append(", version=").append(version)
                .append(", description=").append(description)
                .append(", date=").append(date)
                .append(", downloadCount=").append(downloadCount)
                .append(", file=").append(file).append(')').toString();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProductVersion)) return false;
        ProductVersion that = (ProductVersion) other;
        return id == that.id && productId == that.productId
                && Objects.equals(version, that.version) && Objects.equals(description, that.description)
                && date == that.date && downloadCount == that.downloadCount && Objects.equals(file, that.file);
    }
}
