package com.sahayak.app.sangha;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sangha Partner Registration Activity.
 * Captures comprehensive NGO details in accordance with Phase 1 requirements:
 * Organization Name, Official Registration No, Contact Person, Mobile, Email,
 * Password, District, Locality, Address, Service Categories, and Description.
 * Automatically saves new registration with status PENDING and routes to SanghaApprovalActivity.
 */
public class SanghaRegistrationActivity extends AppCompatActivity {

    private TextInputLayout tilSanghaName, tilRegNo, tilContactPerson, tilMobile, tilEmail;
    private TextInputLayout tilPassword, tilConfirmPassword, tilDistrict, tilLocality, tilAddress, tilDescription;

    private TextInputEditText etSanghaName, etRegNo, etContactPerson, etMobile, etEmail;
    private TextInputEditText etPassword, etConfirmPassword, etDistrict, etLocality, etAddress, etDescription;

    private ChipGroup chipGroupServices;

    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_registration_sangha);

        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
    }

    private void initViews() {
        tilSanghaName = findViewById(R.id.tilSanghaName);
        tilRegNo = findViewById(R.id.tilRegNo);
        tilContactPerson = findViewById(R.id.tilContactPerson);
        tilMobile = findViewById(R.id.tilMobile);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilDistrict = findViewById(R.id.tilDistrict);
        tilLocality = findViewById(R.id.tilLocality);
        tilAddress = findViewById(R.id.tilAddress);
        tilDescription = findViewById(R.id.tilDescription);

        etSanghaName = findViewById(R.id.etSanghaName);
        etRegNo = findViewById(R.id.etRegNo);
        etContactPerson = findViewById(R.id.etContactPerson);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etDistrict = findViewById(R.id.etDistrict);
        etLocality = findViewById(R.id.etLocality);
        etAddress = findViewById(R.id.etAddress);
        etDescription = findViewById(R.id.etDescription);

        chipGroupServices = findViewById(R.id.chipGroupServices);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnRegister).setOnClickListener(v -> attemptRegistration());

        findViewById(R.id.tvLoginLink).setOnClickListener(v -> {
            startActivity(new Intent(SanghaRegistrationActivity.this, SanghaLoginActivity.class));
            finish();
        });
    }

    private void attemptRegistration() {
        clearErrors();

        String orgName = etSanghaName.getText() != null ? etSanghaName.getText().toString().trim() : "";
        String regNo = etRegNo.getText() != null ? etRegNo.getText().toString().trim() : "";
        String contactPerson = etContactPerson.getText() != null ? etContactPerson.getText().toString().trim() : "";
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
        String district = etDistrict.getText() != null ? etDistrict.getText().toString().trim() : "";
        String locality = etLocality.getText() != null ? etLocality.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        boolean isValid = true;

        if (orgName.isEmpty()) {
            tilSanghaName.setError("Organization name is required");
            isValid = false;
        }

        if (regNo.isEmpty()) {
            tilRegNo.setError("Registration / Darpan number is required");
            isValid = false;
        }

        if (contactPerson.isEmpty()) {
            tilContactPerson.setError("Contact person name is required");
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

        if (district.isEmpty()) {
            tilDistrict.setError("District is required");
            isValid = false;
        }

        if (locality.isEmpty()) {
            tilLocality.setError("Locality is required");
            isValid = false;
        }

        if (address.isEmpty()) {
            tilAddress.setError("Office address is required");
            isValid = false;
        }

        if (description.isEmpty()) {
            tilDescription.setError("Brief organization description is required");
            isValid = false;
        }

        if (!isValid) return;

        // Collect selected service categories
        List<String> selectedServices = new ArrayList<>();
        for (int i = 0; i < chipGroupServices.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupServices.getChildAt(i);
            if (chip.isChecked()) {
                selectedServices.add(chip.getText().toString());
            }
        }
        if (selectedServices.isEmpty()) {
            selectedServices.add("General Community Welfare");
        }

        String newSanghaId = "SNG-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // Create User entity for authentication
        User sanghaUser = new User(null, orgName, email, mobile, password, address, User.ROLE_SANGHA);
        sanghaUser.setSanghaId(newSanghaId);
        sanghaUser.setRegistrationNo(regNo);
        sanghaUser.setContactPerson(contactPerson);
        sanghaUser.setDistrict(district);
        sanghaUser.setLocalityCity(locality);
        sanghaUser.setDescription(description);
        sanghaUser.setApprovalStatus(User.STATUS_PENDING);

        User registeredUser = userRepository.registerUser(sanghaUser);

        // Also register in SanghaRepository
        Sangha sanghaProfile = new Sangha(
                newSanghaId,
                orgName,
                regNo,
                locality,
                locality,
                district,
                address,
                description,
                description,
                mobile,
                email,
                User.STATUS_PENDING
        );
        sanghaProfile.setAssistanceActivities(selectedServices);
        SanghaRepository.getInstance().addSangha(sanghaProfile);

        // Save session
        sessionManager.createSession(registeredUser);

        Toast.makeText(this, "Registration Submitted! Awaiting Administrative Verification.", Toast.LENGTH_LONG).show();

        // Route to approval pending screen
        Intent intent = new Intent(this, SanghaApprovalActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearErrors() {
        tilSanghaName.setError(null);
        tilRegNo.setError(null);
        tilContactPerson.setError(null);
        tilMobile.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilDistrict.setError(null);
        tilLocality.setError(null);
        tilAddress.setError(null);
        tilDescription.setError(null);
    }
}

