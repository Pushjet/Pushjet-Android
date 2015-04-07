package io.Pushjet.api.Async;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.widget.Toast;

import io.Pushjet.api.DatabaseHandler;
import io.Pushjet.api.PushjetApi.PushjetApi;
import io.Pushjet.api.PushjetApi.PushjetException;
import io.Pushjet.api.PushjetApi.PushjetMessage;
import io.Pushjet.api.PushjetApi.PushjetService;
import io.Pushjet.api.R;
import io.Pushjet.api.SettingsActivity;

import java.util.Date;

public class FirstLaunchAsync extends AsyncTask<Context, Void, Void> {

    @Override
    protected Void doInBackground(Context... params) {
        Context context = params[0];

        try {
            Resources resources = context.getResources();

            PushjetApi api = new PushjetApi(context, SettingsActivity.getRegisterUrl(context));
            DatabaseHandler db = new DatabaseHandler(context);

            PushjetService service;
            String serviceToken = resources.getString(R.string.pushjet_announce_service);
            try {
                service = api.newListen(serviceToken);
            } catch (PushjetException e) {
                // If it's telling us that we are already listening
                // to that service then just ignore the error
                if (e.code != 4) {
                    throw e;
                } else {
                    service = new PushjetService(serviceToken, "Pushjet Announcements", new Date());
                }
            }

            PushjetMessage message = new PushjetMessage(
                    service, resources.getString(R.string.pushjet_welcome_message),
                    resources.getString(R.string.pushjet_welcome_title), new Date()
            );

            db.addService(service);
            db.addMessage(message);

            context.sendBroadcast(new Intent("PushjetMessageRefresh"));
            new RefreshServiceAsync(api, db).execute();
        } catch (PushjetException e) {
            Toast.makeText(context, "Could not register to welcome service: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return null;
    }
}
