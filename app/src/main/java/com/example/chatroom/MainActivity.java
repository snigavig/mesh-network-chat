package com.example.chatroom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.example.chatroom.fragments.ChatFragment;
import com.example.chatroom.fragments.MemebersFragment;
import com.example.chatroom.fragments.ProfileFragment;
import com.example.chatroom.model.User;
import com.example.chatroom.util.DBHelper;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int SELECT_PHOTO = 100;
    private ProgressBar progressBar;
    private FrameLayout mainLayer;
    private FrameLayout blockingLayer;
    private ViewPager viewPager;
    private ProfileFragment profileFragment = new ProfileFragment();
    private ChatFragment chatFragment = new ChatFragment();
    private MemebersFragment memebersFragment = new MemebersFragment();

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String messageType = intent.getExtras().getString(NetworkService.BROADCAST_MESSAGE_TYPE_KEY);
            Log.d("BROADCAST MESSAGE", messageType);
            if (messageType != null) {
                switch (messageType) {
                    case NetworkService.BROADCAST_ACTION.NETWORK_INITIALISATION_COMPLETED_KEY:
                        mainLayer.setClickable(true);
                        blockingLayer.setClickable(false);
                        progressBar.setVisibility(View.GONE);
                        viewPager.setVisibility(View.VISIBLE);
                        profileFragment.refreshProfileUI();
                        chatFragment.refreshChat();
                        memebersFragment.refreshUserList();
                        break;
                    case NetworkService.BROADCAST_ACTION.REFRESH_CHAT_KEY:
                        chatFragment.refreshChat();
                        break;
                    case NetworkService.BROADCAST_ACTION.REFRESH_MEMBERS_KEY:
                        memebersFragment.refreshUserList();
                        chatFragment.refreshChat();
                        break;
                    case NetworkService.BROADCAST_ACTION.REFRESH_USER_PROFILE_KEY:
                        profileFragment.refreshProfileUI();
                        break;
                }
            }
        }
    };

    private static Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width) ? height - (height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0) ? 0 : cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0) ? 0 : cropH;
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(profileFragment, "PROFILE");
        adapter.addFrag(chatFragment, "CHAT ROOM");
        adapter.addFrag(memebersFragment, "MEMBERS");
        viewPager.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(NetworkService.NETWORK_BROADCAST_KEY));

        Intent serviceIntent = new Intent(this, NetworkService.class);
        serviceIntent.setAction(NetworkService.ACTION.START_FOREGROUND_ACTION);
        startService(serviceIntent);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        mainLayer = (FrameLayout) findViewById(R.id.main_layer);
        blockingLayer = (FrameLayout) findViewById(R.id.blocking_layer);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        progressBar.setVisibility(View.VISIBLE);

        viewPager.setVisibility(View.GONE);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                switch (tab.getPosition()) {
                    case 0:
                        hideKeyboard();
                        break;
                    case 1:
                        break;
                    case 2:
                        hideKeyboard();
                        break;
                }
            }


            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }


            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        mainLayer.setClickable(false);
        blockingLayer.setClickable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(this, NetworkService.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(NetworkService.ACTIVITY_FOREGROUND_KEY, true);
        serviceIntent.putExtras(bundle);
        serviceIntent.setAction(NetworkService.ACTION.UPDATE_ACTION);
        startService(serviceIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent serviceIntent = new Intent(this, NetworkService.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(NetworkService.ACTIVITY_FOREGROUND_KEY, false);
        serviceIntent.putExtras(bundle);
        serviceIntent.setAction(NetworkService.ACTION.UPDATE_ACTION);
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        Bitmap bitmp = decodeUri(selectedImage);
                        User user = DBHelper.updateSelfAvatar(cropToSquare(bitmp));
                        profileFragment.refreshProfileUI();
                        Intent serviceIntent = new Intent(this, NetworkService.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(NetworkService.MINIFIED_OBJECT_SERIALIZABLE_EXTRA_KEY, user.minified());
                        serviceIntent.putExtras(bundle);
                        serviceIntent.setAction(NetworkService.ACTION.SEND_MULTICAST_ACTION);
                        startService(serviceIntent);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        final int REQUIRED_SIZE = 40;

        int scale = 1;
        if (o.outHeight > REQUIRED_SIZE || o.outWidth > REQUIRED_SIZE) {
            scale = (int) Math.pow(2, (int) Math.ceil(Math.log(REQUIRED_SIZE /
                    (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
        }

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();


        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }


        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }


        @Override
        public int getCount() {
            return mFragmentList.size();
        }


        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }


        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}