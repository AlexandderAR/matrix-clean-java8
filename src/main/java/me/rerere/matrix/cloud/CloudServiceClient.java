package me.rerere.matrix.cloud;

import java.lang.reflect.Method;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import me.rerere.matrix.thirdparty.com.google.gson.Gson;
import me.rerere.matrix.thirdparty.com.google.gson.JsonElement;
import me.rerere.matrix.thirdparty.com.google.gson.JsonObject;
import me.rerere.matrix.thirdparty.com.google.gson.JsonParser;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.io.FilesKt;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.okhttp3.HttpUrl;
import me.rerere.matrix.thirdparty.okhttp3.OkHttpClient;
import me.rerere.matrix.thirdparty.okhttp3.OkHttpClient$Builder;
import me.rerere.matrix.thirdparty.okhttp3.Request;
import me.rerere.matrix.thirdparty.okhttp3.Request$Builder;
import me.rerere.matrix.thirdparty.okhttp3.RequestBody;
import me.rerere.matrix.thirdparty.okhttp3.Response;
import me.rerere.matrix.thirdparty.okhttp3.ResponseBody;
import me.rerere.matrix.analysis.AiAnalysisCallback;
import me.rerere.matrix.api.ApiProductInfo;
import me.rerere.matrix.checks.AnalysisBatch;
import me.rerere.matrix.analysis.AnalysisResult;
import me.rerere.matrix.network.HttpApiConfiguration;
import me.rerere.matrix.util.BuildMetadata;
import me.rerere.matrix.version.VersionText;
import me.rerere.matrix.config.ProductVersion;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.cloud.CloudModelMetadataCallback;
import org.bukkit.Bukkit;

/** Cloud metadata, analysis, and product update requests. */
public final class CloudServiceClient {
    private final Lazy<OkHttpClient> httpClient;
    private final Lazy<Gson> gson;
    private final Lazy<JsonParser> jsonParser;
    private String updateStatus;
    public CloudServiceClient() {
        this.httpClient = LazyKt.lazy(CloudServiceClient::createHttpClient);
        this.updateStatus = "NOT_LOAD";
        this.gson = LazyKt.lazy(CloudServiceClient::createGson);
        this.jsonParser = LazyKt.lazy(CloudServiceClient::createJsonParser);
    }

    public String updateStatus() { return updateStatus; }

    public static void setUpdateStatus(CloudServiceClient client, String status) {
        client.updateStatus = status;
    }

    private static String formatUpdateRequestVersion(String version) {
        return "Requesting update for version " + version;
    }
    private static String formatResponseLength(long length) { return "Response length: " + length; }
    private static String updateJarFileName(String version) {
        return new StringBuilder(version).append(".jar").toString();
    }
    private static String formatResponseCode(int code) { return "Response code: " + code; }
    private static String formatUnknown(String value) { return "unknown: " + value; }
    private static String productUrl(int productId) {
        return "https://localhost/offline/api/pub/v1/product/" + productId;
    }

    private static Method findDeclaredMethod(Class<?> owner, String name, Class<?> returnType,
                                             int parameterCount, Class<?>[] parameterTypes) {
        for (Method method : owner.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getReturnType() != returnType) continue;
            Class<?>[] actualParameters = method.getParameterTypes();
            if (actualParameters.length != parameterCount) continue;
            int index = 0;
            while (index < parameterCount && actualParameters[index] == parameterTypes[index]) index++;
            if (index == parameterCount) return method;
        }
        return null;
    }

    private static Method findMethodInInterfaces(Class<?> owner, String name, Class<?> returnType,
                                                 int parameterCount, Class<?>[] parameterTypes) {
        Method declared = findDeclaredMethod(owner, name, returnType, parameterCount, parameterTypes);
        if (declared != null) return declared;
        for (Class<?> implementedInterface : owner.getInterfaces()) {
            Method inherited = findMethodInInterfaces(implementedInterface, name, returnType,
                    parameterCount, parameterTypes);
            if (inherited != null) return inherited;
        }
        return null;
    }

    private static OkHttpClient createHttpClient() {
        return new OkHttpClient$Builder().connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build();
    }

    private static Gson createGson() { return new Gson(); }
    private static JsonParser createJsonParser() { return new JsonParser(); }

    public OkHttpClient httpClient() { return httpClient.getValue(); }
    private Gson gson() { return gson.getValue(); }
    private JsonParser jsonParser() { return jsonParser.getValue(); }
    public static Gson gsonForReceiver(CloudServiceClient receiver) {
        return receiver.gson();
    }
    public static JsonParser jsonParserForReceiver(CloudServiceClient receiver) {
        return receiver.jsonParser();
    }

    private static String formatProgress(long receivedKilobytes, long totalKilobytes,
                                         int percentage) {
        return "Downloading update: " + receivedKilobytes + "KB / " + totalKilobytes
                + "KB (" + percentage + "%)";
    }
    /** ASM request path: asynchronous GET; callback class is resolved by its descriptor token. */
    public void requestCloudMetadata() {
        Request$Builder request = new Request$Builder().url("https://localhost/offline/api/cloud/v1/metadata")
                .get();
        httpClient().newCall(request.build()).enqueue(new CloudModelMetadataCallback(this));
    }

    @SuppressWarnings("unchecked")
    public void submitAnalysis(AnalysisBatch analysis,
                              Function1<AnalysisResult, Unit> callback) {
        String json = gson().toJson(analysis);
        Request request = new Request$Builder()
                .url("https://localhost/offline/api/cloud/v1/analysis")
                .header("Authorization", BuildMetadata.LICENSE_METADATA.getAuthorizationToken())
                .post(RequestBody.Companion.create(json,
                        HttpApiConfiguration.mediaType()))
                .build();
        httpClient().newCall(request).enqueue(new AiAnalysisCallback(this,
                (Function1<Object, Object>) (Function1<?, ?>) callback));
    }

    /** Executes the product metadata GET and returns the Kotlin Result representation. */
    public Object fetchProductInfo(int productId) {
        try {
            Request request = new Request$Builder().url(productUrl(productId)).build();
            Response response = httpClient().newCall(request).execute();
            ResponseBody responseBody = response.body();
            String responseText = responseBody == null ? null : responseBody.string();

            JsonObject parsed = null;
            try {
                parsed = jsonParser().parse(responseText).getAsJsonObject();
            } catch (Throwable ignoredParseFailure) {
                // ASM catches parser failures here and continues with the raw response text.
            }

            if (!response.isSuccessful()) {
                String message = null;
                if (parsed != null) {
                    JsonElement errorElement = parsed.get("error");
                    if (errorElement != null) {
                        String errorMessage = errorElement.getAsString();
                        if (errorMessage != null) message = errorMessage;
                    }
                }
                if (message == null) message = formatUnknown(responseText);
                throw new IllegalArgumentException(message);
            }
            ApiProductInfo productInfo = gson().fromJson(responseText, ApiProductInfo.class);
            return productInfo;
        } catch (Throwable failure) {
            return ResultKt.createFailure(failure);
        }
    }

    /** Synchronous update download and installation handoff. */
    public Object downloadLatestProduct(int productId, String token, Function1 progress) {
        try {
            boolean compatibilityMode = AxisAlignedBox.isMode();
            progress.invoke("Checking for updates...");

            Object infoResult = fetchProductInfo(productId);
            try {
                ResultKt.throwOnFailure(infoResult);
            } catch (Throwable ignoredProductFailure) {
                throw new IllegalStateException("Failed to get product info");
            }
            ApiProductInfo productInfo = (ApiProductInfo) infoResult;
            if (productInfo == null) throw new IllegalStateException("Failed to get product info");

            Matrix matrix = Matrix.Companion.matrixInstance();
            String installedVersion = VersionText.identityVersionText(
                    matrix.getDescription().getVersion());
            ProductVersion latest = productInfo.version();
            Intrinsics.checkNotNull(latest);
            String latestVersion = VersionText.identityVersionText(latest.version());

            if (!compatibilityMode
                    && VersionText.compareVersionTexts(installedVersion, latestVersion) >= 0) {
                progress.invoke("No updates available");
                return Unit.INSTANCE;
            }
            progress.invoke(formatUpdateRequestVersion(latest.version()));

            HttpUrl downloadUrl = HttpUrl.Companion.get(
                    "https://localhost/offline/api/pub/v1/download-latest-product")
                    .newBuilder().addQueryParameter("token", token)
                    .addQueryParameter("product", String.valueOf(productId)).build();
            Request request = new Request$Builder().url(downloadUrl).get().build();
            Response response = httpClient().newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IllegalStateException(formatResponseCode(response.code()));
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) throw new IllegalStateException("Failed to get update");
            InputStream updateStream = responseBody.byteStream();
            long contentLength = responseBody.contentLength();
            if (contentLength <= 0L) throw new IllegalStateException(formatResponseLength(contentLength));

            File updatesDirectory = FilesKt.resolve(Matrix.Companion.matrixInstance().getDataFolder(), "updates");
            if (!updatesDirectory.exists()) updatesDirectory.mkdirs();
            File downloadedFile = FilesKt.resolve(updatesDirectory,
                    updateJarFileName(latest.version()));
            if (downloadedFile.exists()) downloadedFile.delete();
            downloadedFile.createNewFile();

            FileOutputStream output = new FileOutputStream(downloadedFile);
            Throwable outputFailure = null;
            try {
                long bytesWritten = 0L;
                byte[] buffer = new byte[8192];
                int bytesRead = updateStream.read(buffer);
                int lastReportedPercentage = 0;
                while (bytesRead >= 0) {
                    output.write(buffer, 0, bytesRead);
                    bytesWritten += bytesRead;
                    int percentage = (int) (bytesWritten * 100L / contentLength);
                    if (Math.abs(percentage - lastReportedPercentage) >= 10) {
                        progress.invoke(formatProgress(bytesWritten / 1024L,
                                contentLength / 1024L, percentage));
                        lastReportedPercentage = percentage;
                    }
                    bytesRead = updateStream.read(buffer);
                }
            } catch (Throwable failure) {
                outputFailure = failure;
                throw failure;
            } finally {
                CloseableKt.closeFinally(output, outputFailure);
            }

            File updateDirectory = Bukkit.getServer().getUpdateFolderFile();
            File destination = updateDirectory;
            if (!compatibilityMode) {
                if (!updateDirectory.exists()) updateDirectory.mkdirs();
                destination = FilesKt.resolve(Bukkit.getServer().getUpdateFolderFile(),
                        Matrix.Companion.matrixInstance().getPluginFile().getName());
                if (destination.exists()) destination.delete();
            }
            downloadedFile.renameTo(destination);
            return Unit.INSTANCE;
        } catch (Throwable failure) {
            return ResultKt.createFailure(failure);
        }
    }

    /** Bit zero of options redirects the requested product to product 1. */
    private static Object dispatchUpdate(CloudServiceClient receiver, int productId,
                                         String token, Function1 progress, int options) {
        if ((options & 1) != 0) productId = 1;
        return receiver.downloadLatestProduct(productId, token, progress);
    }
}
