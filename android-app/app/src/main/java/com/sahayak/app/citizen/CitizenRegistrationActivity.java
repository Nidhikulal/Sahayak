package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.NotificationRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.User;

public class CitizenRegistrationActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilMobile, tilEmail, tilPassword, tilConfirmPassword, tilAddress;
    private TextInputEditText etFullName, etMobile, etEmail, etPassword, etConfirmPassword, etAddress;

    private UserRepository userRepository;
    private SessionManager sessionManager;
    private NotificationRepository notificationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_registration);

        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);
        notificationRepository = NotificationRepository.getInstance();

        initViews();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilMobile = findViewById(R.id.tilMobile);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilAddress = findViewById(R.id.tilAddress);

        etFullName = findViewById(R.id.etFullName);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAddress = findViewById(R.id.etAddress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnRegister).setOnClickListener(v -> attemptRegistration());

        findViewById(R.id.tvLoginLink).setOnClickListener(v -> {
            startActivity(new Intent(CitizenRegistrationActivity.this, CitizenLoginActivity.class));
            finish();
        });
    }

    private void attemptRegistration() {
        clearErrors();

        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";

        boolean isValid = true;

        if (fullName.isEmpty()) {
            tilFullName.setError("Full name is required");
            isValid = false;
        }

        if (mobile.isEmpty()) {
            tilMobile.setError("Mobile number is required");
            isValid = false;
        } else if (!mobile.matches("^[0-9]{10}$")) {
            tilMobile.setError("Enter a valid 10-digit mobile number");
            isValid = false;
        } else if (userRepository.isMobileRegistered(mobile)) {
            tilMobile.setError("Mobile number is already registered");
            isValid = false;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Email address is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            isValid = false;
        } else if (userRepository.isEmailRegistered(email)) {
            tilEmail.setError("Email address is already registered");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters long");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (address.isEmpty()) {
            tilAddress.setError("Residential address is required");
            isValid = false;
        }

        if (!isValid) return;

        // Create User
        User user = new User(null, fullName, email, mobile, password, address, User.ROLE_CITIZEN);
        User registeredUser = userRepository.registerUser(user);

        // Auto-login session creation
        sessionManager.createSession(registeredUser);

        // Add Welcome Notification
        notificationRepository.addNotification(
                registeredUser.getUserId(),
                "Welcome to Sahayak!",
                "Your citizen account has been created successfully. You can now report people in need and sponsor money.",
                "REGISTRATION"
        );

        Toast.makeText(this, "Registration Successful! Welcome " + fullName, Toast.LENGTH_LONG).show();

        // Redirect directly to Citizen Dashboard
        Intent intent = new Intent(this, CitizenDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilMobile.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilAddress.setError(null);
    }
}