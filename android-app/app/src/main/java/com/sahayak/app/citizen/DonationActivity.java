package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.DonationRepository;
import com.sahayak.app.data.NotificationRepository;
import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.User;

import java.util.UUID;

public class DonationActivity extends AppCompatActivity {

    private String sanghaId, contributionType, requestId;
    private Sangha sangha;
    private User currentUser;

    private TextView tvTargetSangha, tvTargetType;
    private TextInputLayout tilDonorName, tilAmount, tilPurpose;
    private TextInputEditText etDonorName, etAmount, etPurpose;
    private RadioGroup rgPaymentMethod;

    private SessionManager sessionManager;
    private DonationRepository donationRepository;
    private NotificationRepository notificationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        sanghaId = getIntent().getStringExtra("SANGHA_ID");
        if (sanghaId == null) sanghaId = "SNG-001";

        contributionType = getIntent().getStringExtra("CONTRIBUTION_TYPE");
        if (contributionType == null) contributionType = "GENERAL_DONATION";

        requestId = getIntent().getStringExtra("REQUEST_ID");

        sessionManager = new SessionManager(this);
        donationRepository = DonationRepository.getInstance();
        notificationRepository = NotificationRepository.getInstance();

        currentUser = sessionManager.getCurrentUser();
        sangha = SanghaRepository.getInstance().getSanghaById(sanghaId);

        initViews();
        setupQuickAmounts();
    }

    private void initViews() {
        tvTargetSangha = findViewById(R.id.tvTargetSangha);
        tvTargetType = findViewById(R.id.tvTargetType);

        tilDonorName = findViewById(R.id.tilDonorName);
        tilAmount = findViewById(R.id.tilAmount);
        tilPurpose = findViewById(R.id.tilPurpose);

        etDonorName = findViewById(R.id.etDonorName);
        etAmount = findViewById(R.id.etAmount);
        etPurpose = findViewById(R.id.etPurpose);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);

        if (sangha != null) {
            tvTargetSangha.setText("Recipient: " + sangha.getName());
        }

        if ("REQUEST_SPECIFIC".equals(contributionType)) {
            tvTargetType.setText("Type: Request Sponsorship (" + (requestId != null ? requestId : "REQ-2026-0002") + ")");
            etAmount.setText("2500");
        } else {
            tvTargetType.setText("Type: General Welfare Fund");
            etAmount.setText("1000");
        }

        if (currentUser != null && currentUser.getFullName() != null) {
            etDonorName.setText(currentUser.getFullName());
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnProceedPayment).setOnClickListener(v -> validateAndPay());
    }

    private void setupQuickAmounts() {
        findViewById(R.id.btnAmount500).setOnClickListener(v -> etAmount.setText("500"));
        findViewById(R.id.btnAmount1000).setOnClickListener(v -> etAmount.setText("1000"));
        findViewById(R.id.btnAmount2500).setOnClickListener(v -> etAmount.setText("2500"));
        findViewById(R.id.btnAmount5000).setOnClickListener(v -> etAmount.setText("5000"));
    }

    private void validateAndPay() {
        tilDonorName.setError(null);
        tilAmount.setError(null);

        String donorName = etDonorName.getText() != null ? etDonorName.getText().toString().trim() : "";
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String purpose = etPurpose.getText() != null ? etPurpose.getText().toString().trim() : "";

        boolean isValid = true;

        if (donorName.isEmpty()) {
            tilDonorName.setError("Donor name is required");
            isValid = false;
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                tilAmount.setError("Enter an amount greater than ₹0");
                isValid = false;
            }
        } catch (Exception e) {
            tilAmount.setError("Enter a valid numeric amount");
            isValid = false;
        }

        if (!isValid) return;

        int selectedPaymentId = rgPaymentMethod.getCheckedRadioButtonId();
        String paymentMethod = "UPI (Google Pay)";
        if (selectedPaymentId == R.id.rbNetBanking) {
            paymentMethod = "Net Banking (SBI)";
        } else if (selectedPaymentId == R.id.rbCard) {
            paymentMethod = "Debit/Credit Card";
        }

        processPaymentGateway(donorName, amount, purpose, paymentMethod);
    }

    private void processPaymentGateway(String donorName, double amount, String purpose, String paymentMethod) {
        // Show Processing Dialog
        View processingView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_processing, null);
        AlertDialog processingDialog = new AlertDialog.Builder(this)
                .setView(processingView)
                .setCancelable(false)
                .create();

        processingDialog.show();

        // Simulate Gateway verification delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            processingDialog.dismiss();

            String txnRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String cId = currentUser != null ? currentUser.getUserId() : "usr-cit-001";
            String sName = sangha != null ? sangha.getName() : "Seva Bharathi Trust";

            Donation donation = new Donation(
                    null,
                    cId,
                    donorName,
                    sanghaId,
                    sName,
                    contributionType,
                    requestId,
                    amount,
                    purpose.isEmpty() ? "Community Welfare Contribution" : purpose,
                    txnRef,
                    paymentMethod
            );

            donationRepository.recordDonation(donation);

            if (requestId != null) {
                MockRequestRepository.addRaisedAmount(requestId, amount);
            }

            // Notification
            notificationRepository.addNotification(
                    cId,
                    "Donation Confirmed: ₹" + (long) amount,
                    "Thank you " + donorName + "! Your contribution of ₹" + (long) amount + " to " + sName + " was received successfully.",
                    "DONATION"
            );

            showReceiptDialog(donation);
        }, 1500);
    }

    private void showReceiptDialog(Donation donation) {
        View receiptView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_receipt, null);
        AlertDialog receiptDialog = new AlertDialog.Builder(this)
                .setView(receiptView)
                .setCancelable(false)
                .create();

        TextView tvReceiptDonationId = receiptView.findViewById(R.id.tvReceiptDonationId);
        TextView tvReceiptAmount = receiptView.findViewById(R.id.tvReceiptAmount);
        TextView tvReceiptSanghaName = receiptView.findViewById(R.id.tvReceiptSanghaName);
        TextView tvReceiptTypePurpose = receiptView.findViewById(R.id.tvReceiptTypePurpose);
        TextView tvReceiptTxnRef = receiptView.findViewById(R.id.tvReceiptTxnRef);

        tvReceiptDonationId.setText(donation.getDonationId());
        tvReceiptAmount.setText("₹" + String.format("%,.0f", donation.getAmount()));
        tvReceiptSanghaName.setText("To: " + donation.getSanghaName());
        tvReceiptTypePurpose.setText("Type: " + ("REQUEST_SPECIFIC".equals(donation.getContributionType()) ? "Request Sponsorship" : "General Welfare") + "\nNote: " + donation.getPurpose());
        tvReceiptTxnRef.setText("Txn Ref: " + donation.getTransactionRef() + " • " + donation.getPaymentMethod());

        receiptView.findViewById(R.id.btnDoneReceipt).setOnClickListener(v -> {
            receiptDialog.dismiss();
            Intent intent = new Intent(DonationActivity.this, DonationHistoryActivity.class);
            startActivity(intent);
            finish();
        });

        receiptDialog.show();
    }
}