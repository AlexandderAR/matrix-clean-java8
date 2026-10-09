package me.rerere.matrix.crypto;

/** Holds the singleton and opaque encoded payload exposed by the original owner. */
public final class EncodedIndexMaterial {
    public static final EncodedIndexMaterial INDEX = new EncodedIndexMaterial();
    private static final String ENCODED = "TBctSVKZozZJ4gAYCparbcTYIbGxd0/ZaXbwE/JkvG6pUbA8Lv99hY6nbQCV07uozBg2j0PzpTZqstd4fhqhm2irZTv4poZPAEEkgZnnj78YIi6wM65D9b7qY40L3BKb8a0GMxqemxVDW+rqHC2H9cCAiXG2u9jvBS0fC8c4wirXIl5mSGAsnHqPSEpL7oSJwKkFubOknwx4gh28a7/XJzlPKBKlJF807LEaVifBd/XnksLVJnJJQ+YC1OOFsGfP6GfIm5TojQut7sAiire9z5O+xJgzDjTkXAkWg4u/i+dnd3pyqk8GM7twSGbMtvmgeuWskR798fnZM2V+hRo72w==";

    private EncodedIndexMaterial() {}

    public String encodedValue() {
        return ENCODED;
    }
}
