package me.rerere.matrix.analysis;

import java.io.IOException;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.cloud.CloudServiceClient;
import me.rerere.matrix.runtime.MatrixTasks;
import me.rerere.matrix.analysis.AnalysisResult;
import me.rerere.matrix.thirdparty.okhttp3.Call;
import me.rerere.matrix.thirdparty.okhttp3.Callback;
import me.rerere.matrix.thirdparty.okhttp3.Response;
import me.rerere.matrix.thirdparty.okhttp3.ResponseBody;
import me.rerere.matrix.thirdparty.okhttp3.internal.Util;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.com.google.gson.JsonElement;
import me.rerere.matrix.thirdparty.com.google.gson.JsonObject;

/** Handles the asynchronous AI analysis response and delivers its typed result. */
public final class AiAnalysisCallback implements Callback {
    public final Function1<Object, Object> completion;
    public final CloudServiceClient client;
    public AiAnalysisCallback(CloudServiceClient client, Function1<Object, Object> completion) {
        this.client = client;
        this.completion = completion;
    }

    @Override public void onFailure(Call call, IOException failure) {
        String detail = formatAnalysisFailure(failure.getMessage());
        Matrix.Companion.logMessage(detail);
    }

    @Override public void onResponse(Call call, Response response) {
        ResponseBody body = response.body();
        String text = body == null ? null : body.string();
        JsonObject parsed = null;
        Throwable parseFailure = null;
        try {
            parsed = CloudServiceClient.jsonParserForReceiver(client).parse(text).getAsJsonObject();
        } catch (Throwable failure) {
            parseFailure = failure;
        }
        if (response.code() == 402) {
            Matrix.Companion.matrixInstance().getInferenceTask().disable();
            MatrixTasks.logger().warning("Your AI model is out of credits, please renew your subscription.");
        }
        if (!response.isSuccessful()) {
            String message = null;
            if (parseFailure == null && parsed != null) {
                JsonElement value = parsed.get("message");
                if (value != null) message = value.getAsString();
            }
            if (message == null) message = formatUnknown(text);
            throw new IllegalArgumentException(message);
        }
        AnalysisResult result = CloudServiceClient.gsonForReceiver(client).fromJson(text, AnalysisResult.class);
        completion.invoke(result);
        Util.closeQuietly((java.io.Closeable) response);
    }

    private static String formatAnalysisFailure(String message) {
        return new StringBuilder("Analysis failed: ").append(message).toString();
    }

    private static String formatUnknown(String message) {
        return new StringBuilder("unknown: ").append(message).toString();
    }

}
