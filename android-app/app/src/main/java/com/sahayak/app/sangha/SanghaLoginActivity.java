package com.sahayak.app.sangha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.User;

/**
 * Dedicated Sangha Partner Login Activity.
 * Authenticates NGO organizations against shared user credentials.
 * Includes one-click demo credentials for testing approved, pending, and rejected states.
 */
public class SanghaLoginActivity extends AppCompatActivity {

    private TextInputLayout tilIdentifier, tilPassword;
    private TextInputEditText etIdentifier, etPassword;

    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_login);

        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupDemoShortcuts();
    }

    private void initViews() {
        tilIdentifier = findViewById(R.id.tilIdentifier);
        tilPassword = findViewById(R.id.tilPassword);

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.tvRegisterLink).setOnClickListener(v -> {
            startActivity(new Intent(SanghaLoginActivity.this, SanghaRegistrationActivity.class));
        });
    }

    private void setupDemoShortcuts() {
        // 1. One-click Approved Sangha (Seva Bharathi)
        findViewById(R.id.btnDemoApprovedSangha).setOnClickListener(v -> {
            etIdentifier.setText("sangha@sahayak.org");
            etPassword.setText("Password@123");
            attemptLogin();
        });

        // 2. One-click Pending Sangha (Karuna Foundation)
        findViewById(R.id.btnDemoPendingSangha).setOnClickListener(v -> {
            etIdentifier.setText("karuna@sahayak.org");
            etPassword.setText("Password@123");
            attemptLogin();
        });

        // 3. One-click Rejected Sangha (Janata Mandali)
        findViewById(R.id.btnDemoRejectedSangha).setOnClickListener(v -> {
            etIdentifier.setText("rejected@sahayak.org");
            etPassword.setText("Password@123");
            attemptLogin();
        });
    }

    private void attemptLogin() {
        tilIdentifier.setError(null);
        tilPassword.setError(null);

        String identifier = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (identifier.isEmpty()) {
            tilIdentifier.setError("Registered email or mobile is required");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        }

        if (!isValid) return;

        User user = userRepository.authenticate(identifier, password);
        if (user == null) {
            tilPassword.setError("Invalid credentials. Please verify your email/mobile and password.");
            Toast.makeText(this, "Authentication failed. Invalid Sangha credentials.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!User.ROLE_SANGHA.equalsIgnoreCase(user.getRole())) {
            Toast.makeText(this, "This account is a Citizen account. Please use Citizen sign-in.", Toast.LENGTH_LONG).show();
            return;
        }

        // Store active session
        sessionManager.createSession(user);

        Toast.makeText(this, "Welcome, " + user.getFullName() + "!", Toast.LENGTH_SHORT).show();

        if (User.STATUS_APPROVED.equalsIgnoreCase(user.getApprovalStatus())) {
            Intent intent = new Intent(this, SanghaDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, SanghaApprovalActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }
}

