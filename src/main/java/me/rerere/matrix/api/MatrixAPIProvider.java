package me.rerere.matrix.api;


public class MatrixAPIProvider {
    private static MatrixAPI apiInstance = null;

    public MatrixAPIProvider() {
    }

    public static MatrixAPI getAPI() {
        if (apiInstance == null) {
            throw new IllegalStateException("ERROR#132PA");
        }
        return apiInstance;
    }

    public static void register(MatrixAPI api) {
        apiInstance = api;
    }
}
