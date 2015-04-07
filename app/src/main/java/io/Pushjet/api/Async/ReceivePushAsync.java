package io.Pushjet.api.Async;

import android.os.AsyncTask;

import io.Pushjet.api.DatabaseHandler;
import io.Pushjet.api.PushListAdapter;
import io.Pushjet.api.PushjetApi.PushjetApi;
import io.Pushjet.api.PushjetApi.PushjetException;
import io.Pushjet.api.PushjetApi.PushjetMessage;

import java.util.ArrayList;
import java.util.Arrays;


public class ReceivePushAsync extends AsyncTask<Void, Void, ArrayList<PushjetMessage>> {
    private PushjetApi api;
    private PushListAdapter adapter;
    private PushjetException error;
    private ReceivePushCallback callback;

    public ReceivePushAsync(PushjetApi api, PushListAdapter adapter) {
        this.api = api;
        this.adapter = adapter;
    }

    public void setCallBack(ReceivePushCallback cb) {
        this.callback = cb;
    }

    @Override
    protected ArrayList<PushjetMessage> doInBackground(Void... voids) {
        try {
            return new ArrayList<PushjetMessage>(Arrays.asList(this.api.getNewMessage()));
        } catch (PushjetException e) {
            this.error = e;
            return new ArrayList<PushjetMessage>();
        }
    }

    @Override
    protected void onPostExecute(ArrayList<PushjetMessage> result) {
        DatabaseHandler dbh = new DatabaseHandler(this.api.getContext());
        for (PushjetMessage msg : result)
            dbh.addMessage(msg);
        adapter.addEntries(result);
        if (this.callback != null) {
            this.callback.receivePush(result);
        }
    }
}
