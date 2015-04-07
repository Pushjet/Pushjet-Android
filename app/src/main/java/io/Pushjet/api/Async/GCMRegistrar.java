package io.Pushjet.api.Async;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import io.Pushjet.api.HttpUtil;
import io.Pushjet.api.PushjetApi.DeviceUuidFactory;
import io.Pushjet.api.SettingsActivity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GCMRegistrar {
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "app_version";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    protected String TAG = "GCM";
    private GoogleCloudMessaging gcm;
    private Context mContext;

    public GCMRegistrar(Context context) {
        this.mContext = context;
        gcm = GoogleCloudMessaging.getInstance(context);
    }

    public boolean checkPlayServices(android.app.Activity self) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "This device does not support GCM");
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode) && self != null) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, self, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(mContext, "Sorry, you need to have the Google Play services installed :<", Toast.LENGTH_SHORT).show();
            }
            return false;
        } else {
            Log.i(TAG, "Pushjet is ready for Take-Off!");
        }
        return true;
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
        return context.getSharedPreferences(GCMRegistrar.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    public AsyncRegistrar registerInBackground() {
        return this.registerInBackground(false);
    }

    public AsyncRegistrar registerInBackground(boolean force) {
        AsyncRegistrar task = new AsyncRegistrar();
        task.execute(force);
        return task;
    }

    public boolean shouldRegister() {
        return getRegistrationId().equals("");
    }

    public void forgetRegistration() {
        final SharedPreferences prefs = getGcmPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(PROPERTY_REG_ID, "");
        editor.putInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        editor.commit();
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
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(mContext);
                }
                String regid = gcm.register(senderId);

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
