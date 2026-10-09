package me.rerere.matrix.network;

import java.io.IOException;
import me.rerere.matrix.thirdparty.okhttp3.Call;
import me.rerere.matrix.thirdparty.okhttp3.Callback;
import me.rerere.matrix.thirdparty.okhttp3.Response;
import me.rerere.matrix.thirdparty.okhttp3.ResponseBody;
import org.bukkit.Bukkit;

/** Broadcasts a completed HTTP response body to the Bukkit server. */
public final class HttpResponseBroadcastCallback implements Callback {
    private static final String NULL_BODY_FALLBACK = "null".intern();

    @Override
    public void onResponse(Call call, Response response) {
        ResponseBody responseBody = response.body();
        String bodyText = responseBody == null ? null : responseBody.string();
        Bukkit.broadcastMessage(bodyText == null ? NULL_BODY_FALLBACK : bodyText);
    }

    public HttpResponseBroadcastCallback() {
    }

    @Override
    public void onFailure(Call call, IOException exception) {
        exception.printStackTrace();
    }

}
