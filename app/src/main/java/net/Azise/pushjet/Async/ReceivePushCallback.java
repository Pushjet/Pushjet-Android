package net.Azise.pushjet.Async;

import net.Azise.pushjet.PushjetApi.PushjetMessage;

import java.util.ArrayList;


public interface ReceivePushCallback {
    void receivePush(ArrayList<PushjetMessage> msg);
}
