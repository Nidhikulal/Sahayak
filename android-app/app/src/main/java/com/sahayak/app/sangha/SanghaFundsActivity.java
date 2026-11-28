package com.sahayak.app.sangha;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.sahayak.app.R;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.SanghaContribution;
import com.sahayak.app.models.SanghaExpense;
import com.sahayak.app.sangha.data.SanghaDataRepository;
import com.sahayak.app.sangha.data.SanghaRepositoryInterface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SanghaFundsActivity extends AppCompatActivity {

    private static final String TAG = "SanghaFundsDebug";

    private SanghaRepositoryInterface sanghaRepository;
    private String currentSanghaId = "SNG-001";

    private TextView tvTotalFundsAmount, tvTotalDonationsAmount, tvTotalExpensesAmount, tvAvailableBalanceAmount;
    private TextInputEditText etSearchTransactions;
    private AutoCompleteTextView actvTypeFilter, actvCategoryFilter;
    private LinearLayout layoutTransactionsList;
    private ProgressBar progressBar;

    private String currentTypeFilter = "All Types";
    private String currentCategoryFilter = "All Categories";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_funds);

        sanghaRepository = SanghaDataRepository.getInstance();
        currentSanghaId = getIntent().getStringExtra("SANGHA_ID");
        if (currentSanghaId == null || currentSanghaId.isEmpty()) {
            currentSanghaId = "SNG-001";
        }
        Log.d(TAG, "onCreate: currentSanghaId = " + currentSanghaId);

        initViews();
        setupFilters();
        loadFinancialDataFromFirebase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: reloading financial data for sanghaId = " + currentSanghaId);
        loadFinancialDataFromFirebase();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvTotalFundsAmount = findViewById(R.id.tvTotalFundsAmount);
        tvTotalDonationsAmount = findViewById(R.id.tvTotalDonationsAmount);
        tvTotalExpensesAmount = findViewById(R.id.tvTotalExpensesAmount);
        tvAvailableBalanceAmount = findViewById(R.id.tvAvailableBalanceAmount);

        etSearchTransactions = findViewById(R.id.etSearchTransactions);
        actvTypeFilter = findViewById(R.id.actvTypeFilter);
        actvCategoryFilter = findViewById(R.id.actvCategoryFilter);
        layoutTransactionsList = findViewById(R.id.layoutTransactionsList);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnManageFunds).setOnClickListener(v -> showManageFundsActionDialog());

        etSearchTransactions.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                loadFinancialDataFromFirebase();
            }
        });
    }

    private void setupFilters() {
        String[] types = {"All Types", "Donations", "Expenses"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        actvTypeFilter.setAdapter(typeAdapter);
        actvTypeFilter.setText("All Types", false);
        actvTypeFilter.setOnClickListener(v -> actvTypeFilter.showDropDown());
        actvTypeFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) actvTypeFilter.showDropDown();
        });
        actvTypeFilter.setOnItemClickListener((parent, view, position, id) -> {
            currentTypeFilter = types[position];
            Log.d(TAG, "Type filter selected: " + currentTypeFilter);
            loadFinancialDataFromFirebase();
        });

        String[] categories = {
                "All Categories",
                "Medical Assistance",
                "Education Fees",
                "Groceries and Food",
                "General Welfare",
                "Elderly Care",
                "Other"
        };
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actvCategoryFilter.setAdapter(catAdapter);
        actvCategoryFilter.setText("All Categories", false);
        actvCategoryFilter.setOnClickListener(v -> actvCategoryFilter.showDropDown());
        actvCategoryFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) actvCategoryFilter.showDropDown();
        });
        actvCategoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            currentCategoryFilter = categories[position];
            Log.d(TAG, "Category filter selected: " + currentCategoryFilter);
            loadFinancialDataFromFirebase();
        });
    }

    private void loadFinancialDataFromFirebase() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "loadFinancialDataFromFirebase started for sanghaId: " + currentSanghaId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Fetch Budget
        db.collection("sangha_budgets").document(currentSanghaId).get()
                .addOnSuccessListener(doc -> {
                    double budget = 0.0;
                    if (doc.exists()) {
                        Double b = doc.getDouble("budget");
                        if (b != null) {
                            budget = b;
                        } else {
                            Long l = doc.getLong("budget");
                            if (l != null) {
                                budget = l.doubleValue();
                            } else {
                                Object obj = doc.get("budget");
                                if (obj instanceof Number) {
                                    budget = ((Number) obj).doubleValue();
                                }
                            }
                        }
                    }
                    final double finalBudget = budget;
                    tvTotalFundsAmount.setText("₹" + String.format(Locale.US, "%,.0f", finalBudget));

                    // 2. Fetch Expenses
                    db.collection("sangha_expenses").whereEqualTo("sanghaId", currentSanghaId).get()
                            .addOnSuccessListener(expQuery -> {
                                List<SanghaExpense> expenses = new ArrayList<>();
                                double totalExpenses = 0;
                                for (QueryDocumentSnapshot docExp : expQuery) {
                                    try {
                                        SanghaExpense exp = docExp.toObject(SanghaExpense.class);
                                        if (exp != null) {
                                            expenses.add(exp);
                                            totalExpenses += exp.getAmount();
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing expense doc: " + e.getMessage());
                                    }
                                }
                                tvTotalExpensesAmount.setText("₹" + String.format(Locale.US, "%,.0f", totalExpenses));
                                final double finalExpenses = totalExpenses;

                                // 3. Fetch Donations
                                db.collection("sangha_donations").whereEqualTo("sanghaId", currentSanghaId).get()
                                        .addOnSuccessListener(donQuery -> {
                                            List<Donation> donations = new ArrayList<>();
                                            double totalDonations = 0;
                                            for (QueryDocumentSnapshot docDon : donQuery) {
                                                try {
                                                    Donation d = docDon.toObject(Donation.class);
                                                    if (d != null) {
                                                        donations.add(d);
                                                        totalDonations += d.getAmount();
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error parsing donation doc: " + e.getMessage());
                                                }
                                            }
                                            if (donations.isEmpty()) {
                                                donations = sanghaRepository.getReceivedDonations(currentSanghaId);
                                                for (Donation d : donations) totalDonations += d.getAmount();
                                            }
                                            tvTotalDonationsAmount.setText("₹" + String.format(Locale.US, "%,.0f", totalDonations));
                                            final double finalDonations = totalDonations;
                                            final List<Donation> finalDonationsList = donations;

                                            // 4. Fetch Contributions (still kept for net balance calculation if any exist)
                                            db.collection("sangha_contributions").whereEqualTo("sanghaId", currentSanghaId).get()
                                                    .addOnSuccessListener(conQuery -> {
                                                        List<SanghaContribution> contributions = new ArrayList<>();
                                                        double netContributions = 0;
                                                        for (QueryDocumentSnapshot docCon : conQuery) {
                                                            try {
                                                                SanghaContribution c = docCon.toObject(SanghaContribution.class);
                                                                if (c != null) {
                                                                    contributions.add(c);
                                                                    if (SanghaContribution.TYPE_RECEIVED.equalsIgnoreCase(c.getDirection())) {
                                                                        netContributions += c.getAmount();
                                                                    } else if (SanghaContribution.TYPE_GIVEN.equalsIgnoreCase(c.getDirection())) {
                                                                        netContributions -= c.getAmount();
                                                                    }
                                                                }
                                                            } catch (Exception e) {
                                                                Log.e(TAG, "Error parsing contribution doc: " + e.getMessage());
                                                            }
                                                        }

                                                        if (progressBar != null) progressBar.setVisibility(View.GONE);

                                                        double balance = (finalBudget + finalDonations + netContributions) - finalExpenses;
                                                        tvAvailableBalanceAmount.setText("₹" + String.format(Locale.US, "%,.0f", balance));

                                                        renderTransactions(finalDonationsList, expenses, contributions);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                                                        renderTransactions(finalDonationsList, expenses, new ArrayList<>());
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                                            loadFinancialDataFromRepositoryFallback();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                loadFinancialDataFromRepositoryFallback();
                            });
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Firebase fetch error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadFinancialDataFromRepositoryFallback();
                });
    }

    private void loadFinancialDataFromRepositoryFallback() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        Log.d(TAG, "Falling back to repository data for sanghaId: " + currentSanghaId);

        double totalFunds = sanghaRepository.getAnnualBudget(currentSanghaId);
        List<Donation> donations = sanghaRepository.getReceivedDonations(currentSanghaId);
        List<SanghaExpense> expenses = sanghaRepository.getSanghaExpenses(currentSanghaId);
        List<SanghaContribution> contributions = sanghaRepository.getSanghaContributions(currentSanghaId);

        double totalDonations = 0;
        for (Donation d : donations) totalDonations += d.getAmount();

        double totalExpenses = 0;
        for (SanghaExpense exp : expenses) totalExpenses += exp.getAmount();

        double netContributions = 0;
        for (SanghaContribution c : contributions) {
            if (SanghaContribution.TYPE_RECEIVED.equalsIgnoreCase(c.getDirection())) {
                netContributions += c.getAmount();
            } else if (SanghaContribution.TYPE_GIVEN.equalsIgnoreCase(c.getDirection())) {
                netContributions -= c.getAmount();
            }
        }

        double availableBalance = (totalFunds + totalDonations + netContributions) - totalExpenses;

        tvTotalFundsAmount.setText("₹" + String.format(Locale.US, "%,.0f", totalFunds));
        tvTotalDonationsAmount.setText("₹" + String.format(Locale.US, "%,.0f", totalDonations));
        tvTotalExpensesAmount.setText("₹" + String.format(Locale.US, "%,.0f", totalExpenses));
        tvAvailableBalanceAmount.setText("₹" + String.format(Locale.US, "%,.0f", availableBalance));

        renderTransactions(donations, expenses, contributions);
    }

    private void renderTransactions(List<Donation> donations, List<SanghaExpense> expenses, List<SanghaContribution> contributions) {
        layoutTransactionsList.removeAllViews();

        List<TransactionItem> items = new ArrayList<>();

        // Add donations if type filter matches
        if ("All Types".equalsIgnoreCase(currentTypeFilter) || "Donations".equalsIgnoreCase(currentTypeFilter)) {
            for (Donation d : donations) {
                String cat = d.getContributionType() != null ? d.getContributionType() : "General Welfare Fund";
                if (!matchesCategory(cat, d.getPurpose())) continue;
                if (!matchesSearch(d.getDonorName(), d.getPurpose(), d.getTransactionRef(), null)) continue;
                items.add(new TransactionItem(
                        "Donation Received",
                        "+₹" + String.format(Locale.US, "%,.0f", d.getAmount()),
                        "Donor: " + (d.getDonorName() != null ? d.getDonorName() : "Anonymous"),
                        "Purpose: " + (d.getPurpose() != null ? d.getPurpose() : ""),
                        cat,
                        d.getTransactionDate(),
                        d.getTransactionRef() != null ? d.getTransactionRef() : "N/A",
                        "#2E7D32",
                        d
                ));
            }
        }

        // Add expenses if type filter matches
        if ("All Types".equalsIgnoreCase(currentTypeFilter) || "Expenses".equalsIgnoreCase(currentTypeFilter)) {
            for (SanghaExpense exp : expenses) {
                if (!matchesCategory(exp.getServiceCategory(), exp.getPurpose())) continue;
                if (!matchesSearch(null, exp.getPurpose(), exp.getTransactionRef(), exp.getRequestId())) continue;
                items.add(new TransactionItem(
                        "Expense Incurred",
                        "-₹" + String.format(Locale.US, "%,.0f", exp.getAmount()),
                        "Purpose: " + (exp.getPurpose() != null ? exp.getPurpose() : ""),
                        exp.getRequestId() != null && !exp.getRequestId().isEmpty() ? "Request ID: " + exp.getRequestId() : "Operational Expense",
                        exp.getServiceCategory() != null ? exp.getServiceCategory() : "General",
                        exp.getExpenseDate(),
                        exp.getTransactionRef() != null ? exp.getTransactionRef() : "N/A",
                        "#C62828",
                        exp
                ));
            }
        }

        // Sort by timestamp descending
        items.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        if (items.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No transactions found matching the selected filters.");
            tvEmpty.setPadding(16, 24, 16, 24);
            tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_secondary));
            layoutTransactionsList.addView(tvEmpty);
            return;
        }

        for (TransactionItem item : items) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 12;
            card.setLayoutParams(lp);
            card.setRadius(14f);
            card.setCardElevation(2f);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(20, 16, 20, 16);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvType = new TextView(this);
            tvType.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvType.setText(item.title);
            tvType.setTextSize(13f);
            tvType.setTypeface(null, android.graphics.Typeface.BOLD);
            tvType.setTextColor(android.graphics.Color.parseColor(item.colorHex));

            TextView tvAmt = new TextView(this);
            tvAmt.setText(item.amountStr);
            tvAmt.setTextSize(14f);
            tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAmt.setTextColor(android.graphics.Color.parseColor(item.colorHex));

            topRow.addView(tvType);
            topRow.addView(tvAmt);
            inner.addView(topRow);

            TextView tvDetails = new TextView(this);
            tvDetails.setText(item.detail1 + "\n" + item.detail2 + "\nCategory: " + item.category + "\nDate: " + sdf.format(new Date(item.timestamp)) + " | Ref: " + item.ref);
            tvDetails.setTextSize(12f);
            tvDetails.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_secondary));
            tvDetails.setPadding(0, 4, 0, 0);
            inner.addView(tvDetails);

            card.setOnClickListener(v -> showTransactionDetailsDialog(item));

            card.addView(inner);
            layoutTransactionsList.addView(card);
        }
    }

    private void showTransactionDetailsDialog(TransactionItem item) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String details = "Type: " + item.title + "\n" +
                "Amount: " + item.amountStr + "\n" +
                item.detail1 + "\n" +
                item.detail2 + "\n" +
                "Category: " + item.category + "\n" +
                "Date: " + sdf.format(new Date(item.timestamp)) + "\n" +
                "Transaction Reference: " + item.ref;

        new AlertDialog.Builder(this)
                .setTitle("Transaction Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private boolean matchesCategory(String cat, String purpose) {
        if ("All Categories".equalsIgnoreCase(currentCategoryFilter)) return true;
        String filter = currentCategoryFilter.toLowerCase(Locale.ROOT);
        String c = cat != null ? cat.toLowerCase(Locale.ROOT) : "";
        String p = purpose != null ? purpose.toLowerCase(Locale.ROOT) : "";

        if (filter.contains("medical")) {
            return c.contains("medical") || p.contains("medical") || p.contains("cataract") || p.contains("surgery") || p.contains("medicine");
        }
        if (filter.contains("groceries") || filter.contains("food")) {
            return c.contains("grocer") || c.contains("food") || p.contains("grocer") || p.contains("food");
        }
        if (filter.contains("education") || filter.contains("fee")) {
            return c.contains("educat") || c.contains("school") || c.contains("fee") || p.contains("educat") || p.contains("school") || p.contains("fee");
        }
        if (filter.contains("welfare")) {
            return c.contains("welfare") || c.contains("general") || p.contains("welfare") || p.contains("general");
        }
        if (filter.contains("elderly")) {
            return c.contains("elder") || c.contains("support") || p.contains("elder") || p.contains("support");
        }
        if (filter.contains("other")) {
            return !c.contains("medical") && !c.contains("grocer") && !c.contains("food") && !c.contains("educat") && !c.contains("fee") && !c.contains("elder") && !c.contains("welfare") && !c.contains("general");
        }

        return c.contains(filter) || p.contains(filter);
    }

    private boolean matchesSearch(String s1, String s2, String s3, String s4) {
        if (currentSearchQuery.isEmpty()) return true;
        String combined = ((s1 != null ? s1 : "") + " " + (s2 != null ? s2 : "") + " " + (s3 != null ? s3 : "") + " " + (s4 != null ? s4 : "")).toLowerCase(Locale.ROOT);
        return combined.contains(currentSearchQuery);
    }

    private void showManageFundsActionDialog() {
        String[] actions = new String[]{
                "1. Add New Donation",
                "2. Record New Expense",
                "3. Update Total Annual Budget"
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
        String[] cats = {"Medical Assistance", "Education Fees", "Groceries and Food", "General Welfare", "Elderly Care", "Other"};
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
                        "Sangha Organization",
                        cat,
                        null,
                        amt,
                        purpose,
                        ref,
                        "Online Transfer"
                );
                sanghaRepository.addDonation(d);
                loadFinancialDataFromFirebase();
                Toast.makeText(this, "Donation recorded & synced to Firebase!", Toast.LENGTH_SHORT).show();
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
        String[] cats = {"Medical Assistance", "Education Fees", "Groceries and Food", "General Welfare", "Elderly Care", "Other"};
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
                loadFinancialDataFromFirebase();
                Toast.makeText(this, "Expense recorded & synced to Firebase!", Toast.LENGTH_SHORT).show();
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
                loadFinancialDataFromFirebase();
                Toast.makeText(this, "Annual budget updated to ₹" + newBudget, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private static class TransactionItem {
        String title;
        String amountStr;
        String detail1;
        String detail2;
        String category;
        long timestamp;
        String ref;
        String colorHex;
        Object rawObject;

        public TransactionItem(String title, String amountStr, String detail1, String detail2, String category, long timestamp, String ref, String colorHex, Object rawObject) {
            this.title = title;
            this.amountStr = amountStr;
            this.detail1 = detail1;
            this.detail2 = detail2;
            this.category = category;
            this.timestamp = timestamp;
            this.ref = ref;
            this.colorHex = colorHex;
            this.rawObject = rawObject;
        }
    }
}
