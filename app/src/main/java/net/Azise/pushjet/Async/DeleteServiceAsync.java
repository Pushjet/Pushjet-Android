package net.Azise.pushjet.Async;


import android.os.AsyncTask;

import net.Azise.pushjet.DatabaseHandler;
import net.Azise.pushjet.PushjetApi.PushjetApi;
import net.Azise.pushjet.PushjetApi.PushjetException;
import net.Azise.pushjet.PushjetApi.PushjetService;

public class DeleteServiceAsync extends AsyncTask<PushjetService, Void, Void> {
    private DatabaseHandler db;
    private PushjetApi api;
    private GenericAsyncCallback callback;

    public DeleteServiceAsync(PushjetApi api, DatabaseHandler db) {
        this.api = api;
        this.db = db;
        this.callback = null;
    }

    public void setCallback(GenericAsyncCallback callback) {
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(PushjetService... services) {
        for (PushjetService service : services) {
            try {
                api.deleteListen(service.getToken());
                db.removeService(service);
            } catch (PushjetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (callback != null)
            callback.onComplete();
    }
}
