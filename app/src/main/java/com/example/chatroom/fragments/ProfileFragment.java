package com.example.chatroom.fragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatroom.ChatApplication;
import com.example.chatroom.MainActivity;
import com.example.chatroom.NetworkService;
import com.example.chatroom.R;
import com.example.chatroom.model.User;
import com.example.chatroom.ui.RoundImage;
import com.example.chatroom.util.DBHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    private TextView nicknameTextView;
    private ImageView avatarImageView;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfileUI();
    }

    public void refreshProfileUI() {
        User user = DBHelper.getSelfUser();
        if (null != user) {
            if (null != nicknameTextView) {
                nicknameTextView.setText(user.getNickname());
            }
            if (null != avatarImageView) {
                String avatarString = user.getAvatar();
                if (null != avatarString) {
                    Bitmap btmp = DBHelper.StringToBitMap(avatarString);
                    RoundImage roundedImage = new RoundImage(btmp);
                    avatarImageView.setImageDrawable(roundedImage);
                } else {
                    avatarImageView.setImageDrawable(ContextCompat.getDrawable(ChatApplication.getInstance(), R.drawable.default_avatar));
                }

            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);
        nicknameTextView = (TextView) rootView.findViewById(R.id.nameTextView);
        avatarImageView = (ImageView) rootView.findViewById(R.id.avatarImageView);
        ImageView editAvatar = (ImageView) rootView.findViewById(R.id.editAvatarImageView);
        editAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                getActivity().startActivityForResult(photoPickerIntent, MainActivity.SELECT_PHOTO);
            }
        });

        nicknameTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (event.getRawX() >= (nicknameTextView.getRight() - nicknameTextView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final EditText edittext = new EditText(getActivity());
                        alert.setTitle("Set new nickname:");
                        alert.setView(edittext);
                        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String desiredNickname = edittext.getText().toString();
                                if (null == DBHelper.getUserByNickname(desiredNickname)) {
                                    User user = DBHelper.updateSelfNickname(desiredNickname);
                                    refreshProfileUI();

                                    Intent membersIntent = new Intent(NetworkService.NETWORK_BROADCAST_KEY);
                                    membersIntent.putExtra(NetworkService.BROADCAST_MESSAGE_TYPE_KEY, NetworkService.BROADCAST_ACTION.REFRESH_MEMBERS_KEY);
                                    LocalBroadcastManager.getInstance(ChatApplication.getInstance()).sendBroadcast(membersIntent);
                                    Intent chatIntent = new Intent(NetworkService.NETWORK_BROADCAST_KEY);
                                    chatIntent.putExtra(NetworkService.BROADCAST_MESSAGE_TYPE_KEY, NetworkService.BROADCAST_ACTION.REFRESH_CHAT_KEY);
                                    LocalBroadcastManager.getInstance(ChatApplication.getInstance()).sendBroadcast(chatIntent);

                                    Intent serviceIntent = new Intent(ChatApplication.getInstance(), NetworkService.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putSerializable(NetworkService.MINIFIED_OBJECT_SERIALIZABLE_EXTRA_KEY, user.minified());
                                    serviceIntent.putExtras(bundle);
                                    serviceIntent.setAction(NetworkService.ACTION.SEND_MULTICAST_ACTION);
                                    ChatApplication.getInstance().startService(serviceIntent);
                                } else {
                                    Toast.makeText(ChatApplication.getInstance(), "Nickname " + desiredNickname + " already taken", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        });
                        alert.show();
                        return true;
                    }
                }
                return false;
            }
        });


        return rootView;
    }
}
