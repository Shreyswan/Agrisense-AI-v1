package com.example.agrisense_ai;

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Apply edge-to-edge insets handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.plant_disease_detector) {
                selectedFragment = new Plant_disease_detectorFragment();
                updateBottomNavColors(R.color.plant_bg, R.color.plant_active, R.color.plant_inactive);
            } else if (itemId == R.id.chatbot) {
                selectedFragment = new chatbotFragment();
                updateBottomNavColors(R.color.chatbot_bg, R.color.chatbot_active, R.color.chatbot_inactive);
            } else if (itemId == R.id.video) {
                selectedFragment = new videoFragment();
                updateBottomNavColors(R.color.video_bg, R.color.video_active, R.color.video_inactive);
            }

            if (selectedFragment != null) {
                switchFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Set initial colors for default selected item
        bottomNavigationView.setSelectedItemId(R.id.plant_disease_detector);
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setReorderingAllowed(true);
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }

    private void updateBottomNavColors(@ColorRes int bgColor, @ColorRes int activeColor, @ColorRes int inactiveColor) {
        // Update background color
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, bgColor));

        // Create color state list for icon and text colors
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked }, // checked state
                new int[] { -android.R.attr.state_checked } // unchecked state
        };

        int[] colors = new int[] {
                ContextCompat.getColor(this, activeColor),
                ContextCompat.getColor(this, inactiveColor)
        };

        ColorStateList colorStateList = new ColorStateList(states, colors);
        bottomNavigationView.setItemIconTintList(colorStateList);
        bottomNavigationView.setItemTextColor(colorStateList);

        // Update system navigation bar color (Android Q+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, bgColor));
        }
    }
}