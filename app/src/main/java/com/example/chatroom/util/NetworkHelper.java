package com.example.chatroom.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.chatroom.NetworkService;
import com.example.chatroom.model.MinifiedMessage;
import com.example.chatroom.model.MinifiedUser;
import com.example.chatroom.model.TechnicalMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;

public class NetworkHelper {
    private static final int[] NETWORK_TYPES = {ConnectivityManager.TYPE_WIFI,
            ConnectivityManager.TYPE_ETHERNET};

    public static boolean isConnectedToNetwork(Context context) {
        ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        for (int networkType : NETWORK_TYPES) {
            NetworkInfo info = connManager.getNetworkInfo(networkType);
            if (info != null && info.isConnectedOrConnecting()) {
                return true;
            }
        }
        return false;
    }

    public static byte[] convertToBytes(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(object);
        byte[] byteArrayObject = bos.toByteArray();

        byte a = 0;
        if (object instanceof TechnicalMessage)
            a = NetworkService.NETWORK_MESSAGE_TYPE.TECHNICAL_MESSAGE;
        else if (object instanceof MinifiedMessage)
            a = NetworkService.NETWORK_MESSAGE_TYPE.MESSAGE;
        else if (object instanceof MinifiedUser)
            a = NetworkService.NETWORK_MESSAGE_TYPE.USER_UPDATE;

        if (0 != a) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(a);
            outputStream.write(byteArrayObject);
            return outputStream.toByteArray();
        } else {
            return null;
        }
    }

    public static HashMap<Byte, Object> convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {

        byte technicalByte = bytes[0];
        byte[] filteredByteArray = Arrays.copyOfRange(bytes, 1, bytes.length);

        ByteArrayInputStream bis = new ByteArrayInputStream(filteredByteArray);
        ObjectInput in = new ObjectInputStream(bis);
        HashMap<Byte, Object> payload = new HashMap<>();
        Object object = in.readObject();
        in.close();
        payload.put(technicalByte, object);
        return payload;
    }

}
