package com.sahayak.app.sangha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
 * Sangha Partner Login — Firebase-backed.
 * Uses username + password for Firebase Auth login.
 * New organizations tap "Register Here" to submit all organization details for admin approval.
 */
public class SanghaLoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_login);

        firebaseManager = FirebaseManager.getInstance();
        sessionManager = new SessionManager(this);

        User existingUser = sessionManager.getCurrentUser();
        if (firebaseManager.getCurrentFirebaseUser() != null && sessionManager.isLoggedIn()
                && existingUser != null && User.ROLE_SANGHA.equalsIgnoreCase(existingUser.getRole())) {
            goToDashboard();
            return;
        }

        initViews();
        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri data = intent.getData();
        String username = data.getQueryParameter("username");
        if (username != null && !username.trim().isEmpty() && etUsername != null) {
            etUsername.setText(username.trim());
        }
    }

    private void initViews() {
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());
        findViewById(R.id.tvRegisterLink).setOnClickListener(v ->
                startActivity(new Intent(SanghaLoginActivity.this, SanghaApplicationActivity.class)));
    }

    private void attemptLogin() {
        tilUsername.setError(null);
        tilPassword.setError(null);

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (username.isEmpty()) { tilUsername.setError("Username is required"); return; }
        if (password.isEmpty()) { tilPassword.setError("Password is required"); return; }

        setLoading(true);

        firebaseManager.loginWithUsername(username, password, new FirebaseManager.LoginCallback() {
            @Override
            public void onSuccess(Map<String, Object> sanghaData, String sanghaId, String email) {
                setLoading(false);
                User user = buildUser(sanghaData, sanghaId, email);
                syncLocal(sanghaData, sanghaId, email);
                sessionManager.createSession(user);
                Toast.makeText(SanghaLoginActivity.this, "Welcome back, " + user.getFullName() + "!", Toast.LENGTH_SHORT).show();
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create();

        TextInputLayout tilDialogUsername = dialogView.findViewById(R.id.tilDialogUsername);
        TextInputEditText etDialogUsername = dialogView.findViewById(R.id.etDialogUsername);
        ProgressBar progressReset = dialogView.findViewById(R.id.progressReset);

        String existing = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        if (!existing.isEmpty()) etDialogUsername.setText(existing);

        dialogView.findViewById(R.id.btnCancelReset).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSendReset).setOnClickListener(v -> {
            String uname = etDialogUsername.getText() != null ? etDialogUsername.getText().toString().trim() : "";
            if (uname.isEmpty()) { tilDialogUsername.setError("Enter your username"); return; }
            tilDialogUsername.setError(null);
            progressReset.setVisibility(View.VISIBLE);
            dialogView.findViewById(R.id.btnSendReset).setEnabled(false);

            firebaseManager.sendPasswordResetForUsername(uname, new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess(String sentToEmail) {
                    dialog.dismiss();
                    new AlertDialog.Builder(SanghaLoginActivity.this)
                            .setTitle("Password Reset Email Sent")
                            .setMessage("A password reset link has been sent to the email associated with username \"" + uname + "\". Check your inbox.")
                            .setPositiveButton("OK", null).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    progressReset.setVisibility(View.GONE);
                    dialogView.findViewById(R.id.btnSendReset).setEnabled(true);
                    tilDialogUsername.setError(errorMessage);
                }
            });
        });
        dialog.show();
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, SanghaDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private User buildUser(Map<String, Object> data, String sanghaId, String email) {
        String addr = !s(data, "address").isEmpty() ? s(data, "address") : s(data, "Address");
        User user = new User(sanghaId, s(data, "name"), email, s(data, "mobile"), "", addr, User.ROLE_SANGHA);
        user.setSanghaId(sanghaId);
        user.setRegistrationNo(s(data, "registrationNo"));
        user.setDistrict(s(data, "district"));
        user.setLocalityCity(s(data, "locality"));
        user.setDescription(s(data, "description"));
        user.setContactPerson(s(data, "contactPerson"));
        user.setApprovalStatus(s(data, "status").isEmpty() ? User.STATUS_APPROVED : s(data, "status"));
        return user;
    }

    private void syncLocal(Map<String, Object> data, String sanghaId, String email) {
        if (SanghaRepository.getInstance().getSanghaById(sanghaId) == null) {
            String addr = !s(data, "address").isEmpty() ? s(data, "address") : s(data, "Address");
            SanghaRepository.getInstance().addSangha(new Sangha(
                    sanghaId, s(data, "name"), s(data, "registrationNo"), s(data, "locality"),
                    s(data, "city"), s(data, "district"), addr,
                    s(data, "description"), s(data, "description"), s(data, "mobile"),
                    email, s(data, "status").isEmpty() ? User.STATUS_APPROVED : s(data, "status")));
        }
    }

    private String s(Map<String, Object> d, String k) {
        if (d == null) return "";
        Object v = d.get(k);
        return v instanceof String ? (String) v : "";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnLogin).setEnabled(!loading);
    }
}
