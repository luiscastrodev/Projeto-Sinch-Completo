package com.br.androidsinchvoicecallcomplete;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.PushTokenRegistrationCallback;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.UserController;
import com.sinch.android.rtc.UserRegistrationCallback;
import com.sinch.android.rtc.calling.Call;

import java.security.MessageDigest;

import static com.br.androidsinchvoicecallcomplete.SinchService.APP_KEY;
import static com.br.androidsinchvoicecallcomplete.SinchService.APP_SECRET;
import static com.br.androidsinchvoicecallcomplete.SinchService.ENVIRONMENT;

public class HomeActivity extends BaseActivity implements SinchService.StartFailedListener {

    EditText editTextNumber;
    Button buttonNext;
    TextView login_text;
    private long mSigningSequence = 1;
    String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        editTextNumber=findViewById(R.id.input_number);
        buttonNext=findViewById(R.id.login_next);
        login_text=findViewById(R.id.login_text);

        loadingBar = new ProgressDialog(HomeActivity.this);

        PersistedSettings pref = new PersistedSettings(getApplicationContext());

        login_text.setText(pref.getUsername());


        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs=getSharedPreferences("sinch_service",MODE_PRIVATE);

                if (!prefs.getBoolean("isLogin",false)) {

                    loadingBar.setTitle("Voice Call");
                    loadingBar.setMessage("Please wait, while we are configuring voice call...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    SharedPreferences prefs2  = getSharedPreferences("Sinch",MODE_PRIVATE);

                    if (!prefs2.getString("Username","").equals(getSinchServiceInterface().getUsername())) {
                        getSinchServiceInterface().stopClient();
                    }

                    getSinchServiceInterface().setUsername(prefs2.getString("Username",""));

                    currentUserID = prefs2.getString("Username","");
                    userid = editTextNumber.getText().toString();

                    UserController uc = Sinch.getUserControllerBuilder()
                            .context(getApplicationContext())
                            .applicationKey(APP_KEY)
                            .userId(prefs2.getString("Username",""))
                            .environmentHost(ENVIRONMENT)
                            .build();
                    uc.registerUser(new UserRegistrationCallback() {
                        @Override
                        public void onCredentialsRequired(ClientRegistration clientRegistration) {
                            String toSign = currentUserID + APP_KEY + mSigningSequence + APP_SECRET;
                            String signature;
                            MessageDigest messageDigest;
                            try {
                                messageDigest = MessageDigest.getInstance("SHA-1");
                                byte[] hash = messageDigest.digest(toSign.getBytes("UTF-8"));
                                signature = Base64.encodeToString(hash, Base64.DEFAULT).trim();
                            } catch (Exception e) {
                                throw new RuntimeException(e.getMessage(), e.getCause());
                            }

                            clientRegistration.register(signature, mSigningSequence++);
                        }

                        @Override
                        public void onUserRegistered() {
                            // Instance is registered, but we'll wait for another callback, assuring that the push token is
                            // registered as well, meaning we can receive incoming calls.
                        }

                        @Override
                        public void onUserRegistrationFailed(SinchError sinchError) {

                        }
                    }, new PushTokenRegistrationCallback() {
                        @Override
                        public void tokenRegistered() {
                            startClientAndMakeCall();
                        }

                        @Override
                        public void tokenRegistrationFailed(SinchError sinchError) {
                            loadingBar.dismiss();
                            Toast.makeText(HomeActivity.this, "Push token registration failed - incoming calls can't be received!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                else {
                    loadingBar.setTitle("Voice Call");
                    loadingBar.setMessage("Please wait, while we are configuring voice call...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    makeCall();
                }
            }
        });
    }

    ProgressDialog loadingBar;
    private static String callType="voice";
    String userid;

    private void makeCall() {

        Call call;
        String callId;

        loadingBar.dismiss();

        if (callType.equals("voice")) {
            call=getSinchServiceInterface().callUser(userid);
            callId=call.getCallId();

            Intent voiceCallIntent;
            voiceCallIntent=new Intent(this,VoiceCallScreenActivity.class);
            voiceCallIntent.putExtra(SinchService.CALL_ID,callId);
            voiceCallIntent.putExtra("userid", userid);
            startActivity(voiceCallIntent);
        }
        /*else {
            call = getSinchServiceInterface().callUserVideo(userid);
            callId = call.getCallId();

            Intent videoCallIntent;
            videoCallIntent = new Intent(this, VoiceCallScreenActivity.class);
            videoCallIntent.putExtra(SinchService.CALL_ID, callId);
            videoCallIntent.putExtra("userid", userid);
            startActivity(videoCallIntent);
        }*/
    }


    private class PersistedSettings {

        private SharedPreferences mStore;

        private static final String PREF_KEY = "Sinch";

        public PersistedSettings(Context context) {
            mStore = context.getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        }

        public String getUsername() {
            return mStore.getString("Username", "");
        }

        public void setUsername(String username) {
            SharedPreferences.Editor editor = mStore.edit();
            editor.putString("Username", username);
            editor.apply();
        }
    }



    @Override
    protected void onServiceConnected() {
        getSinchServiceInterface().setStartListener(this);
    }

    @Override
    public void onStartFailed(SinchError error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
        loadingBar.dismiss();
    }

    @Override
    public void onStarted() {
        SharedPreferences.Editor ed=getSharedPreferences("sinch_service",MODE_PRIVATE).edit();
        ed.putBoolean("isLogin",true);
        ed.apply();
        makeCall();
    }

    private void startClientAndMakeCall() {
        // start Sinch Client, it'll result onStarted() callback from where the place call activity will be started
        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient();
        }
    }


}