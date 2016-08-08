package com.emillozev.sipaudiocalls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipProfile;
import android.util.Log;

public class IncomingCallReceiver extends BroadcastReceiver {

    private String mCallerUriString;

    @Override
    public void onReceive(final Context context, Intent intent) {
        SipAudioCall incomingCall = null;

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                MainActivity activity = (MainActivity) context;

                @Override
                public void onReadyToCall(SipAudioCall call) {
                    Log.d(MainActivity.LOG_TAG, "onReadyToCall: " + call);
                    super.onReadyToCall(call);
                }

                @Override
                public void onCalling(SipAudioCall call) {
                    Log.d(MainActivity.LOG_TAG, "onCalling: " + call);
                    super.onCalling(call);
                }

                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    Log.d(MainActivity.LOG_TAG, "onRinging: " + call + " caller:" + caller);
                    try {
                        call.answerCall(30);
                    } catch (SipException e) {
                        e.printStackTrace();
                    }

                    mCallerUriString = caller.getUriString();
                    activity.setCallerID(mCallerUriString + " is ringing");
                    super.onRinging(call, caller);
                }

                @Override
                public void onRingingBack(SipAudioCall call) {
                    Log.d(MainActivity.LOG_TAG, "onRingingBack: " + call);
                    super.onRingingBack(call);
                }

                @Override
                public void onCallEstablished(SipAudioCall call) {
                    Log.d(MainActivity.LOG_TAG, "onCallEstablished: " + call);
                    call.startAudio();
                    if (call.isMuted()) {
                        call.toggleMute();
                    }
                    activity.setCallerID("You are talking to: " + mCallerUriString);
                    super.onCallEstablished(call);
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    Log.d(MainActivity.LOG_TAG, "onCallEnded: " + call);
                    activity.setCallerID("---");
                    super.onCallEnded(call);
                }

                @Override
                public void onCallBusy(SipAudioCall call) {
                    Log.d(MainActivity.LOG_TAG, "onCallBusy: " + call);
                    super.onCallBusy(call);
                }

                @Override
                public void onCallHeld(SipAudioCall call) {
                    Log.d(MainActivity.LOG_TAG, "onCallHeld: " + call);
                    super.onCallHeld(call);
                }

                @Override
                public void onError(SipAudioCall call, int errorCode, String errorMessage) {
                    Log.d(MainActivity.LOG_TAG, "onError: " + call + "; errorCode: " + errorCode + "errorMessage: " + errorMessage);
                    super.onError(call, errorCode, errorMessage);
                }

                @Override
                public void onChanged(SipAudioCall call) {
                    Log.d(MainActivity.LOG_TAG, "onChanged: " + call);
                    super.onChanged(call);
                }
            };

            MainActivity activity = (MainActivity) context;
            incomingCall = activity.mSipManager.takeAudioCall(intent, listener);
            incomingCall.answerCall(30);
            incomingCall.startAudio();

            Log.d(MainActivity.LOG_TAG, String.valueOf(activity.mSipManager.isOpened(String.valueOf(incomingCall.getLocalProfile()))));
            activity.mAudioCall = incomingCall;
            activity.setTVStatus(incomingCall.getPeerProfile().getUriString());

        } catch (SipException e) {
            e.printStackTrace();
        }
    }
}
