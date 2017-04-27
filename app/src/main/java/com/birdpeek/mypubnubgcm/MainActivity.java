package com.birdpeek.mypubnubgcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.pubnub.api.Callback;
import com.pubnub.api.PnGcmMessage;
import com.pubnub.api.PnMessage;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MyTag";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 2;
    private static final String SENDER_ID = "9893...";
    private static final String PROPERTY_REG_ID = "PROPERTY_REGID";
    private Button btnRegister, btnUnregister, btnNotify;
    private String regId;
    private GoogleCloudMessaging gcm = null;

    private final Pubnub pubnub = new Pubnub("pub-c-...", "sub-c-...");
    private String my_channel = "birdpeek";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnRegister = (Button)findViewById(R.id.btnRegister);
        btnUnregister = (Button)findViewById(R.id.btnUnregister);
        btnNotify = (Button)findViewById(R.id.btnNotify);
        btnRegister.setOnClickListener(this);
        btnUnregister.setOnClickListener(this);
        btnNotify.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.btnRegister:
                Log.i(TAG, "Start registering...");
                register();

                break;
            case R.id.btnUnregister:
                Log.i(TAG, "Start unregistering...");
                unregister();
                break;
            case R.id.btnNotify:
                Log.i(TAG, "Send notification...");
                sendNotification();

                break;
            default:
                break;
        }

    }

    private void unregister() {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    Context context = getApplicationContext();
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }

                    // Unregister from GCM
                    gcm.unregister();

                    // Remove Registration ID from memory
                    removeRegistrationId(context);

                    // Disable Push Notification
                    pubnub.disablePushNotificationsOnChannel(my_channel, regId);

                } catch (Exception e) { }
                return null;
            }
        }.execute(null, null, null);

    }

    private void removeRegistrationId(Context context) throws Exception{
        final SharedPreferences prefs =
                getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PROPERTY_REG_ID);
        editor.apply();
        Log.i(TAG, "Registration ID removed");
    }

    private void sendNotification() {
        PnGcmMessage gcmMessage = new PnGcmMessage();
        JSONObject jso = new JSONObject();
        try {
            jso.put("GCMSays", "hi");
        } catch (JSONException e) { }
        gcmMessage.setData(jso);

        PnMessage message = new PnMessage(
                pubnub,
                my_channel,
                callback,
                gcmMessage);
        try {
            message.publish();
        } catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    private static final String CHANNEL = "birdpeek";
    public static Callback callback = new Callback() {
        @Override
        public void successCallback(String channel, Object message) {
            Log.i(TAG, "Success on Channel " + CHANNEL + " : " + message);
        }
        @Override
        public void errorCallback(String channel, PubnubError error) {
            Log.i(TAG, "Error On Channel " + CHANNEL + " : " + error);
        }
    };

    private void register() {
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            try {
                Context context = this;
                regId = getRegistrationId(context);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (regId.isEmpty()) {
                registerInBackground();
            } else {
                toastUser("Registration ID already exists: " + regId);
                Log.i(TAG, "Registration ID already exists: " + regId);
            }
        } else {
            Log.e(TAG, "No valid Google Play Services APK found.");
        }

    }

    private void toastUser(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private void registerInBackground() {
        new AsyncTask() {
            @Override
            protected String doInBackground(Object[] params) {
                String msg;
                try {
                    Context context = getApplicationContext();
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID: " + regId;
                    Log.i(TAG, "" + msg);

                    sendRegistrationId(regId);

                    storeRegistrationId(context, regId);//store the token obtain from gcm
                    Log.i(TAG, msg);
                } catch (Exception ex) {
                    msg = "Error :" + ex.getMessage();
                    Log.e(TAG, msg);
                }
                return msg;
            }
        }.execute(null, null, null);
    }

    private void storeRegistrationId(Context context, String regId) throws Exception{
        final SharedPreferences prefs =
                getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.apply();
        Log.i(TAG, "Registration ID stored...");

    }

    private void sendRegistrationId(String regId) {
        pubnub.enablePushNotificationsOnChannel(my_channel, regId);
        Log.i(TAG, "Send Reg ID to PubNub... " + regId);
    }

    private String getRegistrationId(Context context) throws Exception {
        final SharedPreferences prefs =
                getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            return "";
        }

        return registrationId;
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
//                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                GooglePlayServicesUtil.showErrorNotification(resultCode, getApplicationContext());
            } else {
                Log.e(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
