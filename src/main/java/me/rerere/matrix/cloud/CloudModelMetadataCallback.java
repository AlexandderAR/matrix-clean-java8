package me.rerere.matrix.cloud;

import java.io.IOException;
import java.util.logging.Logger;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.runtime.MatrixTasks;
import me.rerere.matrix.thirdparty.com.google.gson.JsonObject;
import me.rerere.matrix.thirdparty.okhttp3.Call;
import me.rerere.matrix.thirdparty.okhttp3.Callback;
import me.rerere.matrix.thirdparty.okhttp3.Response;
import me.rerere.matrix.thirdparty.okhttp3.ResponseBody;

public final class CloudModelMetadataCallback implements Callback {
    private final CloudServiceClient metadata;

    public CloudModelMetadataCallback(CloudServiceClient metadata) {
        this.metadata = metadata;
    }

    @Override
    public void onResponse(Call call, Response response) {
        ResponseBody body = response.body();
        String responseBodyText = body == null ? null : body.string();

        JsonObject parsed;
        try {
            parsed = CloudServiceClient.jsonParserForReceiver(metadata)
                    .parse(responseBodyText).getAsJsonObject();
        } catch (Throwable failure) {
            failure.printStackTrace();
            return;
        }

        String modelName = parsed.get("model").getAsString();
        CloudServiceClient.setUpdateStatus(metadata, modelName);
        Logger logger = MatrixTasks.logger();
        logger.info(currentModelMessage(metadata.updateStatus()));
    }

    @Override
    public void onFailure(Call call, IOException failure) {
        String message = failure.getMessage();
        // The original long is consumed only by the discarded logger key calculation.
        Matrix.Companion.logMessage(failureMessage(message));
    }

    private static String currentModelMessage(String model) {
        return new StringBuilder("(Cloud) Current AI model: ").append(model).toString();
    }

    private static String failureMessage(String message) {
        return new StringBuilder("Failed to get model metadata: ").append(message).toString();
    }

}
