package com.emillozev.sipaudiocalls;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String LOG_TAG = "SIPAUDIOCALLS";

    public SipManager mSipManager = null;
    public SipProfile mSipProfile = null;
    private IncomingCallReceiver mCallReceiver;
    public SipAudioCall mAudioCall = null;

    private SharedPreferences mSharedPreferences;

    @BindView(R.id.username_edtxt)
    EditText mETUsername;

    @BindView(R.id.password_edtxt)
    EditText mETPassword;

    @BindView(R.id.domain_edtxt)
    EditText mETDomain;

    @BindView(R.id.submit_data_btn)
    Button mSubmitData;

    @BindView(R.id.status_txtview)
    TextView mTVStatus;

    @BindView(R.id.answer_btn)
    Button mAnswer;

    @BindView(R.id.end_btn)
    Button mCloseCall;

    @BindView(R.id.make_call_edtxt)
    EditText mSipAddress;

    @BindView(R.id.chronometer_view)
    Chronometer mChronometer;

    @BindView(R.id.caller_id)
    TextView mCallerID;

    @BindView(R.id.proxy_edtxt)
    EditText mETProxy;

    @BindView(R.id.auth_edtxt)
    EditText mETAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        mTVStatus.setText("Not Registered Yet");
        mSubmitData.setOnClickListener(this);
        mAnswer.setOnClickListener(this);
        mCloseCall.setOnClickListener(this);

        mSharedPreferences = getSharedPreferences("Fields", MODE_PRIVATE);

        mETUsername.setText(mSharedPreferences.getString("user", ""));
        mETPassword.setText(mSharedPreferences.getString("pass", ""));
        mETDomain.setText(mSharedPreferences.getString("domain", ""));
        mSipAddress.setText(mSharedPreferences.getString("sip_address", ""));
        mETAuth.setText(mSharedPreferences.getString("auth", ""));
        mETProxy.setText(mSharedPreferences.getString("proxy", ""));

        initializeManager();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");

        mCallReceiver = new IncomingCallReceiver();
        this.registerReceiver(mCallReceiver, filter);
    }

    public void setCallerID(String text) {
        mCallerID.setText(text);
    }

    @Override
    protected void onDestroy() {
        closeLocalProfile();
        super.onDestroy();
    }

    private void closeLocalProfile() {
        if (mSipManager == null) {
            return;
        }

        try {
            if (mSipManager != null)
                mSipManager.close(mSipProfile.getUriString());
        } catch (SipException e) {
            Log.d(LOG_TAG, "Failed to close local profile: ", e);
        }
    }

    private void initializeProfile(String username, String password, String domain, String outboundProxy, String authName) {
        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);

            if (!outboundProxy.equals(""))
                builder.setOutboundProxy(outboundProxy);

            if (!authName.equals(""))
                builder.setAuthUserName(authName);

            mSipProfile = builder.build();

            Intent intent = new Intent();
            intent.setAction("android.SipDemo.INCOMING_CALL");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);
            mSipManager.open(mSipProfile, pendingIntent, null);

            SipRegistrationListener listener = new SipRegistrationListener() {
                @Override
                public void onRegistering(String s) {
                    final String regS = s;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setTVStatus("Registering with the server ... ( " + regS + " )");
                        }
                    });
                    Log.d(LOG_TAG, "onRegistering");
                }

                @Override
                public void onRegistrationDone(String s, long l) {
                    final String regS = s;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setTVStatus("Ready! ( " + regS + " )");
                        }
                    });
                    Log.d(LOG_TAG, "onRegistrationDone");
                }

                @Override
                public void onRegistrationFailed(String s, int i, String s1) {
                    final String regS = s;
                    final int failI = i;
                    final String errS = s1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setTVStatus("Failed to register! (localProfile: " + regS
                                    + "; errorCode: " + failI + "; errorMessage: " + errS);
                        }
                    });
                    Log.d(LOG_TAG, "onRegistrationFailed");
                }
            };
            mSipManager.setRegistrationListener(mSipProfile.getUriString(), listener);

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SipException e) {
            e.printStackTrace();
        }

        SipProfile peerProfile = null;
        try {
            SipProfile.Builder builder = new SipProfile.Builder(mSipAddress.getText().toString());
            peerProfile = builder.build();
        } catch (ParseException e) {
            e.printStackTrace();
        }


        Intent intent = new Intent();
        intent.setAction("android.SipDemo.INCOMING_CALL");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);
        try {
            mSipManager.open(mSipProfile, pendingIntent, null);
//            mSipManager.makeAudioCall(mSipProfile, peerProfile,mListener, 30);
//            mSipManager.takeAudioCall(pendingIntent, mListener);
        } catch (SipException e) {
            e.printStackTrace();
        }


    }

    private void startIntent() {

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");

        mCallReceiver = new IncomingCallReceiver();
        this.registerReceiver(mCallReceiver, filter);


//        Intent intent = new Intent();
//        intent.setAction("android.SipDemo.INCOMING_CALL");
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);

        //TODO: The Listener
//        try {
//            mSipManager.open(mSipProfile, pendingIntent, );
//        } catch (SipException e) {
//            e.printStackTrace();
//        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.USE_SIP)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.USE_SIP)) {
                        Toast.makeText(MainActivity.this, "Permission needed", Toast.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.USE_SIP},
                                123);

                    }
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 123:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
//                return;
                break;

        }
    }

    SipAudioCall.Listener mListener = new SipAudioCall.Listener() {
        @Override
        public void onReadyToCall(SipAudioCall call) {
            Log.d(LOG_TAG, "SipAudioCall.Listener onReadyToCall: " + call);
            super.onReadyToCall(call);
        }

        @Override
        public void onCallEstablished(SipAudioCall call) {
            Log.d(LOG_TAG, "SipAudioCall.Listener onCallEstablished: " + call);
            mAudioCall = call;
            mAudioCall.startAudio();
            call.startAudio();

            super.onCallEstablished(call);
        }

        @Override
        public void onCalling(SipAudioCall call) {
            Log.d(LOG_TAG, "SipAudioCall.Listener onCalling: " + call);
            super.onCalling(call);
        }

        @Override
        public void onRinging(SipAudioCall call, SipProfile caller) {
            Log.d(LOG_TAG, "SipAudioCall.Listener onRinging: " + call);
            setTVStatus("Caller: " + caller.getUserName() + " : is calling you!");
            super.onRinging(call, caller);
        }

        @Override
        public void onError(SipAudioCall call, int errorCode, String errorMessage) {
            Log.d(LOG_TAG, "SipAudioCall.Listener onError: " + call + " errorCode: " + errorCode + " errorMessage: " + errorMessage);
            super.onError(call, errorCode, errorMessage);
        }
    };

    private void initializeManager() {
        if (mSipManager == null) {
            mSipManager = mSipManager.newInstance(this);
        }
        if (SipManager.isVoipSupported(this))
            toaster("Voip supported");
        else
            toaster("Voip not supported");


        if (SipManager.isApiSupported(this))
            toaster("Api supported");
        else
            toaster("Api not supported");

    }

    public void setTVStatus(String text) {
        mTVStatus.setText(text);
    }

    private void toaster(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.answer_btn:
                mChronometer.start();
                SharedPreferences.Editor editor1 = mSharedPreferences.edit();
                editor1.putString("sip_address", mSipAddress.getText().toString());
                editor1.apply();

                toaster("Answer");
                makeCall();
                break;

            case R.id.end_btn:
                mChronometer.stop();
                if (mAudioCall != null) {
                    try {
                        mAudioCall.endCall();
                    } catch (SipException e) {
                        e.printStackTrace();
                    }
                    mAudioCall.close();
                }
                toaster("Close");
                break;

            case R.id.submit_data_btn:
                if (!mETUsername.getText().toString().equals("")
                        && !mETDomain.getText().toString().equals("")
                        && !mETPassword.getText().toString().equals("")) {


                    String proxy = mETProxy.getText().toString();
                    String auth = mETAuth.getText().toString();

                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putString("user", mETUsername.getText().toString());
                    editor.putString("pass", mETPassword.getText().toString());
                    editor.putString("domain", mETDomain.getText().toString());
                    editor.putString("proxy", proxy);
                    editor.putString("auth", auth);

                    editor.apply();

                    initializeProfile(mETUsername.getText().toString(), mETPassword.getText().toString(), mETDomain.getText().toString(), proxy, auth);
                    toaster("initializing");

                    toaster(mSipProfile.getUriString());

                } else {
                    toaster("Fill all the fields");
                }

                break;
        }
    }

    private void makeCall() {
        if (!mSipAddress.getText().toString().equals(""))
            try {
                mAudioCall = mSipManager.makeAudioCall(mSipProfile.getUriString(), mSipAddress.getText().toString(), mListener, 30);
                mAudioCall.startAudio();
                mChronometer.start();
                if (mAudioCall.isInCall()) {
                    toaster("in call");
                }
            } catch (SipException e) {
                e.printStackTrace();
            }
        else
            toaster("Fill who to call");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
