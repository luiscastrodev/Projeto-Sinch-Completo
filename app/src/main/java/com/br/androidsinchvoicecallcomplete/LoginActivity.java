package com.br.androidsinchvoicecallcomplete;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    EditText editTextNumber;
    Button buttonNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextNumber=findViewById(R.id.input_number);
        buttonNext=findViewById(R.id.login_next);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isNetworkAvailable(LoginActivity.this)) {

                    String number = editTextNumber.getText().toString();

                    if (number.isEmpty()) {
                        editTextNumber.setError("Field is empty");
                        editTextNumber.requestFocus();
                        return;
                    }


                    PersistedSettings persistedSettings = new PersistedSettings(getApplicationContext());
                    persistedSettings.setUsername(editTextNumber.getText().toString());


                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("Username", editTextNumber.getText().toString());
                    startActivity(intent);
                    finish();
                }
                else{
                    Snackbar.make(v,"No internet",Snackbar.LENGTH_LONG).show();
                }
            }
        });
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
    public static boolean isNetworkAvailable(Context context){

        if (context==null) return false;

        ConnectivityManager connectivityManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager!=null){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){

                NetworkCapabilities capabilities=connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities!=null){
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                        return true;
                    }
                }
            }

            else {

                try {
                    NetworkInfo activeNetworkInfo=connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo!=null && activeNetworkInfo.isConnected()){
                        Log.i("update_status","Network is available : true");
                        return true;
                    }
                } catch (Exception e){
                    Log.i("update_status",""+e.getMessage());
                }
            }
        }
        Log.i("update_status","Network is available : FALSE");
        return false;
    }
}