package net.Azise.pushjet.Async;

import net.Azise.pushjet.PushjetApi.PushjetService;


public interface RefreshServiceCallback {
    void onComplete(PushjetService[] services);
}
