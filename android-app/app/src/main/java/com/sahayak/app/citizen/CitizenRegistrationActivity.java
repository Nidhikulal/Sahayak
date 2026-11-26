package com.sahayak.app.citizen;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.FirebaseManager;
import com.sahayak.app.data.NotificationRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.User;

import java.util.Calendar;
import java.util.Locale;

public class CitizenRegistrationActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilMobile, tilEmail, tilDateOfBirth, tilGender, tilAddress, tilPinCode, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etMobile, etEmail, etDateOfBirth, etAddress, etPinCode, etPassword, etConfirmPassword;
    private MaterialAutoCompleteTextView actvGender;

    private UserRepository userRepository;
    private SessionManager sessionManager;
    private NotificationRepository notificationRepository;

    private String generatedOtp = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_registration);

        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);
        notificationRepository = NotificationRepository.getInstance();

        initViews();
        setupDatePicker();
        setupGenderDropdown();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilMobile = findViewById(R.id.tilMobile);
        tilEmail = findViewById(R.id.tilEmail);
        tilDateOfBirth = findViewById(R.id.tilDateOfBirth);
        tilGender = findViewById(R.id.tilGender);
        tilAddress = findViewById(R.id.tilAddress);
        tilPinCode = findViewById(R.id.tilPinCode);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etFullName = findViewById(R.id.etFullName);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        actvGender = findViewById(R.id.actvGender);
        etAddress = findViewById(R.id.etAddress);
        etPinCode = findViewById(R.id.etPinCode);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnCreateAccount).setOnClickListener(v -> attemptRegistration());

        View tvLoginLink = findViewById(R.id.tvLoginLink);
        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                startActivity(new Intent(CitizenRegistrationActivity.this, CitizenLoginActivity.class));
                finish();
            });
        }
    }

    private void setupDatePicker() {
        View.OnClickListener datePickerListener = v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR) - 20; // Default to 20 years ago
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    CitizenRegistrationActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        etDateOfBirth.setText(formattedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        };

        etDateOfBirth.setOnClickListener(datePickerListener);
        tilDateOfBirth.setOnClickListener(datePickerListener);
    }

    private void setupGenderDropdown() {
        String[] genderOptions = new String[]{"Male", "Female", "Other", "Prefer not to say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genderOptions);
        actvGender.setAdapter(adapter);
    }

    private void attemptRegistration() {
        clearErrors();

        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String dob = etDateOfBirth.getText() != null ? etDateOfBirth.getText().toString().trim() : "";
        String gender = actvGender.getText() != null ? actvGender.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String pinCode = etPinCode.getText() != null ? etPinCode.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

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
        }

        if (email.isEmpty()) {
            tilEmail.setError("Email address is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            isValid = false;
        }

        if (address.isEmpty()) {
            tilAddress.setError("Residential address (Locality and City) is required");
            isValid = false;
        }

        if (pinCode.isEmpty()) {
            tilPinCode.setError("PIN code is required");
            isValid = false;
        } else if (!pinCode.matches("^[0-9]{6}$")) {
            tilPinCode.setError("Enter a valid 6-digit PIN code");
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

        if (!isValid) return;

        // Immediately save citizen details to Firebase Auth & Firestore
        completeAccountCreation(fullName, email, mobile, password, dob, gender, address, pinCode);
    }

    private void showOtpVerificationDialog(String fullName, String email, String mobile, String password,
                                          String dob, String gender, String address, String pinCode) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verification, null);

        TextView tvOtpSubtitle = dialogView.findViewById(R.id.tvOtpSubtitle);
        TextView tvDemoOtpHint = dialogView.findViewById(R.id.tvDemoOtpHint);
        TextInputLayout tilOtpInput = dialogView.findViewById(R.id.tilOtpInput);
        TextInputEditText etOtpInput = dialogView.findViewById(R.id.etOtpInput);

        tvOtpSubtitle.setText("Enter the 6-digit verification code sent to +91 " + mobile);
        tvDemoOtpHint.setText("Demo Verification Code: " + generatedOtp);

        AlertDialog otpDialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.btnVerifyOtp).setOnClickListener(v -> {
            String enteredOtp = etOtpInput.getText() != null ? etOtpInput.getText().toString().trim() : "";
            if (enteredOtp.isEmpty()) {
                tilOtpInput.setError("Please enter the 6-digit OTP");
            } else if (!enteredOtp.equals(generatedOtp)) {
                tilOtpInput.setError("Invalid OTP code. Use demo code: " + generatedOtp);
            } else {
                tilOtpInput.setError(null);
                otpDialog.dismiss();

                // Create Account
                completeAccountCreation(fullName, email, mobile, password, dob, gender, address, pinCode);
            }
        });

        dialogView.findViewById(R.id.btnResendOtp).setOnClickListener(v -> {
            generatedOtp = String.format(Locale.getDefault(), "%06d", (int) (Math.random() * 900000) + 100000);
            tvDemoOtpHint.setText("Demo Verification Code: " + generatedOtp);
            Toast.makeText(CitizenRegistrationActivity.this, "New OTP sent to +91 " + mobile, Toast.LENGTH_SHORT).show();
        });

        otpDialog.show();
    }

    private void completeAccountCreation(String fullName, String email, String mobile, String password,
                                        String dob, String gender, String address, String pinCode) {
        // Show loading
        findViewById(R.id.btnCreateAccount).setEnabled(false);

        FirebaseManager firebaseManager = FirebaseManager.getInstance();

        if (!firebaseManager.isFirebaseReady()) {
            // Fallback to local if Firebase not configured
            createLocalAccount(fullName, email, mobile, password, dob, gender, address, pinCode);
            return;
        }

        // Register with Firebase Auth + save to Firestore "citizens" collection
        firebaseManager.registerCitizen(fullName, email, mobile, password, dob, gender, address, pinCode,
                new FirebaseManager.CitizenCallback() {
                    @Override
                    public void onSuccess(java.util.Map<String, Object> citizenData, String uid, String registeredEmail) {
                        // Create local session from Firebase data
                        User user = new User(uid, fullName, email, mobile, password, dob, gender, address, pinCode, User.ROLE_CITIZEN);
                        sessionManager.createSession(user);

                        // Also keep in local repo for other screens that may reference it
                        userRepository.registerUser(user);

                        notificationRepository.addNotification(
                                uid,
                                "Welcome to Sahayak!",
                                "Your citizen account has been created successfully. A verification email has been sent to " + email,
                                "REGISTRATION"
                        );

                        Toast.makeText(CitizenRegistrationActivity.this,
                                "Account Created! Verification email sent to " + email, Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(CitizenRegistrationActivity.this, CitizenDashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        findViewById(R.id.btnCreateAccount).setEnabled(true);
                        Toast.makeText(CitizenRegistrationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Fallback for when Firebase is not configured (keeps existing local behavior) */
    private void createLocalAccount(String fullName, String email, String mobile, String password,
                                    String dob, String gender, String address, String pinCode) {
        User user = new User(null, fullName, email, mobile, password, dob, gender, address, pinCode, User.ROLE_CITIZEN);
        User registeredUser = userRepository.registerUser(user);
        sessionManager.createSession(registeredUser);

        notificationRepository.addNotification(
                registeredUser.getUserId(),
                "Welcome to Sahayak!",
                "Your citizen account has been created successfully.",
                "REGISTRATION"
        );

        Toast.makeText(this, "Account Created Successfully! Welcome " + fullName, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, CitizenDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilMobile.setError(null);
        tilEmail.setError(null);
        tilDateOfBirth.setError(null);
        tilGender.setError(null);
        tilAddress.setError(null);
        tilPinCode.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }
}
