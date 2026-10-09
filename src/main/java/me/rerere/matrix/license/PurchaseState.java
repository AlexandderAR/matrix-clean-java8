package me.rerere.matrix.license;

/** Stores the purchase-state text exposed by the plugin. */
public final class PurchaseState {
    public static final PurchaseState INSTANCE = new PurchaseState();
    private static String purchaseStatus;

    private PurchaseState() {}

    public final String getStatus() { return purchaseStatus; }
    public final void setStatus(String purchaseStatus) { PurchaseState.purchaseStatus = purchaseStatus; }

    static { purchaseStatus = "NOT PURCHASED YET"; }
}
