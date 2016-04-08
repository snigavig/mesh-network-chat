package com.example.chatroom.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferencesHelper {
    private static final String LAST_UPDATE_TIME_KEY = "LAST_UPDATE_TIME";
    private static final String ACTIVE_REMOTE_ENDPOINT_ID_KEY = "ACTIVE_REMOTE_ENDPOINT_ID";


    private final SharedPreferences prefs;


    public SharedPreferencesHelper(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear().apply();
    }


    public SharedPreferences getPrefs() {
        return prefs;
    }


    public long getLastUpdateTime() {
        return prefs.getLong(LAST_UPDATE_TIME_KEY, -1);
    }


    public void setLastUpdateTime(long value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_UPDATE_TIME_KEY, value);
        editor.apply();
    }

    public String getActiveRemoteEndpointId() {
        return prefs.getString(ACTIVE_REMOTE_ENDPOINT_ID_KEY, "");
    }


    public void setActiveRemoteEndpointId(String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ACTIVE_REMOTE_ENDPOINT_ID_KEY, value);
        editor.apply();
    }
}