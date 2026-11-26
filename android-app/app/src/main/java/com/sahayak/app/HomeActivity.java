package com.sahayak.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.citizen.RoleSelectionActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always display the App Introduction Page first on launch as requested!
        setContentView(R.layout.activity_home);

        findViewById(R.id.btnHeaderRegister).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RoleSelectionActivity.class));
        });

        findViewById(R.id.btnGetStarted).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RoleSelectionActivity.class));
        });
    }
}
