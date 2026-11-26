package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.FirebaseManager;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.User;

import java.util.Map;

/**
 * Citizen Portal Login — Firebase-backed, matching Sangha Login flow.
 * Opens first when Citizen is selected; unregistered users tap "Register Here".
 */
public class CitizenLoginActivity extends AppCompatActivity {

    private TextInputLayout tilIdentifier, tilPassword;
    private TextInputEditText etIdentifier, etPassword;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_login);

        firebaseManager = FirebaseManager.getInstance();
        sessionManager = new SessionManager(this);

        User existingUser = sessionManager.getCurrentUser();
        if (firebaseManager.getCurrentFirebaseUser() != null && sessionManager.isLoggedIn()
                && existingUser != null && User.ROLE_CITIZEN.equalsIgnoreCase(existingUser.getRole())) {
            goToDashboard();
            return;
        }

        initViews();
    }

    private void initViews() {
        tilIdentifier = findViewById(R.id.tilIdentifier);
        tilPassword = findViewById(R.id.tilPassword);

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());

        findViewById(R.id.tvRegisterLink).setOnClickListener(v ->
                startActivity(new Intent(CitizenLoginActivity.this, CitizenRegistrationActivity.class)));
    }

    private void attemptLogin() {
        tilIdentifier.setError(null);
        tilPassword.setError(null);

        String identifier = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (identifier.isEmpty()) {
            tilIdentifier.setError("Email or mobile number is required");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        }

        if (!isValid) return;

        setLoading(true);

        firebaseManager.loginCitizen(identifier, password, new FirebaseManager.CitizenCallback() {
            @Override
            public void onSuccess(Map<String, Object> citizenData, String uid, String email) {
                setLoading(false);

                String fullName = getStringField(citizenData, "fullName", "Citizen");
                String mobile = getStringField(citizenData, "mobile", "");
                String address = getStringField(citizenData, "address", "");
                String dob = getStringField(citizenData, "dob", "");
                String gender = getStringField(citizenData, "gender", "");
                String pinCode = getStringField(citizenData, "pinCode", "");

                User user = new User(uid, fullName, email, mobile, "", dob, gender, address, pinCode, User.ROLE_CITIZEN);
                sessionManager.createSession(user);

                Toast.makeText(CitizenLoginActivity.this,
                        "Welcome back, " + fullName + "!", Toast.LENGTH_SHORT).show();

                goToDashboard();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                tilPassword.setError(errorMessage);
            }
        });
    }

    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        TextInputEditText etResetIdentifier = dialogView.findViewById(R.id.etDialogUsername);
        TextInputLayout tilResetIdentifier = dialogView.findViewById(R.id.tilDialogUsername);
        ProgressBar progressReset = dialogView.findViewById(R.id.progressReset);

        if (tvSubtitle != null) {
            tvSubtitle.setText("Enter your registered email or mobile number to receive a password reset link.");
        }
        tilResetIdentifier.setHint("Email or Mobile Number");

        String existing = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        if (!existing.isEmpty()) etResetIdentifier.setText(existing);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.btnSendReset).setOnClickListener(v -> {
            String identifier = etResetIdentifier.getText() != null ? etResetIdentifier.getText().toString().trim() : "";
            if (identifier.isEmpty()) {
                tilResetIdentifier.setError("Enter your registered email or mobile number");
                return;
            }
            tilResetIdentifier.setError(null);
            if (progressReset != null) progressReset.setVisibility(View.VISIBLE);
            dialogView.findViewById(R.id.btnSendReset).setEnabled(false);

            firebaseManager.sendCitizenPasswordReset(identifier, new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess(String sentToEmail) {
                    dialog.dismiss();
                    new AlertDialog.Builder(CitizenLoginActivity.this)
                            .setTitle("Password Reset Email Sent")
                            .setMessage("A password reset link has been sent to " + sentToEmail + ". Check your inbox.")
                            .setPositiveButton("OK", null).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (progressReset != null) progressReset.setVisibility(View.GONE);
                    dialogView.findViewById(R.id.btnSendReset).setEnabled(true);
                    tilResetIdentifier.setError(errorMessage);
                }
            });
        });

        dialogView.findViewById(R.id.btnCancelReset).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void goToDashboard() {
        Intent intent = new Intent(CitizenLoginActivity.this, CitizenDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        findViewById(R.id.btnLogin).setEnabled(!loading);
    }

    private String getStringField(Map<String, Object> data, String key, String fallback) {
        if (data == null) return fallback;
        Object val = data.get(key);
        return val instanceof String && !((String) val).isEmpty() ? (String) val : fallback;
    }
}
