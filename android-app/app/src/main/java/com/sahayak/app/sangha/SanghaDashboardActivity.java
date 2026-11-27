package com.sahayak.app.sangha;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.HomeActivity;
import com.sahayak.app.R;
import com.sahayak.app.citizen.AssistanceRequest;
import com.sahayak.app.citizen.RequestDetailsActivity;
import com.sahayak.app.citizen.RequestStatus;
import com.sahayak.app.data.FirebaseManager;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.SanghaExpense;
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
 * and the Funds & Financial History module with transaction tracking and management.
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
    private TextView tvTotalFundsAmount, tvTotalDonationsAmount, tvTotalExpensesAmount, tvAvailableBalanceAmount;

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

        tvTotalFundsAmount = findViewById(R.id.tvTotalFundsAmount);
        tvTotalDonationsAmount = findViewById(R.id.tvTotalDonationsAmount);
        tvTotalExpensesAmount = findViewById(R.id.tvTotalExpensesAmount);
        tvAvailableBalanceAmount = findViewById(R.id.tvAvailableBalanceAmount);

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

        // 7. Manage Funds Button & Card
        findViewById(R.id.btnManageFunds).setOnClickListener(v -> showManageFundsActionDialog());
        findViewById(R.id.cardFundsFinancialHistory).setOnClickListener(v -> showManageFundsActionDialog());

        // Option 5: Sangha Profile
        findViewById(R.id.cardNavProfile).setOnClickListener(v -> {
            Toast.makeText(this, "Sangha Profile & Service Offerings", Toast.LENGTH_SHORT).show();
        });
    }

    private void showManageFundsActionDialog() {
        String[] actions = new String[]{
                "1. Add New Donation",
                "2. Record New Expense",
                "3. Update Total Annual Budget",
                "4. View Financial History & Contributions"
        };

        new AlertDialog.Builder(this)
                .setTitle("Manage Funds & Financial Ledger")
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAddDonationDialog();
                            break;
                        case 1:
                            showAddExpenseDialog();
                            break;
                        case 2:
                            showUpdateBudgetDialog();
                            break;
                        case 3:
                            showFinancialHistoryDialog();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddDonationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Donation / Sponsorship");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText etDonor = new EditText(this);
        etDonor.setHint("Donor Name *");
        layout.addView(etDonor);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("Amount (₹) *");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        final EditText etPurpose = new EditText(this);
        etPurpose.setHint("Purpose / Description *");
        layout.addView(etPurpose);

        final AutoCompleteTextView actvCategory = new AutoCompleteTextView(this);
        actvCategory.setHint("Service Category *");
        String[] cats = {"Medical Treatment", "Groceries and Food", "Education Fees", "Financial Assistance", "General Welfare Fund"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cats);
        actvCategory.setAdapter(adapter);
        layout.addView(actvCategory);

        final EditText etRef = new EditText(this);
        etRef.setHint("Transaction Reference / UPI Ref *");
        layout.addView(etRef);

        builder.setView(layout);
        builder.setPositiveButton("Save Donation", (dialog, which) -> {
            String donor = etDonor.getText().toString().trim();
            String amtStr = etAmount.getText().toString().trim();
            String purpose = etPurpose.getText().toString().trim();
            String cat = actvCategory.getText().toString().trim();
            String ref = etRef.getText().toString().trim();

            if (donor.isEmpty() || amtStr.isEmpty() || purpose.isEmpty() || cat.isEmpty() || ref.isEmpty()) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amt = Double.parseDouble(amtStr);
                Donation d = new Donation(
                        "DON-" + System.currentTimeMillis(),
                        "usr-cit-001",
                        donor,
                        currentSanghaId,
                        tvSanghaOrgName.getText().toString(),
                        Donation.TYPE_GENERAL,
                        null,
                        amt,
                        purpose,
                        ref,
                        "Online Transfer"
                );
                sanghaRepository.addDonation(d);
                loadDashboardData();
                Toast.makeText(this, "Donation of ₹" + amt + " recorded successfully!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount entered.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record New Expense");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("Expense Amount (₹) *");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        final EditText etPurpose = new EditText(this);
        etPurpose.setHint("Purpose / Details *");
        layout.addView(etPurpose);

        final AutoCompleteTextView actvCategory = new AutoCompleteTextView(this);
        actvCategory.setHint("Service Category *");
        String[] cats = {"Medical Treatment", "Groceries and Food", "Education Fees", "Financial Assistance", "General Welfare Fund"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cats);
        actvCategory.setAdapter(adapter);
        layout.addView(actvCategory);

        final EditText etReqId = new EditText(this);
        etReqId.setHint("Beneficiary / Request ID (Optional)");
        layout.addView(etReqId);

        final EditText etRef = new EditText(this);
        etRef.setHint("Transaction Ref / Receipt No. *");
        layout.addView(etRef);

        builder.setView(layout);
        builder.setPositiveButton("Save Expense", (dialog, which) -> {
            String amtStr = etAmount.getText().toString().trim();
            String purpose = etPurpose.getText().toString().trim();
            String cat = actvCategory.getText().toString().trim();
            String reqId = etReqId.getText().toString().trim();
            String ref = etRef.getText().toString().trim();

            if (amtStr.isEmpty() || purpose.isEmpty() || cat.isEmpty() || ref.isEmpty()) {
                Toast.makeText(this, "Required fields cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amt = Double.parseDouble(amtStr);
                SanghaExpense expense = new SanghaExpense(
                        "EXP-" + System.currentTimeMillis(),
                        currentSanghaId,
                        amt,
                        purpose,
                        cat,
                        reqId.isEmpty() ? null : reqId,
                        ref
                );
                sanghaRepository.addExpense(expense);
                loadDashboardData();
                Toast.makeText(this, "Expense of ₹" + amt + " recorded successfully!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount entered.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showUpdateBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Total Annual Budget / Funds");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText etBudget = new EditText(this);
        etBudget.setHint("New Total Funds / Budget (₹) *");
        etBudget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etBudget.setText(String.valueOf(sanghaRepository.getAnnualBudget(currentSanghaId)));
        layout.addView(etBudget);

        builder.setView(layout);
        builder.setPositiveButton("Update Budget", (dialog, which) -> {
            String bStr = etBudget.getText().toString().trim();
            if (bStr.isEmpty()) return;
            try {
                double newBudget = Double.parseDouble(bStr);
                sanghaRepository.setAnnualBudget(currentSanghaId, newBudget);
                loadDashboardData();
                Toast.makeText(this, "Annual budget updated to ₹" + newBudget, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showFinancialHistoryDialog() {
        List<Donation> donations = sanghaRepository.getReceivedDonations(currentSanghaId);
        List<SanghaExpense> expenses = sanghaRepository.getSanghaExpenses(currentSanghaId);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 24);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        TextView tvHeaderD = new TextView(this);
        tvHeaderD.setText("--- Donations Received (" + donations.size() + ") ---");
        tvHeaderD.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeaderD.setTextColor(ContextCompat.getColor(this, R.color.sahayak_primary));
        container.addView(tvHeaderD);

        for (Donation d : donations) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = 12;
            lp.bottomMargin = 12;
            card.setLayoutParams(lp);
            card.setRadius(12f);
            card.setCardElevation(2f);

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(20, 16, 20, 16);

            TextView tv = new TextView(this);
            tv.setText("Donor: " + d.getDonorName() + " | +₹" + d.getAmount() + "\nPurpose: " + d.getPurpose() + "\nCategory: " + (d.getContributionType() != null ? d.getContributionType() : "General") + "\nDate: " + sdf.format(new Date(d.getTransactionDate())) + " | Ref: " + d.getTransactionRef());
            tv.setTextSize(12f);
            tv.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_primary));
            inner.addView(tv);
            card.addView(inner);
            container.addView(card);
        }

        TextView tvHeaderE = new TextView(this);
        tvHeaderE.setText("\n--- Expenses Incurred (" + expenses.size() + ") ---");
        tvHeaderE.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeaderE.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        container.addView(tvHeaderE);

        for (SanghaExpense exp : expenses) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = 12;
            lp.bottomMargin = 12;
            card.setLayoutParams(lp);
            card.setRadius(12f);
            card.setCardElevation(2f);

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(20, 16, 20, 16);

            TextView tv = new TextView(this);
            tv.setText("Amount: -₹" + exp.getAmount() + " | Category: " + exp.getServiceCategory() + "\nPurpose: " + exp.getPurpose() + (exp.getRequestId() != null && !exp.getRequestId().isEmpty() ? "\nRequest ID: " + exp.getRequestId() : "") + "\nDate: " + sdf.format(new Date(exp.getExpenseDate())) + " | Ref: " + exp.getTransactionRef());
            tv.setTextSize(12f);
            tv.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_primary));
            inner.addView(tv);
            card.addView(inner);
            container.addView(card);
        }

        new AlertDialog.Builder(this)
                .setTitle("Financial Ledger & Transaction History")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
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

        // Financial Ledger Metrics
        double totalFunds = sanghaRepository.getAnnualBudget(currentSanghaId);
        double totalDonations = sanghaRepository.getTotalDonationsAmount(currentSanghaId);
        double totalExpenses = sanghaRepository.getTotalExpensesAmount(currentSanghaId);
        double availableBalance = sanghaRepository.getAvailableBalance(currentSanghaId);

        tvTotalFundsAmount.setText("₹" + String.format(Locale.US, "%,.0f", totalFunds));
        tvTotalDonationsAmount.setText("₹" + String.format(Locale.US, "%,.0f", totalDonations));
        tvTotalExpensesAmount.setText("₹" + String.format(Locale.US, "%,.0f", totalExpenses));
        tvAvailableBalanceAmount.setText("₹" + String.format(Locale.US, "%,.0f", availableBalance));
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
