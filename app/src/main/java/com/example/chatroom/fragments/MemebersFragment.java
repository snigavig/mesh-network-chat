package com.example.chatroom.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.chatroom.MemberListRecyclerAdapter;
import com.example.chatroom.R;
import com.example.chatroom.model.User;
import com.example.chatroom.util.DBHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MemebersFragment extends Fragment {

    private MemberListRecyclerAdapter adapter;
    private RecyclerView recyclerView;

    public MemebersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_members, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.members_scrollableview);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        List<User> users = DBHelper.getAllUsers();
        if (users.size() > 0) {
            adapter = new MemberListRecyclerAdapter(users);
        } else {
            adapter = new MemberListRecyclerAdapter(new ArrayList<User>());
        }
        recyclerView.setAdapter(adapter);

        return view;
    }

    public void refreshUserList() {
        List<User> users = DBHelper.getAllUsers();
        if (null != recyclerView) {
            adapter = new MemberListRecyclerAdapter(users);
            recyclerView.swapAdapter(adapter, true);
        }
    }

}
