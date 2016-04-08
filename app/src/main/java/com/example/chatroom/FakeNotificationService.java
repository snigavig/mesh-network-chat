package com.example.chatroom;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

//This class is here for showcase purposes only.
//If this ever gets to be a real application, this should not be used.
@Deprecated
public class FakeNotificationService extends Service {

    private android.support.v4.app.NotificationCompat.Builder mNotificationBuilder;

    private android.support.v4.app.NotificationCompat.Builder getNotificationBuilder() {
        if (null == mNotificationBuilder) {
            mNotificationBuilder = buildNotification();
        }
        return mNotificationBuilder;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIntent(Intent intent) {
        if (ACTION.START_FOREGROUND_ACTION.equals(intent.getAction())) {
            startForeground(NetworkService.NOTIFICATION_ID.FOREGROUND_SERVICE, getNotificationBuilder().build());
        } else if (ACTION.STOP_FOREGROUND_ACTION.equals(intent.getAction())) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            handleIntent(intent);
        }
        return START_STICKY;
    }
    public void onDestroy() {
        stopForeground(true);
        Log.d("boooo", "fake service desroyed");
    }

    private android.support.v4.app.NotificationCompat.Builder buildNotification() {

        return new NotificationCompat.Builder(this)
                .setContentTitle(ChatApplication.getInstance().getString(R.string.app_name));
    }

    public interface ACTION {
        String STOP_FOREGROUND_ACTION = "com.example.chatroom.fake_action.stop_foreground";
        String START_FOREGROUND_ACTION = "com.example.chatroom.fake_action.start_foreground";
    }
}