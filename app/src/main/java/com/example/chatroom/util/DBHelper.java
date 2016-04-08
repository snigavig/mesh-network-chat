package com.example.chatroom.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.example.chatroom.model.Message;
import com.example.chatroom.model.MinifiedMessage;
import com.example.chatroom.model.MinifiedUser;
import com.example.chatroom.model.User;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

public class DBHelper {
    public static User getSelfUser() {
        return new Select().from(User.class).where(User.COLUMN_SELF + " = ?", true).executeSingle();
    }

    public static List<User> getAllUsers() {
        return new Select().from(User.class).execute();
    }

    public static List<Message> getLastTwentyMessages() {
        return new Select().from(Message.class).limit(20).execute();
    }

    public static void createSelfUser(String localDeviceId, String localEndpointId) {
        User selfUser = getSelfUser();
        if (null == selfUser) {
            ActiveAndroid.beginTransaction();
            try {
                User user = new User();
                user.setModified(System.currentTimeMillis());
                user.setNickname((localDeviceId + localEndpointId).substring(0, 20));
                user.setUuid(UUID.randomUUID().toString());
                user.setRemoteEndpointId(localEndpointId);
                user.setSelf(true);
                user.save();
                ActiveAndroid.setTransactionSuccessful();
            } finally {
                ActiveAndroid.endTransaction();
            }
        }
    }

    public static void addMessage(MinifiedMessage minifiedMessage) {

        ActiveAndroid.beginTransaction();
        try {
            Message message = new Message();
            message.setCreated(minifiedMessage.created);
            message.setText(minifiedMessage.text);
            message.setUuid(minifiedMessage.uuid);
            message.save();
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
    }

    public static void addUser(MinifiedUser minifiedUser) {
        User existingUser = DBHelper.getUserByUUID(minifiedUser.uuid);
        if (null != existingUser) {
            ActiveAndroid.beginTransaction();
            try {
                existingUser.setNickname(minifiedUser.nickname);
                existingUser.setModified(minifiedUser.modified);
                existingUser.setRemoteEndpointId(minifiedUser.remoteEndpointId);
                existingUser.setAvatar(minifiedUser.avatar);
                existingUser.save();
                ActiveAndroid.setTransactionSuccessful();
            } finally {
                ActiveAndroid.endTransaction();
            }
        } else {
            ActiveAndroid.beginTransaction();
            try {
                User user = new User();
                user.setNickname(minifiedUser.nickname);
                user.setModified(minifiedUser.modified);
                user.setUuid(minifiedUser.uuid);
                user.setRemoteEndpointId(minifiedUser.remoteEndpointId);
                user.setAvatar(minifiedUser.avatar);
                user.save();
                ActiveAndroid.setTransactionSuccessful();
            } finally {
                ActiveAndroid.endTransaction();
            }
        }
    }

    public static User getUserByUUID(String uuid) {
        return new Select().from(User.class).where(User.COLUMN_UUID + " = ?", uuid).executeSingle();
    }

    public static User getUserByNickname(String nickname) {
        return new Select().from(User.class).where(User.COLUMN_NAME + " = ?", nickname).executeSingle();
    }

    public static User updateSelfAvatar(Bitmap bitmap) {
        User selfUser = getSelfUser();
        ActiveAndroid.beginTransaction();
        try {
            selfUser.setAvatar(BitMapToString(bitmap));
            selfUser.save();
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
        return selfUser;
    }

    public static User updateSelfNickname(String nickname) {
        User selfUser = getSelfUser();
        ActiveAndroid.beginTransaction();
        try {
            selfUser.setNickname(nickname);
            selfUser.save();
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
        return selfUser;
    }

    private static String BitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public static Bitmap StringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }
}
