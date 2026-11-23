package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.User;

public class RoleSelectionActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        sessionManager = new SessionManager(this);
        userRepository = UserRepository.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Citizen Option -> Directs to Citizen Dashboard
        ViewOnClickListener openCitizenDashboard = v -> {
            if (!sessionManager.isLoggedIn() || !User.ROLE_CITIZEN.equalsIgnoreCase(sessionManager.getUserRole())) {
                User demoCitizen = userRepository.findById("usr-cit-001");
                if (demoCitizen == null) {
                    demoCitizen = new User("usr-cit-001", "Ramesh Kumar", "citizen@sahayak.org", "9876543210", "Password@123", "12/4 MG Road, Mangaluru", User.ROLE_CITIZEN);
                    userRepository.registerUser(demoCitizen);
                }
                sessionManager.createSession(demoCitizen);
            }
            startActivity(new Intent(RoleSelectionActivity.this, CitizenDashboardActivity.class));
        };

        findViewById(R.id.cardCitizen).setOnClickListener(openCitizenDashboard);
        findViewById(R.id.btnContinueCitizen).setOnClickListener(openCitizenDashboard);

        // Sangha Option -> Directs to Sangha Approval / Registration
        ViewOnClickListener openSangha = v -> {
            if (sessionManager.isLoggedIn() && User.ROLE_SANGHA.equalsIgnoreCase(sessionManager.getUserRole())) {
                if (User.STATUS_APPROVED.equalsIgnoreCase(sessionManager.getApprovalStatus())) {
                    startActivity(new Intent(RoleSelectionActivity.this, SanghaDashboardActivity.class));
                } else {
                    startActivity(new Intent(RoleSelectionActivity.this, SanghaApprovalActivity.class));
                }
            } else {
                startActivity(new Intent(RoleSelectionActivity.this, SanghaRegistrationActivity.class));
            }
        };

        findViewById(R.id.cardSangha).setOnClickListener(openSangha);
        findViewById(R.id.btnContinueSangha).setOnClickListener(openSangha);

        // New Registration Form link
        findViewById(R.id.tvRegisterNewForm).setOnClickListener(v -> {
            startActivity(new Intent(RoleSelectionActivity.this, CitizenRegistrationActivity.class));
        });
    }

    private interface ViewOnClickListener extends android.view.View.OnClickListener {}
}