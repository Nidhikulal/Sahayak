package com.sahayak.app.sangha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.FirebaseManager;
import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.User;

import java.util.Map;

/**
 * Sangha Account Creation using a secure, single-use, expiring Approval Link/Token.
 * Can be opened directly via deep link: sahayak://sangha/complete-registration?appId=...&token=...
 * or by manually entering Application ID + Registration Token received in the approval email.
 */
public class SanghaRegistrationActivity extends AppCompatActivity {

    public static final String EXTRA_APP_ID = "extra_app_id";
    public static final String EXTRA_REG_TOKEN = "extra_reg_token";

    private TextInputLayout tilApplicationId, tilRegistrationToken, tilUsername, tilPassword, tilConfirmPassword;
    private TextInputEditText etApplicationId, etRegistrationToken, etUsername, etPassword, etConfirmPassword;
    private TextView tvVerifiedBanner;
    private LinearLayout layoutAccountSetup;
    private ProgressBar progressBar;
    private Button btnVerifyToken, btnRegister;

    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    private boolean tokenVerified = false;
    private String verifiedAppId = "";
    private String verifiedToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_registration_sangha);

        firebaseManager = FirebaseManager.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        handleIncomingIntent(getIntent());
    }

    private void initViews() {
        tilApplicationId = findViewById(R.id.tilApplicationId);
        tilRegistrationToken = findViewById(R.id.tilRegistrationToken);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etApplicationId = findViewById(R.id.etApplicationId);
        etRegistrationToken = findViewById(R.id.etRegistrationToken);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tvVerifiedBanner = findViewById(R.id.tvVerifiedBanner);
        layoutAccountSetup = findViewById(R.id.layoutAccountSetup);
        progressBar = findViewById(R.id.progressBar);
        btnVerifyToken = findViewById(R.id.btnVerifyToken);
        btnRegister = findViewById(R.id.btnRegister);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnVerifyToken.setOnClickListener(v -> verifyToken());
        btnRegister.setOnClickListener(v -> attemptRegistration());

        findViewById(R.id.tvApplyLink).setOnClickListener(v -> {
            startActivity(new Intent(SanghaRegistrationActivity.this, SanghaApplicationActivity.class));
            finish();
        });

        findViewById(R.id.tvLoginLink).setOnClickListener(v -> {
            startActivity(new Intent(SanghaRegistrationActivity.this, SanghaLoginActivity.class));
            finish();
        });
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String appId = intent.getStringExtra(EXTRA_APP_ID);
        String token = intent.getStringExtra(EXTRA_REG_TOKEN);

        Uri data = intent.getData();
        if (data != null) {
            if (data.getQueryParameter("appId") != null) appId = data.getQueryParameter("appId");
            if (data.getQueryParameter("token") != null) token = data.getQueryParameter("token");
        }

        if (appId != null && !appId.trim().isEmpty()) {
            etApplicationId.setText(appId.trim());
        }
        if (token != null && !token.trim().isEmpty()) {
            etRegistrationToken.setText(token.trim());
        }

        if (appId != null && !appId.trim().isEmpty() && token != null && !token.trim().isEmpty()) {
            verifyToken();
        }
    }

    private void verifyToken() {
        tilApplicationId.setError(null);
        tilRegistrationToken.setError(null);

        String appId = etApplicationId.getText() != null ? etApplicationId.getText().toString().trim() : "";
        String token = etRegistrationToken.getText() != null ? etRegistrationToken.getText().toString().trim() : "";

        if (appId.isEmpty()) {
            tilApplicationId.setError("Application ID is required");
            return;
        }
        if (token.isEmpty()) {
            tilRegistrationToken.setError("Registration Token is required");
            return;
        }

        setLoading(true);
        firebaseManager.validateRegistrationToken(appId, token, new FirebaseManager.ApplicationCallback() {
            @Override
            public void onSuccess(Map<String, Object> appData) {
                setLoading(false);
                tokenVerified = true;
                verifiedAppId = appId;
                verifiedToken = token;

                // Lock verified ID & Token fields
                etApplicationId.setEnabled(false);
                etRegistrationToken.setEnabled(false);
                btnVerifyToken.setVisibility(View.GONE);

                String orgName = getStringField(appData, "organizationName", "Approved Sangha");
                String email = getStringField(appData, "email", "");
                String expires = getStringField(appData, "registrationTokenExpiresAtStr", "");

                tvVerifiedBanner.setVisibility(View.VISIBLE);
                tvVerifiedBanner.setText("Verified & Approved: " + orgName
                        + "\nLocked Email: " + email
                        + (!expires.isEmpty() ? "\nLink Expires: " + expires : ""));
                layoutAccountSetup.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                tokenVerified = false;
                layoutAccountSetup.setVisibility(View.GONE);
                tvVerifiedBanner.setVisibility(View.GONE);
                Toast.makeText(SanghaRegistrationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void attemptRegistration() {
        if (!tokenVerified) {
            Toast.makeText(this, "Please verify your approval token first.", Toast.LENGTH_SHORT).show();
            return;
        }

        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (username.isEmpty()) {
            tilUsername.setError("Choose a username");
            isValid = false;
        } else if (username.length() < 4 || username.contains(" ")) {
            tilUsername.setError("Username must be at least 4 characters with no spaces");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (!isValid) return;

        setLoading(true);

        firebaseManager.completeRegistrationWithToken(
                verifiedAppId,
                verifiedToken,
                username,
                password,
                new FirebaseManager.RegisterCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> sanghaData, String sanghaId, String email) {
                        setLoading(false);

                        User user = buildSanghaUser(sanghaData, sanghaId, email);
                        syncToLocalSanghaRepository(sanghaData, sanghaId, email);
                        sessionManager.createSession(user);

                        Toast.makeText(SanghaRegistrationActivity.this,
                                "Account created! Welcome, " + user.getFullName(),
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(SanghaRegistrationActivity.this, SanghaDashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(SanghaRegistrationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private User buildSanghaUser(Map<String, Object> data, String sanghaId, String email) {
        String name = getStringField(data, "name", "Sangha Partner");
        String mobile = getStringField(data, "mobile", "");
        String address = getStringField(data, "address", getStringField(data, "Address", ""));
        String regNo = getStringField(data, "registrationNo", "");
        String district = getStringField(data, "district", "");
        String locality = getStringField(data, "locality", "");
        String description = getStringField(data, "description", "");
        String contactPerson = getStringField(data, "contactPerson", "");
        String status = getStringField(data, "status", User.STATUS_APPROVED);

        User user = new User(sanghaId, name, email, mobile, "", address, User.ROLE_SANGHA);
        user.setSanghaId(sanghaId);
        user.setRegistrationNo(regNo);
        user.setDistrict(district);
        user.setLocalityCity(locality);
        user.setDescription(description);
        user.setContactPerson(contactPerson);
        user.setApprovalStatus(status);
        return user;
    }

    private void syncToLocalSanghaRepository(Map<String, Object> data, String sanghaId, String email) {
        SanghaRepository repo = SanghaRepository.getInstance();
        if (repo.getSanghaById(sanghaId) == null) {
            Sangha s = new Sangha(
                    sanghaId,
                    getStringField(data, "name", "Sangha Partner"),
                    getStringField(data, "registrationNo", ""),
                    getStringField(data, "locality", ""),
                    getStringField(data, "city", ""),
                    getStringField(data, "district", ""),
                    getStringField(data, "address", ""),
                    getStringField(data, "description", ""),
                    getStringField(data, "description", ""),
                    getStringField(data, "mobile", ""),
                    email,
                    getStringField(data, "status", User.STATUS_APPROVED)
            );
            repo.addSangha(s);
        }
    }

    private String getStringField(Map<String, Object> data, String key, String fallback) {
        if (data == null) return fallback;
        Object val = data.get(key);
        return val instanceof String && !((String) val).isEmpty() ? (String) val : fallback;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnVerifyToken.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
    }
}
