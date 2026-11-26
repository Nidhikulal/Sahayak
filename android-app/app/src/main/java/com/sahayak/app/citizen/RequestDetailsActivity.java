package com.sahayak.app.citizen;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.R;

public class RequestDetailsActivity extends AppCompatActivity {

    private String requestId;
    private AssistanceRequest request;

    private TextView tvTitleRequestId, tvStatusText, tvAssignedSanghaBadge, tvBeneficiaryName, tvCategory, tvContact, tvAddress, tvDescription, tvEvidenceDocs;
    private View cardRejectionBanner;
    private TextView tvRejectionReason;
    private TextView tvStepSubmitted, tvStepAssigned, tvStepVerification, tvStepCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_details);

        requestId = getIntent().getStringExtra("REQUEST_ID");
        if (requestId == null) requestId = "REQ-2026-0001";

        initViews();
        loadData();
    }

    private void initViews() {
        tvTitleRequestId = findViewById(R.id.tvTitleRequestId);
        tvStatusText = findViewById(R.id.tvStatusText);
        tvAssignedSanghaBadge = findViewById(R.id.tvAssignedSanghaBadge);
        tvBeneficiaryName = findViewById(R.id.tvBeneficiaryName);
        tvCategory = findViewById(R.id.tvCategory);
        tvContact = findViewById(R.id.tvContact);
        tvAddress = findViewById(R.id.tvAddress);
        tvDescription = findViewById(R.id.tvDescription);
        tvEvidenceDocs = findViewById(R.id.tvEvidenceDocs);

        cardRejectionBanner = findViewById(R.id.cardRejectionBanner);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);

        tvStepSubmitted = findViewById(R.id.tvStepSubmitted);
        tvStepAssigned = findViewById(R.id.tvStepAssigned);
        tvStepVerification = findViewById(R.id.tvStepVerification);
        tvStepCompleted = findViewById(R.id.tvStepCompleted);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Simulation Listeners
        findViewById(R.id.btnSimulateVerify).setOnClickListener(v -> {
            MockRequestRepository.updateStatus(requestId, RequestStatus.VERIFIED, null);
            Toast.makeText(this, "Request verified on ground!", Toast.LENGTH_SHORT).show();
            loadData();
        });

        findViewById(R.id.btnSimulateComplete).setOnClickListener(v -> {
            MockRequestRepository.updateStatus(requestId, RequestStatus.COMPLETED, null);
            Toast.makeText(this, "Assistance delivered and request completed!", Toast.LENGTH_SHORT).show();
            loadData();
        });

        findViewById(R.id.btnSimulateReject).setOnClickListener(v -> {
            MockRequestRepository.updateStatus(requestId, RequestStatus.REJECTED, "Beneficiary address verification unconfirmed by Sangha field visit.");
            Toast.makeText(this, "Request rejected.", Toast.LENGTH_SHORT).show();
            loadData();
        });
    }

    private void loadData() {
        request = MockRequestRepository.getById(requestId);
        if (request == null) return;

        tvTitleRequestId.setText("Request " + request.requestId);
        tvBeneficiaryName.setText(request.beneficiaryName + " (" + request.age + " yrs, " + request.gender + ")");
        tvCategory.setText("Category: " + request.assistanceType);
        tvContact.setText("Contact: " + (request.contactNumber != null && !request.contactNumber.isEmpty() ? request.contactNumber : "No phone provided"));
        tvAddress.setText("Address: " + request.getFormattedAddress());
        tvDescription.setText(request.description);

        if (request.evidenceDocuments != null && !request.evidenceDocuments.isEmpty()) {
            StringBuilder sb = new StringBuilder("Attached Documents: ");
            for (int i = 0; i < request.evidenceDocuments.size(); i++) {
                sb.append(request.evidenceDocuments.get(i));
                if (i < request.evidenceDocuments.size() - 1) sb.append(", ");
            }
            tvEvidenceDocs.setText(sb.toString());
        } else {
            tvEvidenceDocs.setText("Attached Documents: None provided");
        }

        if (request.assignedSanghaName != null && !request.assignedSanghaName.isEmpty()) {
            tvAssignedSanghaBadge.setText(request.assignedSanghaName);
        } else {
            tvAssignedSanghaBadge.setText("Sangha Unassigned");
        }

        tvStatusText.setText(RequestStatus.friendlyLabel(request.status));

        // Rejection Check
        if (RequestStatus.REJECTED.equals(request.status)) {
            cardRejectionBanner.setVisibility(View.VISIBLE);
            tvRejectionReason.setText("Reason: " + (request.rejectionReason != null ? request.rejectionReason : "Requirements unverified on site inspection."));
        } else {
            cardRejectionBanner.setVisibility(View.GONE);
        }

        updateTimelineUI(request.status);
    }

    private void updateTimelineUI(String status) {
        // Reset timeline styles
        tvStepSubmitted.setText("✓ 1. Request Submitted");
        tvStepAssigned.setText("✓ 2. Forwarded & Assigned to Sangha");

        if (RequestStatus.SUBMITTED.equals(status)) {
            tvStepAssigned.setText("⏳ 2. Awaiting Sangha Assignment");
            tvStepVerification.setText("○ 3. On-Ground Verification");
            tvStepCompleted.setText("○ 4. Relief Aid Delivery & Completion");
        } else if (RequestStatus.ASSIGNED.equals(status) || RequestStatus.UNDER_VERIFICATION.equals(status)) {
            tvStepVerification.setText("⏳ 3. On-Ground Verification in Progress");
            tvStepCompleted.setText("○ 4. Relief Aid Delivery & Completion");
        } else if (RequestStatus.VERIFIED.equals(status) || RequestStatus.IN_PROGRESS.equals(status)) {
            tvStepVerification.setText("✓ 3. On-Ground Request Verified");
            tvStepCompleted.setText("⏳ 4. Relief Aid Delivery in Progress");
        } else if (RequestStatus.COMPLETED.equals(status)) {
            tvStepVerification.setText("✓ 3. On-Ground Request Verified");
            tvStepCompleted.setText("✓ 4. Relief Aid Delivered & Completed");
        } else if (RequestStatus.REJECTED.equals(status)) {
            tvStepVerification.setText("❌ 3. Request Rejected on Verification");
            tvStepCompleted.setText("○ 4. Closed");
        }
    }
}
