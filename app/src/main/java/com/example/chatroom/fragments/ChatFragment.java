package com.example.chatroom.fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.chatroom.ChatRecyclerAdapter;
import com.example.chatroom.MainActivity;
import com.example.chatroom.NetworkService;
import com.example.chatroom.R;
import com.example.chatroom.model.Message;
import com.example.chatroom.model.User;
import com.example.chatroom.util.DBHelper;
import com.example.chatroom.util.SecurityHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    private EditText messageEditText;

    private ChatRecyclerAdapter adapter;
    private RecyclerView recyclerView;
    private WeakReference<MainActivity> mainActivityWeakReference;
    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mainActivityWeakReference = new WeakReference<>((MainActivity) activity);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        messageEditText = (EditText) rootView.findViewById(R.id.messageEditText);
        ImageView sendButton = (ImageView) rootView.findViewById(R.id.sendImageButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                User user = DBHelper.getSelfUser();
                Message message = new Message();
                message.setCreated(System.currentTimeMillis());
                message.setUuid(user.getUuid());
                message.setText(SecurityHelper.encryptIt(String.valueOf(messageEditText.getText())));
                message.save();
                Intent serviceIntent = new Intent(getActivity(), NetworkService.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(NetworkService.MINIFIED_OBJECT_SERIALIZABLE_EXTRA_KEY, message.minified());
                serviceIntent.putExtras(bundle);
                serviceIntent.setAction(NetworkService.ACTION.SEND_MULTICAST_ACTION);
                getActivity().startService(serviceIntent);
                messageEditText.setText("");
                refreshChat();
            }
        });
        recyclerView = (RecyclerView) rootView.findViewById(R.id.chat_scrollableview);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);


        List<Message> messages = DBHelper.getLastTwentyMessages();
        List<Message> filteredMessages = new ArrayList<>();
        for (Message message:messages) {
            if(null != DBHelper.getUserByUUID(message.getUuid()))
                filteredMessages.add(message);
        }
        if (filteredMessages.size() > 0) {
            adapter = new ChatRecyclerAdapter(filteredMessages, ShareCompat.IntentBuilder.from(mainActivityWeakReference.get()));
        } else {
            adapter = new ChatRecyclerAdapter(new ArrayList<Message>(), ShareCompat.IntentBuilder.from(mainActivityWeakReference.get()));
        }
        recyclerView.setAdapter(adapter);
        return rootView;
    }

    public void refreshChat(){
        if (null != mainActivityWeakReference) {
            List<Message> messages = DBHelper.getLastTwentyMessages();
            List<Message> filteredMessages = new ArrayList<>();
            for (Message message : messages) {
                if (null != DBHelper.getUserByUUID(message.getUuid()))
                    filteredMessages.add(message);
            }
            adapter = new ChatRecyclerAdapter(filteredMessages, ShareCompat.IntentBuilder.from(mainActivityWeakReference.get()));
            if (null != recyclerView) {
                recyclerView.swapAdapter(adapter, true);
            }
        }
    }
}
