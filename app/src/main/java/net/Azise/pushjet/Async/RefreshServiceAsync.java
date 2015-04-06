package net.Azise.pushjet.Async;

import android.os.AsyncTask;

import net.Azise.pushjet.DatabaseHandler;
import net.Azise.pushjet.PushjetApi.PushjetApi;
import net.Azise.pushjet.PushjetApi.PushjetException;
import net.Azise.pushjet.PushjetApi.PushjetService;
import net.Azise.pushjet.SubscriptionsAdapter;


public class RefreshServiceAsync extends AsyncTask<Void, Void, PushjetService[]> {
    private DatabaseHandler db;
    private PushjetApi api;
    private RefreshServiceCallback callback;

    public RefreshServiceAsync(PushjetApi api, DatabaseHandler db) {
        this.api = api;
        this.db = db;
        this.callback = null;
    }

    public void setCallback(RefreshServiceCallback callback) {
        this.callback = callback;
    }


    @Override
    protected PushjetService[] doInBackground(Void... voids) {
        try {
            PushjetService[] listen = this.api.listListen();
            db.refreshServices(listen);
            return listen;
        } catch (PushjetException e) {
            e.printStackTrace();
        }
        return new PushjetService[0];
    }

    @Override
    protected void onPostExecute(PushjetService[] services) {
        super.onPostExecute(services);
        for (PushjetService service : services)
            new DownloadServiceLogoAsync(api.getContext()).execute(service);
        if (callback != null)
            callback.onComplete(services);
    }
}
