package io.Pushjet.api;


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.Pushjet.api.PushjetApi.PushjetMessage;
import io.Pushjet.api.PushjetApi.PushjetService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class PushjetFcmListenerService extends FirebaseMessagingService {
    private static int NOTIFICATION_ID = 0;
    private static final String TAG = "PushjetGcmListeners";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String from = message.getFrom();
        Map data = message.getData();
        try {
            JSONObject AzMsg = new JSONObject((String)data.get("message"));
            JSONObject AzServ = AzMsg.getJSONObject("service");
            PushjetService srv = new PushjetService(
                    AzServ.getString("public"),
                    AzServ.getString("name"),
                    new Date((long) AzServ.getInt("created") * 1000)
            );
            srv.setIcon(AzServ.getString("icon"));

            PushjetMessage msg = new PushjetMessage(
                    srv,
                    AzMsg.getString("message"),
                    AzMsg.getString("title"),
                    AzMsg.getInt("timestamp")
            );
            msg.setLevel(AzMsg.getInt("level"));
            msg.setLink(AzMsg.getString("link"));
            DatabaseHandler db = new DatabaseHandler(this);
            db.addMessage(msg);
            sendNotification(msg);
        } catch (JSONException ignore) {
            Log.e("PushjetJson", ignore.getMessage());
        }
    }

    private void sendNotification(PushjetMessage msg) {
        NOTIFICATION_ID++;
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, PushListActivity.class);
        if (msg.hasLink()) {
            try {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.getLink()));
            } catch (Exception ignore) {
            }
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_notif)
                        .setContentTitle(msg.getTitleOrName())
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg.getMessage()))
                        .setContentText(msg.getMessage())
                        .setAutoCancel(true);

        if (msg.getService().hasIcon()) {
            try {
                Bitmap icon = msg.getService().getIconBitmap(getApplicationContext());
                Resources res = getApplicationContext().getResources();

                int nHeight = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
                int nWidth = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

                mBuilder.setLargeIcon(MiscUtil.scaleBitmap(icon, nWidth, nHeight));
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }

        setPriority(mBuilder, msg);
        mBuilder.setDefaults(Notification.DEFAULT_ALL);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setPriority(NotificationCompat.Builder mBuilder, PushjetMessage msg) {
        int priority = msg.getLevel() - 3;
        if(Math.abs(priority) > 2) {
            priority = 0;
        }

        mBuilder.setPriority(priority);
    }
}
