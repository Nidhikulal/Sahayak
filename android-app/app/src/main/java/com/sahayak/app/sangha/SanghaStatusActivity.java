package com.sahayak.app.sangha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.FirebaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lets a Sangha look up their application status by Application ID,
 * respond & attach extra documents when INFO_REQUESTED,
 * or jump to account creation once APPROVED.
 */
public class SanghaStatusActivity extends AppCompatActivity {

    public static final String EXTRA_APP_ID = "extra_application_id";

    private TextInputLayout tilSearchAppId, tilResponseNote;
    private TextInputEditText etSearchAppId, etResponseNote;
    private ProgressBar progressBar;
    private LinearLayout layoutResult, layoutResubmitSection;
    private TextView tvStatusBadge, tvOrgTitle, tvAppMeta, tvRemarksBox, tvExtraDocsSummary;
    private MaterialButton btnProceedToRegister, btnResubmit;

    private FirebaseManager firebaseManager;
    private final List<Map<String, Object>> extraDocs = new ArrayList<>();
    private String loadedAppId = "";
    private String loadedToken = "";

    private final ActivityResultLauncher<String[]> extraDocPicker =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) return;
                for (Uri uri : uris) {
                    if (extraDocs.size() >= 3) break;
                    try {
                        extraDocs.add(DocumentAttachmentHelper.readUriAsDocumentMap(this, uri));
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                tvExtraDocsSummary.setText(extraDocs.size() + " additional file(s) ready to upload");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_status);

        firebaseManager = FirebaseManager.getInstance();
        initViews();

        String prefillId = getIntent().getStringExtra(EXTRA_APP_ID);
        if (prefillId == null || prefillId.isEmpty()) {
            prefillId = getSharedPreferences(SanghaApplicationActivity.PREFS_APP, MODE_PRIVATE)
                    .getString(SanghaApplicationActivity.KEY_LAST_APP_ID, "");
        }
        if (prefillId != null && !prefillId.isEmpty()) {
            etSearchAppId.setText(prefillId);
            lookupStatus(prefillId);
        }
    }

    private void initViews() {
        tilSearchAppId = findViewById(R.id.tilSearchAppId);
        tilResponseNote = findViewById(R.id.tilResponseNote);
        etSearchAppId = findViewById(R.id.etSearchAppId);
        etResponseNote = findViewById(R.id.etResponseNote);

        progressBar = findViewById(R.id.progressBar);
        layoutResult = findViewById(R.id.layoutResult);
        layoutResubmitSection = findViewById(R.id.layoutResubmitSection);

        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvOrgTitle = findViewById(R.id.tvOrgTitle);
        tvAppMeta = findViewById(R.id.tvAppMeta);
        tvRemarksBox = findViewById(R.id.tvRemarksBox);
        tvExtraDocsSummary = findViewById(R.id.tvExtraDocsSummary);

        btnProceedToRegister = findViewById(R.id.btnProceedToRegister);
        btnResubmit = findViewById(R.id.btnResubmit);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCheckStatus).setOnClickListener(v -> {
            String appId = etSearchAppId.getText() != null ? etSearchAppId.getText().toString().trim() : "";
            if (appId.isEmpty()) {
                tilSearchAppId.setError("Enter Application ID");
                return;
            }
            tilSearchAppId.setError(null);
            lookupStatus(appId);
        });

        findViewById(R.id.btnAttachExtraDocs).setOnClickListener(v ->
                extraDocPicker.launch(new String[]{"application/pdf", "image/*"}));

        btnResubmit.setOnClickListener(v -> resubmit());

        btnProceedToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, SanghaRegistrationActivity.class);
            intent.putExtra(SanghaRegistrationActivity.EXTRA_APP_ID, loadedAppId);
            intent.putExtra(SanghaRegistrationActivity.EXTRA_REG_TOKEN, loadedToken);
            startActivity(intent);
        });
    }

    private void lookupStatus(String appId) {
        progressBar.setVisibility(View.VISIBLE);
        layoutResult.setVisibility(View.GONE);

        firebaseManager.fetchSanghaApplication(appId, new FirebaseManager.ApplicationCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                progressBar.setVisibility(View.GONE);
                renderApplication(data);
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SanghaStatusActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderApplication(Map<String, Object> data) {
        layoutResult.setVisibility(View.VISIBLE);
        loadedAppId = str(data, "applicationId");
        loadedToken = str(data, "registrationToken");

        String status = str(data, "status");
        String orgName = str(data, "organizationName");
        String regNo = str(data, "registrationNo");
        String email = str(data, "email");
        String remarks = str(data, "adminRemarks");
        String expiresStr = str(data, "registrationTokenExpiresAtStr");
        int docCount = data.get("documentCount") instanceof Number ? ((Number) data.get("documentCount")).intValue() : 0;

        tvOrgTitle.setText(orgName);
        tvAppMeta.setText("Application ID: " + loadedAppId
                + "\nReg No: " + regNo
                + "\nEmail: " + email
                + "\nSupporting Docs: " + docCount);

        tvStatusBadge.setText(status);
        layoutResubmitSection.setVisibility(View.GONE);
        btnProceedToRegister.setVisibility(View.GONE);
        tvRemarksBox.setVisibility(View.GONE);

        if (FirebaseManager.STATUS_APPROVED.equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.status_completed));
            tvRemarksBox.setVisibility(View.VISIBLE);
            tvRemarksBox.setText("Approved! Your single-use registration link/token is active"
                    + (!expiresStr.isEmpty() ? " until " + expiresStr : "")
                    + ". Check your email (" + email + ") or tap below to complete registration.");
            btnProceedToRegister.setVisibility(View.VISIBLE);
        } else if (FirebaseManager.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.sahayak_primary));
            tvRemarksBox.setVisibility(View.VISIBLE);
            tvRemarksBox.setText("Registration completed! Please Sign In with your username and password.");
        } else if (FirebaseManager.STATUS_REJECTED.equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected));
            tvRemarksBox.setVisibility(View.VISIBLE);
            tvRemarksBox.setText("Rejection Reason:\n" + (!remarks.isEmpty() ? remarks : "Application was not approved."));
        } else if (FirebaseManager.STATUS_INFO_REQUESTED.equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.status_in_progress));
            tvRemarksBox.setVisibility(View.VISIBLE);
            tvRemarksBox.setText("Admin Requested More Info:\n" + (!remarks.isEmpty() ? remarks : "Please upload additional supporting details."));
            layoutResubmitSection.setVisibility(View.VISIBLE);
        } else {
            tvStatusBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.status_verification));
            tvRemarksBox.setVisibility(View.VISIBLE);
            tvRemarksBox.setText("Your application is under review by Sahayak Admin (" + FirebaseManager.ADMIN_EMAIL + ").");
        }
    }

    private void resubmit() {
        String note = etResponseNote.getText() != null ? etResponseNote.getText().toString().trim() : "";
        if (note.isEmpty() && extraDocs.isEmpty()) {
            tilResponseNote.setError("Provide a response message or attach additional documents");
            return;
        }
        tilResponseNote.setError(null);
        progressBar.setVisibility(View.VISIBLE);
        btnResubmit.setEnabled(false);

        firebaseManager.resubmitSanghaApplication(
                this, loadedAppId, note, new ArrayList<>(extraDocs),
                new FirebaseManager.ApplicationSubmitCallback() {
                    @Override
                    public void onSuccess(String applicationId, String reviewToken, boolean emailSent, String emailMessage) {
                        progressBar.setVisibility(View.GONE);
                        btnResubmit.setEnabled(true);
                        extraDocs.clear();
                        tvExtraDocsSummary.setText("");
                        Toast.makeText(SanghaStatusActivity.this,
                                emailSent ? "Resubmitted & Admin notified via email!" : "Resubmitted! (" + emailMessage + ")",
                                Toast.LENGTH_LONG).show();
                        lookupStatus(loadedAppId);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        btnResubmit.setEnabled(true);
                        Toast.makeText(SanghaStatusActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private String str(Map<String, Object> d, String key) {
        if (d == null) return "";
        Object val = d.get(key);
        return val instanceof String ? (String) val : "";
    }
}
