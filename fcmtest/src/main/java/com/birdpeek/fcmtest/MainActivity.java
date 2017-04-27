package com.birdpeek.fcmtest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.enums.PNPushType;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.push.PNPushAddChannelResult;
import com.pubnub.api.models.consumer.push.PNPushRemoveChannelResult;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MyTag";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 2;
    private static final String SENDER_ID = "8552...";
    private static final String PROPERTY_REG_ID = "PROPERTY_REGID";
    private Button btnRegister, btnUnregister, btnNotify;
    private String regId;


    private PNConfiguration pnConfiguration = new PNConfiguration();
    private PubNub pubnub;
    private String my_channel = "birdpeek_fcm";


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

        pnConfiguration.setSubscribeKey("sub-c-");
        pnConfiguration.setPublishKey("pub-c-");
        pnConfiguration.setSecure(true);
        pubnub = new PubNub(pnConfiguration);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.btnRegister:
                Log.i(TAG, "Start registering...");
                try {
                    if (checkPlayServices()) {
                        register();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

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

    private void register() throws IOException {

        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "token registered : " + token);
//        InstanceID instanceID = InstanceID.getInstance(this);
//        String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
//                GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

        List<String> arrayChannel = new ArrayList<>();
        arrayChannel.add(my_channel);
        pubnub.addPushNotificationsOnChannels()
                .pushType(PNPushType.GCM)
                .channels(arrayChannel)
                .deviceId(token)
                .async(new PNCallback<PNPushAddChannelResult>() {
                    @Override
                    public void onResponse(PNPushAddChannelResult result, PNStatus status) {
                        // handle response.
                        if (status.isError()) {
                            // something bad happened.
                            Log.d(TAG, "error happened while registering: " + status.toString());
                        } else {
                            Log.d(TAG, "registration worked! result: " + result.toString());
                        }
                    }
                });

    }

    private void sendNotification() {

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("DisplayName", "Faideen");
        data.put("message", "JDT won 10-0");
        data.put("summary", "Congrats, it was successful!");
        data.put("photoUrl", "A url link");
        data.put("gcm.notification.title", "JDT is ahead");
        data.put("gcm.notification.body", "Here news from jdt");
        payload.put("data", data);
        Map<String, Object> jsonParent = new HashMap<>();
        jsonParent.put("pn_gcm", payload);  //compulsory
        jsonParent.put("pn_debug", true);

        pubnub.publish().message(jsonParent)
                .channel(my_channel)
                .async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        /// handle publish result.
                        if (status.isError()) {
                            // something bad happened.
                            Log.d(TAG,"error happened while publishing: " + status.toString());
                        } else {
                            Log.d(TAG,"publish worked! timetoken: " + result.getTimetoken());
                        }
                    }
                });
    }

    private void unregister() {
        String token = FirebaseInstanceId.getInstance().getToken();
        pubnub.removePushNotificationsFromChannels()
                .deviceId(token)
                .channels(Arrays.asList(my_channel))
                .pushType(PNPushType.GCM)
                .async(new PNCallback<PNPushRemoveChannelResult>() {
                    @Override
                    public void onResponse(PNPushRemoveChannelResult result, PNStatus status) {
                        // handle response.
                        if (status.isError()) {
                            // something bad happened.
                            Log.d(TAG, "error happened while unregistering: " + status.toString());
                        } else {
                            Log.d(TAG, "unregistration worked! result: " + result.toString());
                        }
                    }
                });

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

    //    private  void sendNotification2(){
//        FirebaseMessaging fm = FirebaseMessaging.getInstance();
//        fm.send(new RemoteMessage.Builder(SENDER_ID + "@gcm.googleapis.com")
//                .setMessageId(Integer.toString(1234567890))
//                .addData("my_message", "Hello World")
//                .addData("my_action","SAY_HELLO")
//                .build());
//    }
}
