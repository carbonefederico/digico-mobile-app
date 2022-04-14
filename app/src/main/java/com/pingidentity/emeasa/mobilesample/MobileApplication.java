package com.pingidentity.emeasa.mobilesample;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;

public class MobileApplication extends Application {

    public static String NAME = "given_name";
    public static final String USERNAME = "username";
    private static final String PAIRED = "paired";
    public static String TAG = "MobileApplication";

    public static String ID_TOKEN = "id_token";
    public static String ACCESS_TOKEN = "access_token";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {

                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        Log.i(TAG, "Registration token " + token);

                        PingOne.setDeviceToken(MobileApplication.this, token, new PingOne.PingOneSDKCallback() {
                            @Override
                            public void onComplete(@Nullable PingOneSDKError error) {
                                Log.i(TAG, "setDeviceToken - onComplete");
                            }
                        });
                    }
                });
    }


    public  String getPreference(Context context, String preferenceName) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return _sharedPrefs.getString(preferenceName, "");
    }

    public  boolean isPaired(Context context) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return _sharedPrefs.getBoolean(PAIRED, false);
    }

    public boolean hasValidTokens (Context context) {
        if (getPreference(context, ID_TOKEN).length() > 0) {
            return true;
        }
        return false;
    }

    public  void setPreference(Context context, String preferenceName, String text) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor _prefsEditor;
        _prefsEditor = _sharedPrefs.edit();
        _prefsEditor.putString(preferenceName, text);
        _prefsEditor.apply();
    }

    public void setPaired(Context context, boolean b) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor _prefsEditor;
        _prefsEditor = _sharedPrefs.edit();
        _prefsEditor.putBoolean(PAIRED, b);
        _prefsEditor.apply();
    }


}
