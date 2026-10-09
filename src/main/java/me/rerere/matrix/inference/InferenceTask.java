package me.rerere.matrix.inference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import me.rerere.matrix.compat.KotlinTimeAccess;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.analysis.AnalysisRequest;
import me.rerere.matrix.checks.AnalysisBatch;
import me.rerere.matrix.checks.movement.MovementSampleCheck;
import me.rerere.matrix.commands.DeveloperCommand;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.inference.AnalysisResultItem;
import me.rerere.matrix.inference.InferenceIntervals;
import me.rerere.matrix.network.HttpResponseBroadcastCallback;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.time.TimeMarkOperations;
import me.rerere.matrix.util.concurrent.InferenceTaskReference;
import me.rerere.matrix.analysis.AnalysisResult;
import me.rerere.matrix.thirdparty.com.google.gson.Gson;
import me.rerere.matrix.thirdparty.kotlin.Result;
import me.rerere.matrix.thirdparty.kotlin.Result$Failure;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.okhttp3.MediaType;
import me.rerere.matrix.thirdparty.okhttp3.OkHttpClient;
import me.rerere.matrix.thirdparty.okhttp3.OkHttpClient$Builder;
import me.rerere.matrix.thirdparty.okhttp3.Request;
import me.rerere.matrix.thirdparty.okhttp3.Request$Builder;
import me.rerere.matrix.thirdparty.okhttp3.RequestBody;

public final class InferenceTask implements Runnable {
    private final ArrayList<AnalysisRequest> pending;
    private boolean enabled;
    private long lastFlushMark;

    public InferenceTask() {
        lastFlushMark = TimeMarkOperations.markNow();
        pending = new ArrayList<>();
    }

    public final void enable() { enabled = true; }
    public final void disable() {
        enabled = false;
    }
    public final boolean isEnabled() { return enabled; }

    public final void schedule() {
        FoliaSupport.INSTANCE.runTaskTimerAsync(new InferenceTaskReference(this), 20L, 20L);
    }

    public final void reset() {
        lastFlushMark = TimeMarkOperations.markNow();
        pending.clear();
    }

    @Override public void run() {
        if (KotlinTimeAccess.compareDurations(KotlinTimeAccess.elapsedNow(lastFlushMark),
                InferenceIntervals.getUpdateInterval()) >= 0) {
            flush();
            reset();
        }
    }

    public final void add(AnalysisRequest sample) {
        pending.add(sample);
        if (pending.size() >= batchSize()) {
            flush();
            reset();
        }
    }

    private int batchSize() {
        boolean localInference = DeveloperCommand.bedrockModeEnabled;
        return localInference ? 1 : 3;
    }

    private Object flush() {
        Object coroutineResult;
        try {
            Object resultCompanion = Result.Companion;
            if (!pending.isEmpty()) {
                AnalysisBatch batch = new AnalysisBatch(CollectionsKt.toList((Iterable<AnalysisRequest>) pending));
                Matrix.getInstance().getRuntimeServices()
                        .submitAnalysis(batch, InferenceTask::acceptResponse);
                if (DeveloperCommand.bedrockModeEnabled) {
                    Gson gson = new Gson();
                    OkHttpClient client = new OkHttpClient$Builder()
                            .connectTimeout(500L, TimeUnit.MILLISECONDS).build();
                    for (Object value : (Iterable<?>) batch.component1()) {
                        AnalysisRequest sample = (AnalysisRequest) value;
                        Request request = new Request$Builder().url("http://localhost:8000/infer")
                                .post(RequestBody.Companion.create(gson.toJson(sample), MediaType.Companion.parse("application/json")))
                                .build();
                        client.newCall(request).enqueue(new HttpResponseBroadcastCallback());
                    }
                }
            }
            coroutineResult = Unit.INSTANCE;
        } catch (Throwable failure) {
            Object resultCompanion = Result.Companion;
            coroutineResult = ResultKt.createFailure(failure);
        }
        if (coroutineResult instanceof Result$Failure) {
            ((Result$Failure) coroutineResult).exception.printStackTrace();
        }
        return coroutineResult;
    }

    private static Unit acceptResponse(AnalysisResult response) {
        Iterable<?> analysisResults = (Iterable<?>) response.component1();
        Iterator<?> iterator = analysisResults.iterator();
        while (iterator.hasNext()) {
            AnalysisResultItem analysisResult = (AnalysisResultItem) iterator.next();
            PlayerCheckContext context = Matrix.getInstance().getPlayerDataManager()
                    .find(analysisResult.getOwner());
            if (context != null) {
                MovementSampleCheck check = (MovementSampleCheck) context.checks()
                        .firstCheckOfType(MovementSampleCheck.class);
                if (check != null) check.onSample(analysisResult);
            }
        }
        return Unit.INSTANCE;
    }
}
