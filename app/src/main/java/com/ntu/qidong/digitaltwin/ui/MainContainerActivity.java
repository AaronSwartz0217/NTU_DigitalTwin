package com.ntu.qidong.digitaltwin.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.db.AppDatabase;

public class MainContainerActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private Fragment currentFragment;
    private ForumFragment forumFragment;
    private MessageFragment messageFragment;
    private ProfileFragment profileFragment;

    private SharedPreferences userPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        initDefaultUser();

        bottomNavigation = findViewById(R.id.bottom_navigation);

        forumFragment = new ForumFragment();
        messageFragment = new MessageFragment();
        profileFragment = new ProfileFragment();

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_forum) {
                showFragment(forumFragment);
                return true;
            } else if (itemId == R.id.nav_message) {
                showFragment(messageFragment);
                return true;
            } else if (itemId == R.id.nav_visualization) {
                Intent intent = new Intent(MainContainerActivity.this, SplashActivity.class);
                startActivity(intent);
                return false;
            } else if (itemId == R.id.nav_profile) {
                showFragment(profileFragment);
                return true;
            }
            return false;
        });

        showFragment(forumFragment);
    }

    private void initDefaultUser() {
        if (!userPrefs.contains("default_user_created")) {
            AppDatabase db = AppDatabase.getInstance(this);
            com.ntu.qidong.digitaltwin.db.User defaultUser = new com.ntu.qidong.digitaltwin.db.User("1", "1", "默认用户");
            db.userDao().insert(defaultUser);
            userPrefs.edit().putBoolean("default_user_created", true).apply();
        }
    }

    public void showFragment(Fragment fragment) {
        if (currentFragment == fragment) return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        if (fragment.isAdded()) {
            transaction.show(fragment);
        } else {
            transaction.add(R.id.fragment_container, fragment);
        }

        transaction.commitAllowingStateLoss();
        currentFragment = fragment;
    }

    public BottomNavigationView getBottomNavigation() {
        return bottomNavigation;
    }
}