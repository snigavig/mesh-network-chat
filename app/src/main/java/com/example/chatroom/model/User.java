package com.example.chatroom.model;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Users", id = BaseColumns._ID)
public class User extends Model {
    public static final String COLUMN_SELF = "Self";
    public static final String COLUMN_UUID = "UUID";
    public static final String COLUMN_NAME = "Name";
    private static final String COLUMN_MODIFIED = "Modified";
    private static final String COLUMN_STATUS = "Status";
    private static final String COLUMN_AVATAR = "Avatar";
    private static final String COLUMN_REMOTE_ENDPOINT_ID = "RemoteEndpointId";
    @Column(name = COLUMN_MODIFIED)
    private long modified;
    @Column(name = COLUMN_STATUS)
    private String status;
    @Column(name = COLUMN_NAME, unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private String nickname;
    @Column(name = COLUMN_AVATAR)
    private String avatar;
    @Column(name = COLUMN_SELF)
    private boolean self = false;
    @Column(name = COLUMN_UUID)
    private String uuid;
    @Column(name = COLUMN_REMOTE_ENDPOINT_ID)
    private String remoteEndpointId;


    public User() {
        super();
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public boolean isSelf() {
        return self;
    }

    public void setSelf(boolean self) {
        this.self = self;
    }

    public MinifiedUser minified() {
        return new MinifiedUser(this);
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRemoteEndpointId() {
        return remoteEndpointId;
    }

    public void setRemoteEndpointId(String remoteEndpointId) {
        this.remoteEndpointId = remoteEndpointId;
    }
}