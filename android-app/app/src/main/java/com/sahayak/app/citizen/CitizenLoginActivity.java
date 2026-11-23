package com.sahayak.app.citizen;

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

public class CitizenLoginActivity extends AppCompatActivity {

    private TextInputLayout tilIdentifier, tilPassword;
    private TextInputEditText etIdentifier, etPassword;

    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_login);

        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
    }

    private void initViews() {
        tilIdentifier = findViewById(R.id.tilIdentifier);
        tilPassword = findViewById(R.id.tilPassword);

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.tvRegisterLink).setOnClickListener(v -> {
            startActivity(new Intent(CitizenLoginActivity.this, RoleSelectionActivity.class));
            finish();
        });
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

        User user = userRepository.authenticate(identifier, password);
        if (user == null) {
            tilPassword.setError("Invalid credentials. Please check your email/mobile and password.");
            Toast.makeText(this, "Authentication failed. Invalid credentials.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save session securely
        sessionManager.createSession(user);

        Toast.makeText(this, "Login successful! Welcome back, " + user.getFullName(), Toast.LENGTH_SHORT).show();

        if (User.ROLE_CITIZEN.equalsIgnoreCase(user.getRole())) {
            Intent intent = new Intent(this, CitizenDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (User.ROLE_SANGHA.equalsIgnoreCase(user.getRole())) {
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
}