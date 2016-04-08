package com.example.chatroom.model;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Messages", id = BaseColumns._ID)
public class Message extends Model {
    private static final String COLUMN_TEXT = "Text";
    private static final String COLUMN_CREATED = "Created";
    private static final String COLUMN_UUID = "UUID";
    @Column(name = COLUMN_UUID, uniqueGroups = {"single-user-message"}, onUniqueConflicts = {Column.ConflictAction.IGNORE})
    private String uuid;
    @Column(name = COLUMN_CREATED, uniqueGroups = {"single-user-message"}, onUniqueConflicts = {Column.ConflictAction.IGNORE})
    private long created;
    @Column(name = COLUMN_TEXT)
    private String text;

    public Message() {
        super();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MinifiedMessage minified() {
        return new MinifiedMessage(this);
    }
}
