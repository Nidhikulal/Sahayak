package com.sahayak.app.sangha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sahayak.app.HomeActivity;
import com.sahayak.app.R;
import com.sahayak.app.citizen.AssistanceRequest;
import com.sahayak.app.citizen.RequestDetailsActivity;
import com.sahayak.app.citizen.RequestStatus;
import com.sahayak.app.data.FirebaseManager;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.User;
import com.sahayak.app.sangha.data.SanghaDataRepository;
import com.sahayak.app.sangha.data.SanghaOperationalStats;
import com.sahayak.app.sangha.data.SanghaRepositoryInterface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Sangha Dashboard Activity.
 * Serves as the central operational cockpit for approved NGOs/Sanghas.
 * Displays real-time operational metrics, interactive statistics cards,
 * and a button to open the separate Funds & Financial Management page.
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
        // 1. Total Assigned Requests Card
        findViewById(R.id.cardTotalAssigned).setOnClickListener(v ->
                showRequestsListDialog("ALL", "All Assigned Requests"));
        findViewById(R.id.cardNavAssignedRequests).setOnClickListener(v ->
                showRequestsListDialog("ALL", "All Assigned Requests"));

        // 2. Pending Verification Card
        findViewById(R.id.cardPendingVerification).setOnClickListener(v ->
                showRequestsListDialog("PENDING", "Pending Verification Requests"));
        findViewById(R.id.cardNavVerification).setOnClickListener(v ->
                showRequestsListDialog("PENDING", "Pending Verification Requests"));

        // 3. Verified Cases Card
        findViewById(R.id.cardApprovedAwaitingAid).setOnClickListener(v ->
                showRequestsListDialog("VERIFIED", "Verified Cases"));
        findViewById(R.id.cardNavAssistanceTracking).setOnClickListener(v ->
                showRequestsListDialog("VERIFIED", "Verified Cases"));

        // 4. Aid In Progress Card
        findViewById(R.id.cardInProgressAid).setOnClickListener(v ->
                showRequestsListDialog("IN_PROGRESS", "Aid In Progress"));

        // 5. Cases Completed Card
        findViewById(R.id.cardCompletedCases).setOnClickListener(v ->
                showRequestsListDialog("COMPLETED", "Completed Cases"));

        // 6. Community Sponsorships Received Card
        findViewById(R.id.cardTotalSponsorships).setOnClickListener(v ->
                showDonationsListDialog());
        findViewById(R.id.cardNavDonations).setOnClickListener(v ->
                showDonationsListDialog());

        // 7. View Funds & Financial Management Button
        findViewById(R.id.btnViewFundsManagement).setOnClickListener(v -> {
            Intent intent = new Intent(this, SanghaFundsActivity.class);
            intent.putExtra("SANGHA_ID", currentSanghaId);
            startActivity(intent);
        });

        // Option 5: Sangha Profile
        findViewById(R.id.cardNavProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, SanghaProfileActivity.class);
            intent.putExtra("SANGHA_ID", currentSanghaId);
            startActivity(intent);
        });
    }

    private void showRequestsListDialog(String filterType, String dialogTitle) {
        List<AssistanceRequest> allAssigned = sanghaRepository.getAssignedRequests(currentSanghaId);
        List<AssistanceRequest> filtered = new ArrayList<>();

        for (AssistanceRequest r : allAssigned) {
            if (r.status == null) continue;
            if ("ALL".equalsIgnoreCase(filterType)) {
                filtered.add(r);
            } else if ("PENDING".equalsIgnoreCase(filterType)) {
                if (RequestStatus.SUBMITTED.equalsIgnoreCase(r.status) ||
                        RequestStatus.ASSIGNED.equalsIgnoreCase(r.status) ||
                        RequestStatus.UNDER_VERIFICATION.equalsIgnoreCase(r.status)) {
                    filtered.add(r);
                }
            } else if ("VERIFIED".equalsIgnoreCase(filterType)) {
                if (RequestStatus.VERIFIED.equalsIgnoreCase(r.status)) {
                    filtered.add(r);
                }
            } else if ("IN_PROGRESS".equalsIgnoreCase(filterType)) {
                if (RequestStatus.IN_PROGRESS.equalsIgnoreCase(r.status)) {
                    filtered.add(r);
                }
            } else if ("COMPLETED".equalsIgnoreCase(filterType)) {
                if (RequestStatus.COMPLETED.equalsIgnoreCase(r.status)) {
                    filtered.add(r);
                }
            }
        }

        if (filtered.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(dialogTitle)
                    .setMessage("No requests found in this category.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 24);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);

        for (AssistanceRequest req : filtered) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 16;
            card.setLayoutParams(lp);
            card.setRadius(16f);
            card.setCardElevation(3f);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(24, 20, 24, 20);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(req.beneficiaryName + " (" + req.assistanceType + ")");
            tvTitle.setTextSize(14f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_primary));

            TextView tvSub = new TextView(this);
            tvSub.setText("Status: " + RequestStatus.friendlyLabel(req.status) + "\nCity: " + req.city + " | Age: " + req.age);
            tvSub.setTextSize(12f);
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_secondary));
            tvSub.setPadding(0, 4, 0, 12);

            MaterialButton btnView = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnView.setText("View Details");
            btnView.setAllCaps(false);
            btnView.setTextSize(12f);
            btnView.setOnClickListener(v -> {
                Intent intent = new Intent(this, RequestDetailsActivity.class);
                intent.putExtra("REQUEST_ID", req.requestId);
                startActivity(intent);
            });

            inner.addView(tvTitle);
            inner.addView(tvSub);
            inner.addView(btnView);
            card.addView(inner);
            container.addView(card);
        }

        new AlertDialog.Builder(this)
                .setTitle(dialogTitle + " (" + filtered.size() + ")")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showDonationsListDialog() {
        List<Donation> donations = sanghaRepository.getReceivedDonations(currentSanghaId);

        if (donations.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Community Sponsorships")
                    .setMessage("No sponsorship donations received yet.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 24);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (Donation d : donations) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 16;
            card.setLayoutParams(lp);
            card.setRadius(16f);
            card.setCardElevation(3f);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(24, 20, 24, 20);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(d.getDonorName() + " - ₹" + String.format(Locale.US, "%,.0f", d.getAmount()));
            tvTitle.setTextSize(14f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_primary));

            String dateStr = sdf.format(new Date(d.getTransactionDate()));
            TextView tvSub = new TextView(this);
            tvSub.setText("Purpose: " + d.getPurpose() + "\nDate: " + dateStr + " | Ref: " + d.getTransactionRef());
            tvSub.setTextSize(12f);
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_secondary));
            tvSub.setPadding(0, 4, 0, 12);

            MaterialButton btnView = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnView.setText("View Details");
            btnView.setAllCaps(false);
            btnView.setTextSize(12f);
            btnView.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Donation Receipt")
                        .setMessage("Donor: " + d.getDonorName() + "\nAmount: ₹" + d.getAmount() + "\nPurpose: " + d.getPurpose() + "\nDate: " + dateStr + "\nPayment Ref: " + d.getTransactionRef())
                        .setPositiveButton("OK", null)
                        .show();
            });

            inner.addView(tvTitle);
            inner.addView(tvSub);
            inner.addView(btnView);
            card.addView(inner);
            container.addView(card);
        }

        new AlertDialog.Builder(this)
                .setTitle("Received Community Sponsorships (" + donations.size() + ")")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
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
