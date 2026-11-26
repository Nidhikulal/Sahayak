package com.sahayak.app.sangha;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
 * Sangha Verification Application Form.
 * Collects all Sangha organization details, login credentials (username/password),
 * and supporting documents, then emails everything to the admin for review.
 * The admin clicks Accept or Reject directly in the email — no manual Android steps needed.
 */
public class SanghaApplicationActivity extends AppCompatActivity {

    public static final String PREFS_APP = "sahayak_sangha_app_prefs";
    public static final String KEY_LAST_APP_ID = "last_application_id";
    private static final int MAX_DOCS = 4;

    private TextInputLayout tilOrgName, tilRegNo, tilContactPerson, tilEmail, tilMobile,
            tilDistrict, tilLocality, tilAddress, tilDescription,
            tilUsername, tilPassword, tilConfirmPassword;
    private TextInputEditText etOrgName, etRegNo, etContactPerson, etEmail, etMobile,
            etDistrict, etLocality, etAddress, etDescription,
            etUsername, etPassword, etConfirmPassword;

    private LinearLayout layoutAttachedDocs;
    private ProgressBar progressBar;
    private MaterialButton btnSubmitApplication;

    private final List<Map<String, Object>> attachedDocuments = new ArrayList<>();
    private FirebaseManager firebaseManager;

    private final ActivityResultLauncher<String[]> documentPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) return;
                for (Uri uri : uris) {
                    if (attachedDocuments.size() >= MAX_DOCS) {
                        Toast.makeText(this, "Maximum " + MAX_DOCS + " documents allowed.", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    try {
                        Map<String, Object> docMap = DocumentAttachmentHelper.readUriAsDocumentMap(this, uri);
                        attachedDocuments.add(docMap);
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Could not attach file", Toast.LENGTH_LONG).show();
                    }
                }
                renderAttachedDocs();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_application);

        firebaseManager = FirebaseManager.getInstance();
        initViews();
    }

    private void initViews() {
        tilOrgName = findViewById(R.id.tilOrgName);
        tilRegNo = findViewById(R.id.tilRegNo);
        tilContactPerson = findViewById(R.id.tilContactPerson);
        tilEmail = findViewById(R.id.tilEmail);
        tilMobile = findViewById(R.id.tilMobile);
        tilDistrict = findViewById(R.id.tilDistrict);
        tilLocality = findViewById(R.id.tilLocality);
        tilAddress = findViewById(R.id.tilAddress);
        tilDescription = findViewById(R.id.tilDescription);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etOrgName = findViewById(R.id.etOrgName);
        etRegNo = findViewById(R.id.etRegNo);
        etContactPerson = findViewById(R.id.etContactPerson);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etDistrict = findViewById(R.id.etDistrict);
        etLocality = findViewById(R.id.etLocality);
        etAddress = findViewById(R.id.etAddress);
        etDescription = findViewById(R.id.etDescription);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        layoutAttachedDocs = findViewById(R.id.layoutAttachedDocs);
        progressBar = findViewById(R.id.progressBar);
        btnSubmitApplication = findViewById(R.id.btnSubmitApplication);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnUploadDocuments).setOnClickListener(v ->
                documentPickerLauncher.launch(new String[]{"application/pdf", "image/*"}));
        btnSubmitApplication.setOnClickListener(v -> attemptSubmit());
    }

    private void renderAttachedDocs() {
        layoutAttachedDocs.removeAllViews();
        for (int i = 0; i < attachedDocuments.size(); i++) {
            final int index = i;
            Map<String, Object> doc = attachedDocuments.get(i);
            String name = String.valueOf(doc.get("fileName"));
            long size = doc.get("sizeBytes") instanceof Number ? ((Number) doc.get("sizeBytes")).longValue() : 0L;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(24, 16, 16, 16);
            row.setBackgroundColor(ContextCompat.getColor(this, R.color.sahayak_surface_variant));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 12;
            row.setLayoutParams(lp);

            TextView tvInfo = new TextView(this);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvInfo.setText(name + " (" + DocumentAttachmentHelper.formatSize(size) + ")");
            tvInfo.setTextSize(12f);
            tvInfo.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_primary));

            TextView btnRemove = new TextView(this);
            btnRemove.setText("Remove");
            btnRemove.setTextSize(12f);
            btnRemove.setPadding(16, 8, 16, 8);
            btnRemove.setTextColor(ContextCompat.getColor(this, R.color.status_rejected));
            btnRemove.setOnClickListener(v -> {
                attachedDocuments.remove(index);
                renderAttachedDocs();
            });

            row.addView(tvInfo);
            row.addView(btnRemove);
            layoutAttachedDocs.addView(row);
        }
    }

    private void attemptSubmit() {
        clearErrors();

        String orgName = textOf(etOrgName);
        String regNo = textOf(etRegNo);
        String contactPerson = textOf(etContactPerson);
        String email = textOf(etEmail);
        String mobile = textOf(etMobile);
        String district = textOf(etDistrict);
        String locality = textOf(etLocality);
        String address = textOf(etAddress);
        String description = textOf(etDescription);
        String username = textOf(etUsername);
        String password = textOf(etPassword);
        String confirmPassword = textOf(etConfirmPassword);

        boolean valid = true;

        if (orgName.isEmpty()) { tilOrgName.setError("Organization name is required"); valid = false; }
        if (regNo.isEmpty()) { tilRegNo.setError("Registration number is required"); valid = false; }
        if (contactPerson.isEmpty()) { tilContactPerson.setError("Contact person is required"); valid = false; }
        if (email.isEmpty()) {
            tilEmail.setError("Official email is required"); valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address"); valid = false;
        }
        if (mobile.isEmpty()) {
            tilMobile.setError("Mobile number is required"); valid = false;
        } else if (!mobile.matches("^[0-9]{10}$")) {
            tilMobile.setError("Enter a valid 10-digit mobile number"); valid = false;
        }
        if (district.isEmpty()) { tilDistrict.setError("District is required"); valid = false; }
        if (locality.isEmpty()) { tilLocality.setError("Locality is required"); valid = false; }
        if (address.isEmpty()) { tilAddress.setError("Address is required"); valid = false; }
        if (description.isEmpty()) { tilDescription.setError("Description is required"); valid = false; }

        // Credentials validation
        if (username.isEmpty()) {
            tilUsername.setError("Username is required"); valid = false;
        } else if (username.length() < 4 || username.contains(" ")) {
            tilUsername.setError("Username: min 4 chars, no spaces"); valid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required"); valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters"); valid = false;
        }
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password"); valid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Passwords do not match"); valid = false;
        }

        if (attachedDocuments.isEmpty()) {
            Toast.makeText(this, "Please attach at least one supporting document.", Toast.LENGTH_LONG).show();
            valid = false;
        }

        if (!valid) return;

        setLoading(true);
        firebaseManager.submitSanghaApplication(
                this,
                orgName, regNo, contactPerson, email, mobile,
                district, locality, address, description,
                username, password,
                new ArrayList<>(attachedDocuments),
                new FirebaseManager.ApplicationSubmitCallback() {
                    @Override
                    public void onSuccess(String applicationId, String reviewToken, boolean emailSent, String emailMessage) {
                        setLoading(false);

                        getSharedPreferences(PREFS_APP, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_LAST_APP_ID, applicationId)
                                .apply();

                        String statusMsg = emailSent
                                ? "Admin (" + FirebaseManager.ADMIN_EMAIL + ") has been notified by email with Accept & Reject buttons."
                                : "Application saved! Note: Email server returned: " + emailMessage;

                        new AlertDialog.Builder(SanghaApplicationActivity.this)
                                .setTitle("Application Submitted!")
                                .setMessage("Your Application ID is:\n\n" + applicationId
                                        + "\n\nKeep this ID to track your application status.\n\n"
                                        + statusMsg)
                                .setCancelable(false)
                                .setNeutralButton("Copy ID", (d, w) -> {
                                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    if (cm != null) {
                                        cm.setPrimaryClip(ClipData.newPlainText("Application ID", applicationId));
                                    }
                                    Toast.makeText(SanghaApplicationActivity.this, "Copied " + applicationId, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .setPositiveButton("Done", (d, w) -> finish())
                                .show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(SanghaApplicationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private String textOf(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrors() {
        tilOrgName.setError(null);
        tilRegNo.setError(null);
        tilContactPerson.setError(null);
        tilEmail.setError(null);
        tilMobile.setError(null);
        tilDistrict.setError(null);
        tilLocality.setError(null);
        tilAddress.setError(null);
        tilDescription.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitApplication.setEnabled(!loading);
    }
}
