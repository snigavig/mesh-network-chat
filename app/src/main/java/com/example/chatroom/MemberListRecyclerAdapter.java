package com.example.chatroom;

import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.chatroom.model.User;
import com.example.chatroom.ui.RoundImage;
import com.example.chatroom.util.DBHelper;

import java.util.List;

public class MemberListRecyclerAdapter extends RecyclerView.Adapter<MemberListRecyclerAdapter.MemberItemViewHolder> {
    private final List<User> members;

    public MemberListRecyclerAdapter(List<User> members) {
        this.members = members;

    }

    @Override
    public MemberItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.members_list_item, viewGroup, false);
        return new MemberItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MemberItemViewHolder memberItemViewHolder, int i) {
        memberItemViewHolder.title.setText(members.get(i).getNickname());
        String avatarString = members.get(i).getAvatar();
        if (null != avatarString) {
            Bitmap btmp = DBHelper.StringToBitMap(members.get(i).getAvatar());
            RoundImage roundedImage = new RoundImage(btmp);
            memberItemViewHolder.avatarImageView.setImageDrawable(roundedImage);
        } else {
            memberItemViewHolder.avatarImageView.setImageDrawable(ContextCompat.getDrawable(ChatApplication.getInstance(), R.drawable.default_avatar));
        }
    }

    @Override
    public int getItemCount() {
        return members == null ? 0 : members.size();
    }

    class MemberItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView avatarImageView;
        final TextView title;
        final TextView subTitle;

        public MemberItemViewHolder(View itemView) {
            super(itemView);

            avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
            title = (TextView) itemView.findViewById(R.id.listitem_text);
            subTitle = (TextView) itemView.findViewById(R.id.listitem_nickname);

            itemView.setOnClickListener(this);
            subTitle.setVisibility(View.GONE);
        }

        @Override
        public void onClick(View v) {
        }
    }

}
