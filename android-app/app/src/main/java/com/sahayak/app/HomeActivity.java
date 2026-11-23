package com.sahayak.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.citizen.CitizenDashboardActivity;
import com.sahayak.app.citizen.CitizenLoginActivity;
import com.sahayak.app.citizen.RoleSelectionActivity;
import com.sahayak.app.citizen.SanghaApprovalActivity;
import com.sahayak.app.citizen.SanghaDashboardActivity;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.User;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Persistent Session Check
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser != null) {
                if (User.ROLE_CITIZEN.equalsIgnoreCase(currentUser.getRole())) {
                    startActivity(new Intent(this, CitizenDashboardActivity.class));
                    finish();
                    return;
                } else if (User.ROLE_SANGHA.equalsIgnoreCase(currentUser.getRole())) {
                    if (User.STATUS_APPROVED.equalsIgnoreCase(currentUser.getApprovalStatus())) {
                        startActivity(new Intent(this, SanghaDashboardActivity.class));
                    } else {
                        startActivity(new Intent(this, SanghaApprovalActivity.class));
                    }
                    finish();
                    return;
                }
            }
        }

        setContentView(R.layout.activity_home);

        findViewById(R.id.btnHeaderRegister).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RoleSelectionActivity.class));
        });

        findViewById(R.id.btnGetStarted).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RoleSelectionActivity.class));
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CitizenLoginActivity.class));
        });
    }
}