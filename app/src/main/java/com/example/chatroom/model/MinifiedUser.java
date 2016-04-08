package com.example.chatroom.model;

import java.io.Serializable;

public class MinifiedUser implements Serializable {

    public final String nickname;
    public final long modified;
    public final String avatar;
    public final String uuid;
    public final String remoteEndpointId;

    public MinifiedUser(User user) {
        this.nickname = user.getNickname();
        this.modified = user.getModified();
        this.avatar = user.getAvatar();
        this.uuid = user.getUuid();
        this.remoteEndpointId = user.getRemoteEndpointId();
    }

    public MinifiedUser(Object object) {
        MinifiedUser incomingUser = (MinifiedUser) object;
        this.nickname = incomingUser.nickname;
        this.modified = incomingUser.modified;
        this.avatar = incomingUser.avatar;
        this.uuid = incomingUser.uuid;
        this.remoteEndpointId = incomingUser.remoteEndpointId;
    }
}
