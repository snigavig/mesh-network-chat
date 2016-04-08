package com.example.chatroom;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.chatroom.model.Message;
import com.example.chatroom.model.User;
import com.example.chatroom.ui.RoundImage;
import com.example.chatroom.util.DBHelper;
import com.example.chatroom.util.SecurityHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChatRecyclerAdapter extends RecyclerView.Adapter<ChatRecyclerAdapter.ChatMessageViewHolder> {
    private final List<Message> messages;
    private int lastPosition = -1;
    private ShareCompat.IntentBuilder shareIntentBuilder;

    public ChatRecyclerAdapter(List<Message> messages, ShareCompat.IntentBuilder builder) {
        this.messages = messages;
        shareIntentBuilder = builder.setType("text/plain");
    }

    @Override
    public ChatMessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chat_item, viewGroup, false);
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatMessageViewHolder chatMessageViewHolder, int i) {
        final String decryptedMessage = SecurityHelper.decryptIt(messages.get(i).getText());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(messages.get(i).getCreated());
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss", Locale.getDefault());
        chatMessageViewHolder.text.setText(decryptedMessage);
        User author = DBHelper.getUserByUUID(messages.get(i).getUuid());
        if (null != author) {
            final String messageHeader = ChatApplication
                    .getInstance()
                    .getString(R.string.message_header,
                            author.getNickname(),
                            formatter.format(calendar.getTime()));

            chatMessageViewHolder.v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    shareIntentBuilder.setSubject(messageHeader);
                    shareIntentBuilder.setText(decryptedMessage);
                    Intent intent = shareIntentBuilder.getIntent();
                    if (intent.resolveActivity(ChatApplication.getInstance().getPackageManager()) != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ChatApplication.getInstance().startActivity(intent);
                    }
                    return true;
                }
            });
            chatMessageViewHolder.nickname.setText(messageHeader);

            String avatarString = author.getAvatar();

            if (author.isSelf()) {
                chatMessageViewHolder.cardItemLayout.setBackgroundResource(R.drawable.out_bbbl);
                chatMessageViewHolder.cardWrapLayout.setGravity(Gravity.END);
                chatMessageViewHolder.outgoingAvatar.setVisibility(View.VISIBLE);
                chatMessageViewHolder.incomingAvatar.setVisibility(View.GONE);
                if (null != avatarString) {
                    Bitmap btmp = DBHelper.StringToBitMap(author.getAvatar());
                    RoundImage roundedImage = new RoundImage(btmp);
                    chatMessageViewHolder.outgoingAvatar.setImageDrawable(roundedImage);
                } else {
                    chatMessageViewHolder.outgoingAvatar.setImageDrawable(ContextCompat.getDrawable(ChatApplication.getInstance(), R.drawable.default_avatar));
                }
                setAnimation(chatMessageViewHolder.cardWrapLayout, i);
            } else {
                chatMessageViewHolder.cardItemLayout.setBackgroundResource(R.drawable.in_bbbl);
                chatMessageViewHolder.cardWrapLayout.setHorizontalGravity(Gravity.START);
                chatMessageViewHolder.outgoingAvatar.setVisibility(View.GONE);
                chatMessageViewHolder.incomingAvatar.setVisibility(View.VISIBLE);
                if (null != avatarString) {
                    Bitmap btmp = DBHelper.StringToBitMap(author.getAvatar());
                    RoundImage roundedImage = new RoundImage(btmp);
                    chatMessageViewHolder.incomingAvatar.setImageDrawable(roundedImage);
                } else {
                    chatMessageViewHolder.incomingAvatar.setImageDrawable(ContextCompat.getDrawable(ChatApplication.getInstance(), R.drawable.default_avatar));
                }
                setAnimation(chatMessageViewHolder.cardWrapLayout, i);
            }
        }
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation;
            animation = AnimationUtils.loadAnimation(ChatApplication.getInstance(), android.R.anim.fade_in);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout cardItemLayout;
        final LinearLayout cardWrapLayout;
        final ImageView incomingAvatar;
        final ImageView outgoingAvatar;
        final TextView text;
        final TextView nickname;
        final View v;

        public ChatMessageViewHolder(View itemView) {
            super(itemView);
            v = itemView;
            cardWrapLayout = (LinearLayout) itemView.findViewById(R.id.chat_message_wrap_wrap);
            cardItemLayout = (LinearLayout) itemView.findViewById(R.id.chat_message_wrap);
            incomingAvatar = (ImageView) itemView.findViewById(R.id.incomingMessageAvatarImageView);
            outgoingAvatar = (ImageView) itemView.findViewById(R.id.outgoingMessageAvatarImageView);
            text = (TextView) itemView.findViewById(R.id.listitem_text);
            nickname = (TextView) itemView.findViewById(R.id.listitem_nickname);
        }
    }
}
