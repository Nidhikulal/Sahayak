package com.sahayak.app.sangha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.HomeActivity;
import com.sahayak.app.R;
import com.sahayak.app.data.FirebaseManager;
import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.User;
import com.sahayak.app.sangha.data.SanghaDataRepository;
import com.sahayak.app.sangha.data.SanghaOperationalStats;
import com.sahayak.app.sangha.data.SanghaRepositoryInterface;

import java.util.Locale;

/**
 * Sangha Dashboard Activity.
 * Serves as the central operational cockpit for approved NGOs/Sanghas.
 * Displays real-time operational metrics: assigned requests, verification pipeline,
 * active aid deliveries, completed cases, and received sponsorships.
 */
public class SanghaDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private SanghaRepositoryInterface sanghaRepository;

    private User currentUser;
    private String currentSanghaId = "SNG-001";
    private Sangha currentSanghaProfile;

    private TextView tvSanghaOrgName, tvSanghaRegNo, tvSanghaStatusBadge;
    private TextView tvTotalAssignedCount, tvPendingVerificationCount, tvApprovedCount, tvInProgressCount, tvCompletedCount;
    private TextView tvTotalSponsorshipsAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        sanghaRepository = SanghaDataRepository.getInstance();

        if (!validateSessionAndApproval()) {
            return;
        }

        setContentView(R.layout.activity_sangha_dashboard_sangha);

        initViews();
        setupNavigation();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (validateSessionAndApproval()) {
            loadDashboardData();
        }
    }

    private boolean validateSessionAndApproval() {
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, SanghaLoginActivity.class));
            finish();
            return false;
        }

        currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || !User.ROLE_SANGHA.equalsIgnoreCase(currentUser.getRole())) {
            Toast.makeText(this, "Access restricted to registered Sangha accounts", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return false;
        }

        String approvalStatus = currentUser.getApprovalStatus();
        if (!User.STATUS_APPROVED.equalsIgnoreCase(approvalStatus)) {
            Intent intent = new Intent(this, SanghaApprovalActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return false;
        }

        // Resolve Sangha ID
        currentSanghaId = sessionManager.getSanghaId();
        if (currentSanghaId == null || currentSanghaId.trim().isEmpty()) {
            if (currentUser.getSanghaId() != null && !currentUser.getSanghaId().trim().isEmpty()) {
                currentSanghaId = currentUser.getSanghaId();
            } else {
                // Match by Sangha name in SanghaRepository or default to SNG-001
                currentSanghaId = "SNG-001";
            }
        }
        return true;
    }

    private void initViews() {
        tvSanghaOrgName = findViewById(R.id.tvSanghaOrgName);
        tvSanghaRegNo = findViewById(R.id.tvSanghaRegNo);
        tvSanghaStatusBadge = findViewById(R.id.tvSanghaStatusBadge);

        tvTotalAssignedCount = findViewById(R.id.tvTotalAssignedCount);
        tvPendingVerificationCount = findViewById(R.id.tvPendingVerificationCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvTotalSponsorshipsAmount = findViewById(R.id.tvTotalSponsorshipsAmount);

        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
    }

    private void setupNavigation() {
        // Option 1: Assigned Requests
        findViewById(R.id.cardNavAssignedRequests).setOnClickListener(v -> {
            Toast.makeText(this, "Assigned Requests pipeline (Phase 2)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardTotalAssigned).setOnClickListener(v -> {
            Toast.makeText(this, "Assigned Requests pipeline (Phase 2)", Toast.LENGTH_SHORT).show();
        });

        // Option 2: Field Verification
        findViewById(R.id.cardNavVerification).setOnClickListener(v -> {
            Toast.makeText(this, "Field Verification module (Phase 3)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardPendingVerification).setOnClickListener(v -> {
            Toast.makeText(this, "Field Verification module (Phase 3)", Toast.LENGTH_SHORT).show();
        });

        // Option 3: Assistance Tracking
        findViewById(R.id.cardNavAssistanceTracking).setOnClickListener(v -> {
            Toast.makeText(this, "Assistance Tracking & Delivery (Phase 4)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardApprovedAwaitingAid).setOnClickListener(v -> {
            Toast.makeText(this, "Assistance Tracking & Delivery (Phase 4)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardInProgressAid).setOnClickListener(v -> {
            Toast.makeText(this, "Assistance Tracking & Delivery (Phase 4)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardCompletedCases).setOnClickListener(v -> {
            Toast.makeText(this, "Assistance Tracking & Delivery (Phase 4)", Toast.LENGTH_SHORT).show();
        });

        // Option 4: Donations & Funds
        findViewById(R.id.cardNavDonations).setOnClickListener(v -> {
            Toast.makeText(this, "Donations & Sponsorships Overview (Phase 5)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardTotalSponsorships).setOnClickListener(v -> {
            Toast.makeText(this, "Donations & Sponsorships Overview (Phase 5)", Toast.LENGTH_SHORT).show();
        });

        // Option 5: Sangha Profile
        findViewById(R.id.cardNavProfile).setOnClickListener(v -> {
            Toast.makeText(this, "Sangha Profile & Service Offerings (Phase 5)", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDashboardData() {
        currentSanghaProfile = sanghaRepository.getSanghaProfile(currentSanghaId);

        String orgName = currentSanghaProfile != null ? currentSanghaProfile.getName() : currentUser.getFullName();
        String regNo = currentSanghaProfile != null ? currentSanghaProfile.getRegistrationNo() : currentUser.getRegistrationNo();
        if (regNo == null || regNo.isEmpty()) {
            regNo = "REG-SNG-2021-042";
        }

        tvSanghaOrgName.setText(orgName);
        tvSanghaRegNo.setText(regNo);
        tvSanghaStatusBadge.setText("APPROVED PARTNER");

        // Query operational stats from decoupled repository
        SanghaOperationalStats stats = sanghaRepository.getOperationalStats(currentSanghaId);

        tvTotalAssignedCount.setText(String.valueOf(stats.getTotalAssigned()));
        tvPendingVerificationCount.setText(String.valueOf(stats.getPendingVerification()));
        tvApprovedCount.setText(String.valueOf(stats.getApprovedAwaitingAid()));
        tvInProgressCount.setText(String.valueOf(stats.getAssistanceInProgress()));
        tvCompletedCount.setText(String.valueOf(stats.getCompletedCases()));

        tvTotalSponsorshipsAmount.setText("₹" + String.format(Locale.US, "%,.0f", stats.getTotalSponsorships()));
    }

    private void performLogout() {
        FirebaseManager.getInstance().signOut();
        sessionManager.clearSession();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SanghaLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

