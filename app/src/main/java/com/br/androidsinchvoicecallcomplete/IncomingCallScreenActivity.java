package com.br.androidsinchvoicecallcomplete;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.ebanx.swipebtn.OnStateChangeListener;
import com.ebanx.swipebtn.SwipeButton;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;

import java.util.List;

public class IncomingCallScreenActivity extends BaseActivity {

    static final String TAG = IncomingCallScreenActivity.class.getSimpleName();
    private String mCallId;
    private AudioPlayer mAudioPlayer;

    public static final String ACTION_ANSWER = "answer";
    public static final String ACTION_IGNORE = "ignore";
    public static final String EXTRA_ID = "id";
    public static int MESSAGE_ID = 14;
    private String mAction;

    ImageView profile_image;
    TextView textViewRemoteUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        TextView callState=findViewById(R.id.callState);
        SwipeButton answer = findViewById(R.id.accept_swipe_btn);
        SwipeButton decline = findViewById(R.id.reject_swipe_btn);
        profile_image=findViewById(R.id.incoming_profile_image);
        textViewRemoteUser=findViewById(R.id.remoteUser);

        callState.setText("Incoming Voice Call");

        mAudioPlayer = new AudioPlayer(this);
        mAudioPlayer.playRingtone();

        Intent intent = getIntent();
        mCallId = intent.getStringExtra(SinchService.CALL_ID);
        mAction = "";

        answer.setOnStateChangeListener(new OnStateChangeListener() {
            @Override
            public void onStateChange(boolean active) {
                answerClicked();
            }
        });

        decline.setOnStateChangeListener(new OnStateChangeListener() {
            @Override
            public void onStateChange(boolean active) {
                declineClicked();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getStringExtra(SinchService.CALL_ID) != null) {
                mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
            }
            final int id = intent.getIntExtra(EXTRA_ID, -1);
            if (id > 0) {
                NotificationManager notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(id);
            }
            mAction = intent.getAction();
        }
    }


    @Override
    protected void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.addCallListener(new SinchCallListener());

            textViewRemoteUser.setText("USER NAME");


            if (ACTION_ANSWER.equals(mAction)) {
                mAction = "";
                answerClicked();
            } else if (ACTION_IGNORE.equals(mAction)) {
                mAction = "";
                declineClicked();
            }

        } else {
            Log.e(TAG, "Started with invalid callId, aborting");
            finish();
        }
    }

    private void answerClicked() {
        mAudioPlayer.stopRingtone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            Log.d(TAG, "Answering call");
            call.answer();
            Intent intent = new Intent(this, VoiceCallScreenActivity.class);
            intent.putExtra(SinchService.CALL_ID, mCallId);
            intent.putExtra("userid",call.getRemoteUserId());
            startActivity(intent);
        } else {
            finish();
        }
    }

    private void declineClicked() {
        mAudioPlayer.stopRingtone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended, cause: " + cause.toString());
            mAudioPlayer.stopRingtone();
            finish();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // no need to implement for managed push
        }

    }
}
