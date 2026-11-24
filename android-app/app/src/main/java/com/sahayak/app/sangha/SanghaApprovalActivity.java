package com.sahayak.app.sangha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.sahayak.app.HomeActivity;
import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.User;

/**
 * Sangha Approval & Verification Status Screen.
 * Displays approval status for newly registered or pending Sanghas.
 * In accordance with Phase 1 requirements:
 * - Pending Sanghas view an informative verification-in-progress screen.
 * - Rejected Sanghas view their mandatory rejection reason.
 * - Includes a demo simulation button to approve the Sangha and unlock the operational dashboard.
 */
public class SanghaApprovalActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private UserRepository userRepository;
    private User currentUser;

    private LinearLayout layoutStatusBanner;
    private ImageView ivStatusIcon;
    private TextView tvStatusHeaderTitle, tvApprovalBadge;

    private TextView tvSanghaName, tvRegNo, tvContact, tvAddress;
    private LinearLayout layoutRejectionBanner, layoutPendingNotice;
    private TextView tvRejectionReason;
    private MaterialButton btnSimulateApprove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_approval_sangha);

        sessionManager = new SessionManager(this);
        userRepository = UserRepository.getInstance();

        currentUser = sessionManager.getCurrentUser();

        initViews();
        displayStatus();
    }

    private void initViews() {
        layoutStatusBanner = findViewById(R.id.layoutStatusBanner);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvStatusHeaderTitle = findViewById(R.id.tvStatusHeaderTitle);
        tvApprovalBadge = findViewById(R.id.tvApprovalBadge);

        tvSanghaName = findViewById(R.id.tvSanghaName);
        tvRegNo = findViewById(R.id.tvRegNo);
        tvContact = findViewById(R.id.tvContact);
        tvAddress = findViewById(R.id.tvAddress);

        layoutRejectionBanner = findViewById(R.id.layoutRejectionBanner);
        layoutPendingNotice = findViewById(R.id.layoutPendingNotice);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);

        btnSimulateApprove = findViewById(R.id.btnSimulateApprove);

        btnSimulateApprove.setOnClickListener(v -> simulateApproval());

        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
    }

    private void displayStatus() {
        if (currentUser == null) return;

        tvSanghaName.setText(currentUser.getFullName());
        tvRegNo.setText("Reg No: " + (currentUser.getRegistrationNo() != null && !currentUser.getRegistrationNo().isEmpty()
                ? currentUser.getRegistrationNo() : "REG-SNG-2025-PENDING"));
        tvContact.setText("Contact: " + currentUser.getMobile() + " | " + currentUser.getEmail());
        tvAddress.setText("Address: " + (currentUser.getAddress() != null ? currentUser.getAddress() : "Mangaluru, Karnataka"));

        String status = currentUser.getApprovalStatus();

        if (User.STATUS_REJECTED.equalsIgnoreCase(status)) {
            // Rejected State
            layoutStatusBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected));
            ivStatusIcon.setImageResource(R.drawable.ic_warning);
            tvStatusHeaderTitle.setText("Registration Not Approved");
            tvApprovalBadge.setText("APPLICATION REJECTED");
            tvApprovalBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.sahayak_primary_dark));

            layoutRejectionBanner.setVisibility(View.VISIBLE);
            layoutPendingNotice.setVisibility(View.GONE);

            String reason = currentUser.getRejectionReason();
            if (reason == null || reason.trim().isEmpty()) {
                reason = sessionManager.getRejectionReason();
            }
            if (reason == null || reason.trim().isEmpty()) {
                reason = "Official NGO Darpan verification check could not be validated with state registry.";
            }
            tvRejectionReason.setText(reason);

            btnSimulateApprove.setVisibility(View.GONE);

        } else {
            // Pending State
            layoutStatusBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.status_verification));
            ivStatusIcon.setImageResource(R.drawable.ic_clock);
            tvStatusHeaderTitle.setText("Registration Under Review");
            tvApprovalBadge.setText("PENDING VERIFICATION");

            layoutRejectionBanner.setVisibility(View.GONE);
            layoutPendingNotice.setVisibility(View.VISIBLE);
            btnSimulateApprove.setVisibility(View.VISIBLE);
        }
    }

    private void simulateApproval() {
        if (currentUser == null) return;

        userRepository.updateApprovalStatus(currentUser.getUserId(), User.STATUS_APPROVED);
        sessionManager.updateApprovalStatus(User.STATUS_APPROVED);

        Toast.makeText(this, "Sangha Approved! Accessing NGO Operational Dashboard...", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, SanghaDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void performLogout() {
        sessionManager.clearSession();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

