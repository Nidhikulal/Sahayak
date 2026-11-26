package com.sahayak.app.sangha;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sahayak.app.R;
import com.sahayak.app.data.EmailServiceClient;
import com.sahayak.app.data.FirebaseManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin Verification Review Portal.
 * Opens either directly from the secure email review link (sahayak://sangha/review?appId=...&token=...)
 * or from the Sangha Portal's "Admin Review Portal" entry.
 * Protected by Admin credentials (sahayakadmin65@gmail.com) + review token validation.
 */
public class AdminReviewActivity extends AppCompatActivity {

    private static final String DEFAULT_ADMIN_PASSCODE = "SahayakAdmin@2026";

    private LinearLayout layoutApplicationsContainer;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private FirebaseManager firebaseManager;
    private final List<Map<String, Object>> allApplications = new ArrayList<>();
    private String currentFilter = FirebaseManager.STATUS_PENDING;

    private String deepLinkAppId = "";
    private String deepLinkReviewToken = "";
    private boolean adminAuthorized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_review);

        firebaseManager = FirebaseManager.getInstance();
        initViews();
        parseIntent(getIntent());
        promptAdminAuth();
    }

    private void initViews() {
        layoutApplicationsContainer = findViewById(R.id.layoutApplicationsContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnServerSettings).setOnClickListener(v -> showServerUrlDialog());

        findViewById(R.id.btnFilterPending).setOnClickListener(v -> setFilter(FirebaseManager.STATUS_PENDING));
        findViewById(R.id.btnFilterInfo).setOnClickListener(v -> setFilter(FirebaseManager.STATUS_INFO_REQUESTED));
        findViewById(R.id.btnFilterApproved).setOnClickListener(v -> setFilter(FirebaseManager.STATUS_APPROVED));
        findViewById(R.id.btnFilterRejected).setOnClickListener(v -> setFilter(FirebaseManager.STATUS_REJECTED));
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> setFilter("ALL"));
    }

    private void parseIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            String appId = data.getQueryParameter("appId");
            String token = data.getQueryParameter("token");
            if (appId != null) deepLinkAppId = appId.trim();
            if (token != null) deepLinkReviewToken = token.trim();
        }
    }

    private void promptAdminAuth() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 12);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Sign in as Admin (" + FirebaseManager.ADMIN_EMAIL + ") to review Sangha applications.\nDefault Passcode: " + DEFAULT_ADMIN_PASSCODE);
        tvInfo.setTextSize(12f);
        layout.addView(tvInfo);

        EditText etEmail = new EditText(this);
        etEmail.setHint("Admin Email");
        etEmail.setText(FirebaseManager.ADMIN_EMAIL);
        layout.addView(etEmail);

        EditText etPasscode = new EditText(this);
        etPasscode.setHint("Admin Passcode");
        etPasscode.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPasscode);

        new AlertDialog.Builder(this)
                .setTitle("Admin Verification Required")
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton("Cancel", (d, w) -> finish())
                .setPositiveButton("Verify", (d, w) -> {
                    String em = etEmail.getText().toString().trim();
                    String pass = etPasscode.getText().toString().trim();
                    if (FirebaseManager.ADMIN_EMAIL.equalsIgnoreCase(em) && DEFAULT_ADMIN_PASSCODE.equals(pass)) {
                        adminAuthorized = true;
                        onAdminAuthorized();
                    } else {
                        Toast.makeText(this, "Unauthorized admin credentials.", Toast.LENGTH_LONG).show();
                        promptAdminAuth();
                    }
                })
                .show();
    }

    private void onAdminAuthorized() {
        loadApplications();
        if (!deepLinkAppId.isEmpty() && !deepLinkReviewToken.isEmpty()) {
            firebaseManager.verifyAdminReviewLink(deepLinkAppId, deepLinkReviewToken, new FirebaseManager.ApplicationCallback() {
                @Override
                public void onSuccess(Map<String, Object> applicationData) {
                    showApplicationDetailDialog(applicationData);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(AdminReviewActivity.this, "Review link error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        renderList();
    }

    private void loadApplications() {
        if (!adminAuthorized) return;
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        layoutApplicationsContainer.removeAllViews();

        firebaseManager.fetchAllSanghaApplications(new FirebaseManager.ApplicationListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> applications) {
                progressBar.setVisibility(View.GONE);
                allApplications.clear();
                allApplications.addAll(applications);
                renderList();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminReviewActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderList() {
        layoutApplicationsContainer.removeAllViews();
        int count = 0;

        for (Map<String, Object> app : allApplications) {
            String status = str(app, "status");
            if (!"ALL".equals(currentFilter) && !currentFilter.equalsIgnoreCase(status)) {
                continue;
            }
            count++;
            layoutApplicationsContainer.addView(buildApplicationCard(app));
        }

        tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private View buildApplicationCard(Map<String, Object> app) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 24;
        card.setLayoutParams(lp);
        card.setRadius(32f);
        card.setCardElevation(6f);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(36, 32, 36, 32);

        String appId = str(app, "applicationId");
        String orgName = str(app, "organizationName");
        String regNo = str(app, "registrationNo");
        String email = str(app, "email");
        String status = str(app, "status");
        int docsCount = app.get("documentCount") instanceof Number ? ((Number) app.get("documentCount")).intValue() : 0;

        TextView tvBadge = new TextView(this);
        tvBadge.setText(status);
        tvBadge.setTextSize(11f);
        tvBadge.setTextColor(ContextCompat.getColor(this, R.color.white));
        tvBadge.setPadding(20, 6, 20, 6);
        tvBadge.setBackgroundColor(colorForStatus(status));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvBadge.setLayoutParams(badgeLp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(orgName);
        tvTitle.setTextSize(16f);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_primary));
        tvTitle.setPadding(0, 14, 0, 4);

        TextView tvSub = new TextView(this);
        tvSub.setText("ID: " + appId + "  |  Reg: " + regNo + "\n" + email + "  |  Docs: " + docsCount);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_secondary));

        MaterialButton btnReview = new MaterialButton(this);
        btnReview.setText("Review Details & Documents");
        btnReview.setAllCaps(false);
        btnReview.setOnClickListener(v -> showApplicationDetailDialog(app));

        inner.addView(tvBadge);
        inner.addView(tvTitle);
        inner.addView(tvSub);
        inner.addView(btnReview);
        card.addView(inner);
        return card;
    }

    @SuppressWarnings("unchecked")
    private void showApplicationDetailDialog(Map<String, Object> app) {
        String appId = str(app, "applicationId");
        String status = str(app, "status");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(44, 28, 44, 20);
        scrollView.addView(root);

        TextView tvDetails = new TextView(this);
        StringBuilder details = new StringBuilder();
        details.append("Application ID: ").append(appId).append("\n")
                .append("Status: ").append(status).append("\n\n")
                .append("Organization: ").append(str(app, "organizationName")).append("\n")
                .append("Registration No: ").append(str(app, "registrationNo")).append("\n")
                .append("Contact Person: ").append(str(app, "contactPerson")).append("\n")
                .append("Email: ").append(str(app, "email")).append("\n")
                .append("Mobile: ").append(str(app, "mobile")).append("\n")
                .append("Location: ").append(str(app, "locality")).append(", ").append(str(app, "district")).append("\n")
                .append("Address: ").append(str(app, "address")).append("\n\n")
                .append("Activities:\n").append(str(app, "description"));

        String sanghaResp = str(app, "sanghaResponseNote");
        if (!sanghaResp.isEmpty()) {
            details.append("\n\nSangha Response Note:\n").append(sanghaResp);
        }
        String adminRemarks = str(app, "adminRemarks");
        if (!adminRemarks.isEmpty()) {
            details.append("\n\nPrevious Admin Remarks:\n").append(adminRemarks);
        }
        tvDetails.setText(details.toString());
        tvDetails.setTextSize(13f);
        tvDetails.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_primary));
        root.addView(tvDetails);

        // Supporting documents section
        TextView tvDocsHeader = new TextView(this);
        tvDocsHeader.setText("\nSupporting Documents (Tap to View):");
        tvDocsHeader.setTextSize(13f);
        tvDocsHeader.setTextColor(ContextCompat.getColor(this, R.color.sahayak_primary));
        root.addView(tvDocsHeader);

        Object docsObj = app.get("documents");
        if (docsObj instanceof List<?> && !((List<?>) docsObj).isEmpty()) {
            for (Object item : (List<?>) docsObj) {
                if (!(item instanceof Map<?, ?>)) continue;
                Map<String, Object> docMap = (Map<String, Object>) item;
                String fName = str(docMap, "fileName");
                String mime = str(docMap, "mimeType");
                String base64 = str(docMap, "base64Data");
                long size = docMap.get("sizeBytes") instanceof Number ? ((Number) docMap.get("sizeBytes")).longValue() : 0L;

                MaterialButton docBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                docBtn.setText(fName + " (" + DocumentAttachmentHelper.formatSize(size) + ")");
                docBtn.setAllCaps(false);
                docBtn.setOnClickListener(v -> viewAttachedDocument(fName, mime, base64));
                root.addView(docBtn);
            }
        } else {
            TextView tvNoDocs = new TextView(this);
            tvNoDocs.setText("No documents attached.");
            tvNoDocs.setTextSize(12f);
            root.addView(tvNoDocs);
        }

        AlertDialog detailDialog = new AlertDialog.Builder(this)
                .setTitle("Application Review")
                .setView(scrollView)
                .setNegativeButton("Close", null)
                .create();

        if (FirebaseManager.STATUS_PENDING.equalsIgnoreCase(status)
                || FirebaseManager.STATUS_INFO_REQUESTED.equalsIgnoreCase(status)) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.VERTICAL);
            actions.setPadding(0, 24, 0, 0);

            MaterialButton btnApprove = new MaterialButton(this);
            btnApprove.setText("Approve & Send Registration Link");
            btnApprove.setAllCaps(false);
            btnApprove.setBackgroundColor(ContextCompat.getColor(this, R.color.status_completed));
            btnApprove.setOnClickListener(v -> {
                detailDialog.dismiss();
                performDecision(appId, FirebaseManager.STATUS_APPROVED, "Approved by Sahayak Admin.");
            });

            MaterialButton btnMoreInfo = new MaterialButton(this);
            btnMoreInfo.setText("Request More Information");
            btnMoreInfo.setAllCaps(false);
            btnMoreInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.status_in_progress));
            btnMoreInfo.setOnClickListener(v -> {
                detailDialog.dismiss();
                promptReasonAndSubmit(appId, FirebaseManager.STATUS_INFO_REQUESTED,
                        "Request More Information", "Describe what extra details or documents are required:");
            });

            MaterialButton btnReject = new MaterialButton(this);
            btnReject.setText("Reject Application");
            btnReject.setAllCaps(false);
            btnReject.setBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected));
            btnReject.setOnClickListener(v -> {
                detailDialog.dismiss();
                promptReasonAndSubmit(appId, FirebaseManager.STATUS_REJECTED,
                        "Reject Application", "Enter mandatory rejection reason (will be emailed to Sangha):");
            });

            actions.addView(btnApprove);
            actions.addView(btnMoreInfo);
            actions.addView(btnReject);
            root.addView(actions);
        }

        detailDialog.show();
    }

    private void viewAttachedDocument(String fileName, String mimeType, String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            Toast.makeText(this, "Document content unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mimeType != null && mimeType.startsWith("image/")) {
            Bitmap bmp = DocumentAttachmentHelper.decodeImage(base64Data);
            if (bmp == null) {
                Toast.makeText(this, "Could not decode image.", Toast.LENGTH_SHORT).show();
                return;
            }
            ImageView iv = new ImageView(this);
            iv.setAdjustViewBounds(true);
            iv.setImageBitmap(bmp);
            iv.setPadding(24, 24, 24, 24);
            new AlertDialog.Builder(this)
                    .setTitle(fileName)
                    .setView(iv)
                    .setPositiveButton("Close", null)
                    .show();
        } else {
            try {
                File out = DocumentAttachmentHelper.writeTempFile(this, fileName, base64Data);
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", out);
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(uri, mimeType != null ? mimeType : "application/pdf");
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(viewIntent, "Open Document"));
            } catch (Exception e) {
                Toast.makeText(this, "No viewer app found for " + fileName, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void promptReasonAndSubmit(String appId, String decision, String title, String hint) {
        EditText etReason = new EditText(this);
        etReason.setHint(hint);
        etReason.setMinLines(3);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(48, 24, 48, 12);
        wrapper.addView(etReason, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrapper)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit & Email Sangha", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Please enter a message/reason.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    performDecision(appId, decision, reason);
                })
                .show();
    }

    private void performDecision(String appId, String decision, String remarks) {
        progressBar.setVisibility(View.VISIBLE);
        firebaseManager.submitAdminDecision(this, appId, decision, remarks, new FirebaseManager.AdminDecisionCallback() {
            @Override
            public void onSuccess(String dec, String regToken, String expiresAtFormatted, boolean emailSent, String emailMessage) {
                progressBar.setVisibility(View.GONE);
                StringBuilder msg = new StringBuilder();
                msg.append("Decision saved: ").append(dec).append("\n\n");
                if (emailSent) {
                    msg.append("Notification email sent to Sangha via Nodemailer.");
                } else {
                    msg.append("Email not sent (").append(emailMessage).append(").");
                }
                if (FirebaseManager.STATUS_APPROVED.equalsIgnoreCase(dec) && regToken != null) {
                    msg.append("\n\nSingle-use Token:\n").append(regToken)
                            .append("\nExpires: ").append(expiresAtFormatted);
                }

                new AlertDialog.Builder(AdminReviewActivity.this)
                        .setTitle("Decision Recorded")
                        .setMessage(msg.toString())
                        .setPositiveButton("OK", (d, w) -> loadApplications())
                        .show();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminReviewActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showServerUrlDialog() {
        EditText etUrl = new EditText(this);
        etUrl.setText(EmailServiceClient.getBaseUrl(this));
        etUrl.setHint("e.g. http://10.0.2.2:3000");
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(48, 24, 48, 12);
        wrapper.addView(etUrl, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Nodemailer Server URL")
                .setMessage("Use http://10.0.2.2:3000 for Emulator, or http://<PC-IP>:3000 on physical device.")
                .setView(wrapper)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    EmailServiceClient.setBaseUrl(this, etUrl.getText().toString());
                    Toast.makeText(this, "Server URL updated.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private int colorForStatus(String status) {
        if (FirebaseManager.STATUS_APPROVED.equalsIgnoreCase(status)
                || FirebaseManager.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            return ContextCompat.getColor(this, R.color.status_completed);
        } else if (FirebaseManager.STATUS_REJECTED.equalsIgnoreCase(status)) {
            return ContextCompat.getColor(this, R.color.status_rejected);
        } else if (FirebaseManager.STATUS_INFO_REQUESTED.equalsIgnoreCase(status)) {
            return ContextCompat.getColor(this, R.color.status_in_progress);
        }
        return ContextCompat.getColor(this, R.color.status_verification);
    }

    private String str(Map<String, Object> d, String key) {
        if (d == null) return "";
        Object v = d.get(key);
        return v instanceof String ? (String) v : "";
    }
}
