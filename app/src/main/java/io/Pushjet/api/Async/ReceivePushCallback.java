package io.Pushjet.api.Async;

import io.Pushjet.api.PushjetApi.PushjetMessage;

import java.util.ArrayList;


public interface ReceivePushCallback {
    void receivePush(ArrayList<PushjetMessage> msg);
}
