package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.HomeActivity;
import com.sahayak.app.R;
import com.sahayak.app.data.NotificationRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.User;

public class CitizenDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private NotificationRepository notificationRepository;
    private User currentUser;

    private TextView tvWelcomeUser, tvNotificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_dashboard);

        sessionManager = new SessionManager(this);
        notificationRepository = NotificationRepository.getInstance();

        currentUser = sessionManager.getCurrentUser();

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    private void initViews() {
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        if (currentUser != null && currentUser.getFullName() != null) {
            tvWelcomeUser.setText("Welcome, " + currentUser.getFullName() + "!");
        } else {
            tvWelcomeUser.setText("Welcome, Citizen!");
        }

        updateNotificationBadge();

        // Main Option 1: Help People in Need
        findViewById(R.id.cardHelpPeople).setOnClickListener(v -> {
            startActivity(new Intent(CitizenDashboardActivity.this, HelpPeopleActivity.class));
        });

        // Main Option 2: Sponsor Money to the Needy
        findViewById(R.id.cardSponsorMoney).setOnClickListener(v -> {
            startActivity(new Intent(CitizenDashboardActivity.this, SponsorDashboardActivity.class));
        });

        // Quick Navigation Buttons
        findViewById(R.id.btnMyRequests).setOnClickListener(v -> {
            startActivity(new Intent(CitizenDashboardActivity.this, MyRequestsActivity.class));
        });

        findViewById(R.id.btnDonationHistory).setOnClickListener(v -> {
            startActivity(new Intent(CitizenDashboardActivity.this, DonationHistoryActivity.class));
        });

        findViewById(R.id.btnNotificationsContainer).setOnClickListener(v -> {
            startActivity(new Intent(CitizenDashboardActivity.this, NotificationsActivity.class));
        });

        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            startActivity(new Intent(CitizenDashboardActivity.this, CitizenProfileActivity.class));
        });

        findViewById(R.id.btnQuickLogout).setOnClickListener(v -> {
            sessionManager.clearSession();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CitizenDashboardActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateNotificationBadge() {
        if (currentUser == null) return;
        int unreadCount = notificationRepository.getUnreadCount(currentUser.getUserId());
        if (unreadCount > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(String.valueOf(unreadCount));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }
}