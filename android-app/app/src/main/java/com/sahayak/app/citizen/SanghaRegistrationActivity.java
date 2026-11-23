package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.User;

public class SanghaRegistrationActivity extends AppCompatActivity {

    private TextInputLayout tilSanghaName, tilRegNo, tilMobile, tilEmail, tilPassword, tilAddress, tilDescription;
    private TextInputEditText etSanghaName, etRegNo, etMobile, etEmail, etPassword, etAddress, etDescription;

    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_registration);

        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
    }

    private void initViews() {
        tilSanghaName = findViewById(R.id.tilSanghaName);
        tilRegNo = findViewById(R.id.tilRegNo);
        tilMobile = findViewById(R.id.tilMobile);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilAddress = findViewById(R.id.tilAddress);
        tilDescription = findViewById(R.id.tilDescription);

        etSanghaName = findViewById(R.id.etSanghaName);
        etRegNo = findViewById(R.id.etRegNo);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etAddress = findViewById(R.id.etAddress);
        etDescription = findViewById(R.id.etDescription);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnRegister).setOnClickListener(v -> attemptSanghaRegistration());
    }

    private void attemptSanghaRegistration() {
        clearErrors();

        String name = etSanghaName.getText() != null ? etSanghaName.getText().toString().trim() : "";
        String regNo = etRegNo.getText() != null ? etRegNo.getText().toString().trim() : "";
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        boolean isValid = true;

        if (name.isEmpty()) {
            tilSanghaName.setError("Organisation name is required");
            isValid = false;
        }

        if (regNo.isEmpty()) {
            tilRegNo.setError("Registration number is required");
            isValid = false;
        }

        if (mobile.isEmpty()) {
            tilMobile.setError("Mobile number is required");
            isValid = false;
        } else if (!mobile.matches("^[0-9]{10}$")) {
            tilMobile.setError("Enter a valid 10-digit mobile number");
            isValid = false;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Email address is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        }

        if (address.isEmpty()) {
            tilAddress.setError("Office address is required");
            isValid = false;
        }

        if (description.isEmpty()) {
            tilDescription.setError("Brief description is required");
            isValid = false;
        }

        if (!isValid) return;

        User sanghaUser = new User(null, name, email, mobile, password, address, User.ROLE_SANGHA);
        sanghaUser.setRegistrationNo(regNo);
        sanghaUser.setDescription(description);
        sanghaUser.setApprovalStatus(User.STATUS_PENDING);

        User registered = userRepository.registerUser(sanghaUser);
        sessionManager.createSession(registered);

        Toast.makeText(this, "Sangha Registered! Approval Pending.", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, SanghaApprovalActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearErrors() {
        tilSanghaName.setError(null);
        tilRegNo.setError(null);
        tilMobile.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilAddress.setError(null);
        tilDescription.setError(null);
    }
}