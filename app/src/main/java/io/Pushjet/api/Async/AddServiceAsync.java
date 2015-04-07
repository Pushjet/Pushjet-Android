package io.Pushjet.api.Async;


import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import io.Pushjet.api.DatabaseHandler;
import io.Pushjet.api.PushjetApi.PushjetApi;
import io.Pushjet.api.PushjetApi.PushjetException;
import io.Pushjet.api.PushjetApi.PushjetService;
import io.Pushjet.api.SubscriptionsAdapter;

public class AddServiceAsync extends AsyncTask<String, Void, PushjetService> {
    private PushjetApi api;
    private SubscriptionsAdapter adapter;
    private DatabaseHandler db;
    private PushjetException exception;

    public AddServiceAsync(PushjetApi api, DatabaseHandler db, SubscriptionsAdapter adapter) {
        this.api = api;
        this.adapter = adapter;
        this.db = db;
    }

    @Override
    protected PushjetService doInBackground(String... strings) {
        try {
            return api.newListen(strings[0]);
        } catch (PushjetException e) {
            Log.e("ServAsync", e.getMessage());
            exception = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(PushjetService service) {
        if (service != null) {
            new DownloadServiceLogoAsync(api.getContext()).execute(service);
            db.addService(service);
            adapter.addEntry(service);
        } else {
            String message = exception.getMessage();
            Toast.makeText(api.getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
