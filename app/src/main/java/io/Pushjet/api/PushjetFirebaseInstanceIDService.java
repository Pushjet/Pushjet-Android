package io.Pushjet.api;


import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import io.Pushjet.api.Async.PushjetRegistrationService;

public class PushjetFirebaseInstanceIDService extends FirebaseInstanceIdService {
    protected final String TAG = "PushjetFCMIIDService";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "FCM Token refreshed.");

        // call PushjetRegistrationService to update the token
        Intent intent = new Intent(this, PushjetRegistrationService.class);
        intent.putExtra(PushjetRegistrationService.PROPERTY_FCM_TOKEN, refreshedToken);
        startService(intent);
    }
}
