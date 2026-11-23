package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.HomeActivity;
import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.User;

public class SanghaDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_dashboard);

        sessionManager = new SessionManager(this);

        User currentUser = sessionManager.getCurrentUser();
        TextView tvWelcome = findViewById(R.id.tvWelcomeSangha);

        if (currentUser != null) {
            tvWelcome.setText("Welcome, " + currentUser.getFullName() + "!");
        }

        findViewById(R.id.btnViewAssignedRequests).setOnClickListener(v -> {
            Intent intent = new Intent(this, MyRequestsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            sessionManager.clearSession();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}