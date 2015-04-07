package io.Pushjet.api.Async;

import io.Pushjet.api.PushjetApi.PushjetService;


public interface RefreshServiceCallback {
    void onComplete(PushjetService[] services);
}
