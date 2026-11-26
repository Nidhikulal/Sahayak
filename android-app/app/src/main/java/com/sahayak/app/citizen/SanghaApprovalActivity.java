package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.HomeActivity;
import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.User;

public class SanghaApprovalActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_approval);

        sessionManager = new SessionManager(this);
        userRepository = UserRepository.getInstance();

        User currentUser = sessionManager.getCurrentUser();

        TextView tvSanghaName = findViewById(R.id.tvSanghaName);
        TextView tvRegNo = findViewById(R.id.tvRegNo);
        TextView tvContact = findViewById(R.id.tvContact);
        TextView tvApprovalBadge = findViewById(R.id.tvApprovalBadge);

        if (currentUser != null) {
            tvSanghaName.setText(currentUser.getFullName());
            if (currentUser.getRegistrationNo() != null && !currentUser.getRegistrationNo().isEmpty()) {
                tvRegNo.setText("Reg No: " + currentUser.getRegistrationNo());
            } else {
                tvRegNo.setText("Reg No: REG-SNG-2025-PENDING");
            }
            tvContact.setText("Contact: " + currentUser.getMobile() + " | " + currentUser.getEmail());
            tvApprovalBadge.setText(currentUser.getApprovalStatus() != null ? currentUser.getApprovalStatus().toUpperCase() : "PENDING APPROVAL");
        }

        findViewById(R.id.btnSimulateApprove).setOnClickListener(v -> {
            if (currentUser != null) {
                userRepository.updateApprovalStatus(currentUser.getUserId(), User.STATUS_APPROVED);
                sessionManager.updateApprovalStatus(User.STATUS_APPROVED);
                Toast.makeText(this, "Sangha Approved! Accessing Operational Dashboard...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SanghaApprovalActivity.this, SanghaDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            sessionManager.clearSession();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SanghaApprovalActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
