package io.Pushjet.api.Async;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import io.Pushjet.api.HttpUtil;
import io.Pushjet.api.PushjetApi.DeviceUuidFactory;
import io.Pushjet.api.PushjetFirebaseInstanceIDService;
import io.Pushjet.api.SettingsActivity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PushjetRegistrationService extends Service {
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_UNREGISTER = "unregister";
    public static final String PROPERTY_FCM_TOKEN = "fcm_token";
    private static final String PROPERTY_APP_VERSION = "app_version";
    protected String TAG = "PushjetRegSvc";
    private Context mContext = this;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Set<String> keySet = intent.getExtras().keySet();
        Boolean force = false;

        Log.d(TAG, "onStartCommand");
        if (keySet.contains(PROPERTY_UNREGISTER)) {
            forgetRegistration();
            force = true;
        }

        if (keySet.contains(PROPERTY_FCM_TOKEN)) {
            String regId = String.valueOf(intent.getStringExtra(PROPERTY_FCM_TOKEN));
            storeToken(regId);
            Log.d(TAG, "Token Registered.");
        }

        registerInBackground(force);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void storeToken(String token) {
        final SharedPreferences prefs = getGcmPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(PROPERTY_FCM_TOKEN, token);
        editor.apply();
    }

    private void storeRegistrationId(String regId) {
        final SharedPreferences prefs = getGcmPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, getAppVersion());
        editor.apply();
    }

    public String getRegistrationId() {
        final SharedPreferences prefs = getGcmPreferences(mContext);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty() || registrationId.equals(""))
            return "";

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        if (registeredVersion != getAppVersion())
            return "";
        return registrationId;
    }

    private String getToken() {
        return getGcmPreferences(mContext).getString(PROPERTY_FCM_TOKEN, "");
    }

    public int getAppVersion() {
        try {
            PackageInfo packageInfo = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private SharedPreferences getGcmPreferences(Context context) {
        return context.getSharedPreferences(PushjetFirebaseInstanceIDService.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    public boolean shouldRegister() {
        return getRegistrationId().equals("");
    }

    public void forgetRegistration() {
        final SharedPreferences prefs = getGcmPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PROPERTY_REG_ID);
        editor.remove(PROPERTY_APP_VERSION);
        editor.commit();
    }

    public AsyncRegistrar registerInBackground() {
        return this.registerInBackground(false);
    }

    public AsyncRegistrar registerInBackground(boolean force) {
        AsyncRegistrar task = new AsyncRegistrar();
        task.execute(force);
        return task;
    }

    private static boolean asyncAlreadyRunning = false;
    public class AsyncRegistrar extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected Void doInBackground(Boolean... params) {
            if(asyncAlreadyRunning) {
                return null;
            }

            asyncAlreadyRunning = true;
            boolean force = params.length > 0 && params[0];
            if (!force && shouldRegister()) {
                Log.i(TAG, "Already registered, no need to re-register");
                asyncAlreadyRunning = false;
                return null; // No need to re-register
            }

            String url = SettingsActivity.getRegisterUrl(mContext) + "/gcm";
            String senderId = SettingsActivity.getSenderId(mContext);
            Looper.prepare();

            try {
                String regid = getToken();

                Map<String, String> data = new HashMap<String, String>();
                data.put("regId", regid);
                data.put("uuid", new DeviceUuidFactory(mContext).getDeviceUuid().toString());
                for (int i = 1; i <= 10; i++) {
                    Log.i(TAG, "Attempt #" + i + " to register device");
                    try {
                        HttpUtil.Post(url, data);
                        break;
                    } catch (Exception ignore) {
                        Log.e(TAG, ignore.getMessage());
                        if (i == 10)
                            throw new IOException();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                storeRegistrationId(regid);
            } catch (IOException ignore) {
                Toast.makeText(mContext, "Could not register with GCM server", Toast.LENGTH_SHORT).show();
            }

            Log.i(TAG, "Registered!");
            if (force) {
                Toast.makeText(mContext, "Registered to " + url, Toast.LENGTH_SHORT).show();
            }

            Log.i(TAG, "Registered to " + url);
            asyncAlreadyRunning = false;
            return null;
        }
    }
}


