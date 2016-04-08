package com.example.chatroom;

import android.app.Application;
import android.content.Intent;

import com.activeandroid.ActiveAndroid;
import com.example.chatroom.util.SharedPreferencesHelper;

public class ChatApplication extends Application {
    private static SharedPreferencesHelper mSharedPreferencesHelper;
    private static ChatApplication mInstance;

    public ChatApplication() {
        super();
    }

    public static ChatApplication getInstance() {
        return mInstance;
    }

    public SharedPreferencesHelper getSharedPreferencesHelper() {
        return mSharedPreferencesHelper;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
        ActiveAndroid.setLoggingEnabled(BuildConfig.DEBUG);
        mSharedPreferencesHelper = new SharedPreferencesHelper(this);
        mInstance = this;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Intent serviceIntent = new Intent(getInstance(), NetworkService.class);
        serviceIntent.setAction(NetworkService.ACTION.STOP_FOREGROUND_ACTION);
        getInstance().startService(serviceIntent);
    }
}