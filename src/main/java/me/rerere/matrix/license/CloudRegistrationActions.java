package me.rerere.matrix.license;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.cloud.CloudServiceClient;
import me.rerere.matrix.inference.InferenceTask;
import me.rerere.matrix.cloud.CloudRegistration;

public final class CloudRegistrationActions implements CloudRegistration.Actions {
    private final Matrix matrix;

    public CloudRegistrationActions(Matrix matrix) {
        this.matrix = matrix;
    }

    @Override
    public void startInference() {
        InferenceTask inferenceTask = matrix.getInferenceTask();
        inferenceTask.enable();
    }

    @Override
    public void requestCloudMetadata() {
        CloudServiceClient cloudService = matrix.getRuntimeServices();
        cloudService.requestCloudMetadata();
    }
}
