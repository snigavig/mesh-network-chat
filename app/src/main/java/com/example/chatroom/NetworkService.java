package com.example.chatroom;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.example.chatroom.model.Message;
import com.example.chatroom.model.MinifiedMessage;
import com.example.chatroom.model.MinifiedUser;
import com.example.chatroom.model.TechnicalMessage;
import com.example.chatroom.model.User;
import com.example.chatroom.util.DBHelper;
import com.example.chatroom.util.NetworkHelper;
import com.example.chatroom.util.SecurityHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NetworkService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Connections.ConnectionRequestListener,
        Connections.MessageListener,
        Connections.EndpointDiscoveryListener {

    public static final String BROADCAST_MESSAGE_TYPE_KEY = "BROADCAST_MESSAGE_TYPE";
    public static final String ACTIVITY_FOREGROUND_KEY = "ACTIVITY_FOREGROUND";
    public static final String MINIFIED_OBJECT_SERIALIZABLE_EXTRA_KEY = "MINIFIED_OBJECT_SERIALIZABLE_EXTRA";
    public static final String NETWORK_BROADCAST_KEY = "NETWORK_BROADCAST";
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();
    // Set an appropriate timeout length in milliseconds
    private final long DISCOVER_TIMEOUT = 10000L;
    private final long CONNECT_TIMEOUT = 10000L;
    private ScheduledFuture<?> discoveringScheduledFuture;
    private ScheduledFuture<?> connectingScheduledFuture;
    private boolean mIsStarted = false;
    private boolean isInitializing = false;
    private GoogleApiClient mGoogleApiClient;
    private android.support.v4.app.NotificationCompat.Builder mNotificationBuilder;
    private PowerManager.WakeLock mWakeLock;
    private String localDeviceId;
    private String localEndpointId;
    private boolean isAlone = true;
    private int initialSyncTotalDataCount = -1;
    private int initialSyncDataCount = 0;
    private int outgoingTotalDataCount;
    private boolean isActivity = false;

    private android.support.v4.app.NotificationCompat.Builder getNotificationBuilder() {
        if (null == mNotificationBuilder) {
            mNotificationBuilder = buildNotification();

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.mipmap.ic_launcher);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(ACTION.MAIN_ACTION);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            mNotificationBuilder
                    .setContentTitle(ChatApplication.getInstance().getString(R.string.app_name))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(
                            Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent);

            updateNotification(null);
        }
        return mNotificationBuilder;
    }

    @Override
    public void onConnected(Bundle bundle) {
        localDeviceId = Nearby.Connections.getLocalDeviceId(mGoogleApiClient);
        localEndpointId = Nearby.Connections.getLocalEndpointId(mGoogleApiClient);
        startDiscovery();
    }

    @Override
    public void onConnectionSuspended(int i) {
        startDiscovery();
    }

    @Override
    public void onConnectionRequest(final String remoteEndpointId, String remoteDeviceId,
                                    final String remoteEndpointName, byte[] payload) {
        // Automatically accept all requests
        Nearby.Connections.acceptConnectionRequest(mGoogleApiClient, remoteEndpointId,
                null, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d("tag", "Connected to " + remoteEndpointName);
                } else {
                    Log.d("tag", "Failed to connect to: " + remoteEndpointName);
                }
            }
        });

    }

    @Override
    public void onEndpointFound(final String endpointId, String deviceId,
                                String serviceId, final String endpointName) {
        isAlone = false;
        connectTo(endpointId, endpointName);
        if (null != discoveringScheduledFuture) {
            discoveringScheduledFuture.cancel(true);
            discoveringScheduledFuture = null;
        }
    }

    private void syncDB() {
        TechnicalMessage technicalMessage = new TechnicalMessage(NETWORK_ACTION.GET_ALL_DATA);
        byte[] myPayload = null;
        try {
            myPayload = NetworkHelper.convertToBytes(technicalMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                ChatApplication.getInstance().getSharedPreferencesHelper().getActiveRemoteEndpointId(),
                myPayload);
    }

    @Override
    public void onEndpointLost(String s) {
        startDiscovery();
    }

    @Override
    public void onMessageReceived(final String endpointId, final byte[] payload, boolean isReliable) {
        Map<Byte, Object> parsedPayload = new HashMap<>();

        try {
            parsedPayload = NetworkHelper.convertFromBytes(payload);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        Map.Entry<Byte, Object> entry = parsedPayload.entrySet().iterator().next();

        switch (entry.getKey()) {
            case NETWORK_MESSAGE_TYPE.TECHNICAL_MESSAGE:
                TechnicalMessage technicalMessage = new TechnicalMessage(entry.getValue());
                switch (technicalMessage.getMessage()) {
                    case NETWORK_ACTION.GET_ALL_DATA:
                        outgoingTotalDataCount = 0;
                        List<User> users = new Select().from(User.class).execute();
                        for (User user : users) {
                            byte[] myPayload = new byte[0];
                            try {
                                MinifiedUser minifiedUser = user.minified();
                                myPayload = NetworkHelper.convertToBytes(minifiedUser);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                                    endpointId,
                                    myPayload);
                            outgoingTotalDataCount++;
                        }

                        List<Message> messages = new Select().from(Message.class).execute();
                        for (Message message : messages) {
                            sendMessage(message, endpointId);
                            outgoingTotalDataCount++;
                        }
                        TechnicalMessage totalDataCountMessage = new TechnicalMessage(NETWORK_ACTION.POST_TOTAL_DATA_COUNT);
                        totalDataCountMessage.setData(String.valueOf(outgoingTotalDataCount));
                        byte[] myPayload = null;
                        try {
                            myPayload = NetworkHelper.convertToBytes(totalDataCountMessage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                                endpointId,
                                myPayload);
                        break;
                    case NETWORK_ACTION.GET_FRESH_DATA:
                        break;
                    case NETWORK_ACTION.POST_TOTAL_DATA_COUNT:
                        initialSyncTotalDataCount = Integer.parseInt(technicalMessage.getData());
                        break;
                }
                break;
            case NETWORK_MESSAGE_TYPE.MESSAGE:
                MinifiedMessage minifiedMessage = (MinifiedMessage) entry.getValue();
                DBHelper.addMessage(minifiedMessage);
                if (!isInitializing) {
                    updateNotification(minifiedMessage);
                }
                Intent chatIntent = new Intent(NETWORK_BROADCAST_KEY);
                chatIntent.putExtra(NetworkService.BROADCAST_MESSAGE_TYPE_KEY, BROADCAST_ACTION.REFRESH_CHAT_KEY);
                LocalBroadcastManager.getInstance(this).sendBroadcast(chatIntent);
                if (isInitializing) initialSyncDataCount++;
                break;
            case NETWORK_MESSAGE_TYPE.USER_UPDATE:
                MinifiedUser minifiedUser = (MinifiedUser) entry.getValue();
                DBHelper.addUser(minifiedUser);
                Intent membersIntent = new Intent(NETWORK_BROADCAST_KEY);
                membersIntent.putExtra(NetworkService.BROADCAST_MESSAGE_TYPE_KEY, BROADCAST_ACTION.REFRESH_MEMBERS_KEY);
                LocalBroadcastManager.getInstance(this).sendBroadcast(membersIntent);
                if (isInitializing) initialSyncDataCount++;
                break;
        }
        if (isInitializing) {
            if (initialSyncTotalDataCount == initialSyncDataCount) {
                DBHelper.createSelfUser(localDeviceId, localEndpointId);

                Intent serviceIntent = new Intent(NetworkService.this, NetworkService.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(NetworkService.MINIFIED_OBJECT_SERIALIZABLE_EXTRA_KEY, DBHelper.getSelfUser().minified());
                serviceIntent.putExtras(bundle);
                serviceIntent.setAction(NetworkService.ACTION.SEND_MULTICAST_ACTION);
                startService(serviceIntent);
                unblockUI();
                isInitializing = false;
            }
        }
    }

    private void sendMessage(Message message, String remoteEndpoint) {
        Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                remoteEndpoint,
                parseMessage(message));
    }

    private void sendMessage(MinifiedMessage message, List<String> remoteEndpoints) {
        Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                remoteEndpoints,
                parseMessage(message));
    }

    private void sendTechnicalMessage(TechnicalMessage message, List<String> remoteEndpoints) {
        Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                remoteEndpoints,
                parseObject(message));
    }

    private void sendUser(MinifiedUser user, List<String> remoteEndpoints) {
        Nearby.Connections.sendReliableMessage(mGoogleApiClient,
                remoteEndpoints,
                parseObject(user));
    }

    private byte[] parseMessage(Message message) {
        byte[] myPayload = new byte[0];
        try {
            myPayload = NetworkHelper.convertToBytes(message.minified());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myPayload;
    }

    private byte[] parseMessage(MinifiedMessage message) {
        byte[] myPayload = new byte[0];
        try {
            myPayload = NetworkHelper.convertToBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myPayload;
    }

    private byte[] parseObject(Object object) {
        byte[] myPayload = new byte[0];
        try {
            myPayload = NetworkHelper.convertToBytes(object);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myPayload;
    }

    @Override
    public void onDisconnected(String s) {
        mGoogleApiClient.reconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Play services are not available", Toast.LENGTH_LONG).show();
        mGoogleApiClient.clearDefaultAccountAndReconnect();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ChatApplication.getInstance().getString(R.string.package_name));
        if (null != mWakeLock && !mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIntent(Intent intent) {
        if (ACTION.START_FOREGROUND_ACTION.equals(intent.getAction())) {
            if (!mGoogleApiClient.isConnecting())
                mGoogleApiClient.connect();

            //The following trick with FakeNotificationService is a dirty hack and not really fare from user's perspective
            //I'm adding this only for the sake of the challenge, so that the app looks pretty.
            //In case this code ever gets to the production, make sure you resolve this, so that user is always aware
            //of what's going on in the background.
            Intent startFakeServiceIntent = new Intent(ChatApplication.getInstance(), FakeNotificationService.class);
            startFakeServiceIntent.setAction(FakeNotificationService.ACTION.START_FOREGROUND_ACTION);
            ChatApplication.getInstance().startService(startFakeServiceIntent);

            startForeground(NOTIFICATION_ID.FOREGROUND_SERVICE, getNotificationBuilder().build());

            Intent stopFakeServiceIntent = new Intent(ChatApplication.getInstance(), FakeNotificationService.class);
            stopFakeServiceIntent.setAction(FakeNotificationService.ACTION.STOP_FOREGROUND_ACTION);
            ChatApplication.getInstance().startService(stopFakeServiceIntent);
            if (mIsStarted) {
                unblockUI();
            } else {
                mIsStarted = true;
            }
        } else if (ACTION.STOP_FOREGROUND_ACTION.equals(intent.getAction())) {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
            stopSelf();
        } else if (ACTION.UPDATE_ACTION.equals(intent.getAction())) {
            isActivity = intent.getExtras().getBoolean(ACTIVITY_FOREGROUND_KEY);
            //updateNotification(null);
        } else if (ACTION.SEND_MULTICAST_ACTION.equals(intent.getAction())) {
            List<User> users = new Select()
                    .from(User.class)
                    .where(User.COLUMN_SELF + " = ?", false)
                    .execute();
            List<String> endpoints = new ArrayList<>();
            for (User user : users) {
                endpoints.add(user.getRemoteEndpointId());
                Object object = intent.getExtras().getSerializable(MINIFIED_OBJECT_SERIALIZABLE_EXTRA_KEY);
                if (object instanceof TechnicalMessage) {
                    TechnicalMessage technicalMessage = new TechnicalMessage(object);
                    sendTechnicalMessage(technicalMessage, endpoints);
                } else if (object instanceof MinifiedMessage) {
                    MinifiedMessage minifiedMessage = new MinifiedMessage(object);
                    sendMessage(minifiedMessage, endpoints);
                } else if (object instanceof MinifiedUser) {
                    MinifiedUser minifiedUser = new MinifiedUser(object);
                    sendUser(minifiedUser, endpoints);
                }
            }
        }
    }

    private void updateNotification(Object object) {
        if (null == object)
            return;
        if (!isActivity) {
            android.support.v4.app.NotificationCompat.Builder builder = getNotificationBuilder();
            if (object instanceof TechnicalMessage) {
                TechnicalMessage technicalMessage = new TechnicalMessage(object);
                Log.d("tag", technicalMessage.getMessage());
                return;
            } else if (object instanceof MinifiedMessage) {
                MinifiedMessage minifiedMessage = new MinifiedMessage(object);
                builder.setContentText(SecurityHelper.decryptIt(minifiedMessage.text));
            } else if (object instanceof MinifiedUser) {
                MinifiedUser minifiedUser = new MinifiedUser(object);
                builder.setContentText(minifiedUser.nickname);
            }
            startForeground(NOTIFICATION_ID.FOREGROUND_SERVICE, builder.build());
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
        super.onDestroy();
        if (null != mWakeLock) {
            mWakeLock.release();
        }
        stopForeground(true);
    }

    private android.support.v4.app.NotificationCompat.Builder buildNotification() {

        return new NotificationCompat.Builder(this)
                .setContentTitle(ChatApplication.getInstance().getString(R.string.app_name));
    }


    private void startAdvertising() {
        if (!NetworkHelper.isConnectedToNetwork(this)) {
            // Implement logic when device is not connected to a network
            Toast.makeText(this, "You are not connected to the network", Toast.LENGTH_LONG).show();
        }

        // Identify that this device is the host

        // Advertising with an AppIdentifer lets other devices on the
        // network discover this application and prompt the user to
        // install the application.
        List<AppIdentifier> appIdentifierList = new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(getPackageName()));
        AppMetadata appMetadata = new AppMetadata(appIdentifierList);

        // The advertising timeout is set to run indefinitely
        // Positive values represent timeout in milliseconds
        long NO_TIMEOUT = 0L;

        Nearby.Connections.startAdvertising(mGoogleApiClient, null, appMetadata, NO_TIMEOUT,
                this).setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
            @Override
            public void onResult(Connections.StartAdvertisingResult result) {
                if (result.getStatus().isSuccess()) {
                    // Device is advertising
                    Log.d("tag", "started advertising");
                } else {
                    startDiscovery();
                }
            }
        });
    }

    private void onNoNetworkResponse() {
        DBHelper.createSelfUser(localDeviceId, localEndpointId);
        unblockUI();
        ChatApplication.getInstance().getSharedPreferencesHelper().setLastUpdateTime(System.currentTimeMillis());
        startAdvertising();
    }

    private void unblockUI() {
        Intent intent = new Intent(NETWORK_BROADCAST_KEY);
        intent.putExtra(NetworkService.BROADCAST_MESSAGE_TYPE_KEY, BROADCAST_ACTION.NETWORK_INITIALISATION_COMPLETED_KEY);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startDiscovery() {
        if (!NetworkHelper.isConnectedToNetwork(this)) {
            // Implement logic when device is not connected to a network
            Toast.makeText(this, "You are not connected to the network", Toast.LENGTH_LONG).show();
        }
        String serviceId = getString(R.string.service_id);
        // Discover nearby apps that are advertising with the required service ID.
        Nearby.Connections.startDiscovery(mGoogleApiClient, serviceId, DISCOVER_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Status>() {
                                       @Override
                                       public void onResult(Status status) {
                                           if (status.isSuccess()) {
                                               // Device is discovering
                                               Log.d("tag", "started discovering");
                                           } else {
                                               Toast.makeText(NetworkService.this, "Failed to connect to network...", Toast.LENGTH_SHORT).show();
                                           }
                                       }
                                   }
                );

        Runnable task = new Runnable() {
            public void run() {
                onNoNetworkResponse();
            }
        };
        discoveringScheduledFuture = worker.schedule(task, DISCOVER_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private void connectTo(final String endpointId, final String endpointName) {
        // Send a connection request to a remote endpoint. By passing 'null' for
        // the name, the Nearby Connections API will construct a default name
        // based on device model such as 'LGE Nexus 5'.
        Runnable task = new Runnable() {
            public void run() {
                onNoNetworkResponse();
            }
        };
        connectingScheduledFuture = worker.schedule(task, CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, null,
                endpointId, null, new Connections.ConnectionResponseCallback() {
                    @Override
                    public void onConnectionResponse(String remoteEndpointId, Status status,
                                                     byte[] bytes) {
                        if (status.isSuccess()) {
                            // Successful connection
                            ChatApplication.getInstance().getSharedPreferencesHelper().setActiveRemoteEndpointId(endpointId);
                            isInitializing = true;
                            syncDB();
                        } else {
                            startDiscovery();
                        }
                    }
                }, this);
    }

    public interface ACTION {
        String MAIN_ACTION = "com.example.chatroom.action.main";
        String STOP_FOREGROUND_ACTION = "com.example.chatroom.action.stop_foreground";
        String UPDATE_ACTION = "com.example.chatroom.action.update";
        String SEND_MULTICAST_ACTION = "com.example.chatroom.action.send_multicast";
        String START_FOREGROUND_ACTION = "com.example.chatroom.action.start_foreground";
    }

    public interface NETWORK_ACTION {
        String GET_FRESH_DATA = "com.example.chatroom.network_action.get_fresh_data";
        String GET_ALL_DATA = "com.example.chatroom.network_action.get_all_data";
        String POST_TOTAL_DATA_COUNT = "com.example.chatroom.network_action.post_total_data_count";
    }

    public interface BROADCAST_ACTION {
        String REFRESH_CHAT_KEY = "REFRESH_CHAT";
        String REFRESH_MEMBERS_KEY = "REFRESH_MEMBERS";
        String REFRESH_USER_PROFILE_KEY = "REFRESH_USER_PROFILE";
        String NETWORK_INITIALISATION_COMPLETED_KEY = "NETWORK_INITIALISATION_COMPLETED";
    }

    public interface NETWORK_MESSAGE_TYPE {
        byte TECHNICAL_MESSAGE = 10;
        byte MESSAGE = 20;
        byte USER_UPDATE = 30;
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 101;
    }
}