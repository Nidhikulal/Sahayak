package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.HomeActivity;
import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.User;

public class CitizenProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private User currentUser;

    private TextView tvProfileName, tvProfileRole, tvProfileEmail, tvProfileMobile, tvProfileAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_profile);

        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getCurrentUser();

        initViews();
        loadProfileData();
    }

    private void initViews() {
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileMobile = findViewById(R.id.tvProfileMobile);
        tvProfileAddress = findViewById(R.id.tvProfileAddress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());

        findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePasswordDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> logoutUser());
    }

    private void loadProfileData() {
        if (currentUser == null) return;

        tvProfileName.setText(currentUser.getFullName());
        tvProfileRole.setText(currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "CITIZEN");
        tvProfileEmail.setText("Email: " + currentUser.getEmail());
        tvProfileMobile.setText("Mobile: " + currentUser.getMobile());
        tvProfileAddress.setText("Address: " + currentUser.getAddress());
    }

    private void showEditProfileDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Full Name");
        etName.setText(currentUser.getFullName());
        layout.addView(etName);

        final EditText etMobile = new EditText(this);
        etMobile.setHint("Mobile Number");
        etMobile.setInputType(InputType.TYPE_CLASS_PHONE);
        etMobile.setText(currentUser.getMobile());
        layout.addView(etMobile);

        final EditText etAddress = new EditText(this);
        etAddress.setHint("Residential Address");
        etAddress.setText(currentUser.getAddress());
        layout.addView(etAddress);

        new AlertDialog.Builder(this)
                .setTitle("Update Personal Details")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newMobile = etMobile.getText().toString().trim();
                    String newAddress = etAddress.getText().toString().trim();

                    if (!newName.isEmpty() && !newMobile.isEmpty() && !newAddress.isEmpty()) {
                        sessionManager.updateUserProfile(newName, newMobile, newAddress);
                        currentUser = sessionManager.getCurrentUser();
                        loadProfileData();
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etCurrent = new EditText(this);
        etCurrent.setHint("Current Password");
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrent);

        final EditText etNew = new EditText(this);
        etNew.setHint("New Password");
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNew);

        final EditText etConfirm = new EditText(this);
        etConfirm.setHint("Confirm New Password");
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirm);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", (dialog, which) -> {
                    String pass = etNew.getText().toString().trim();
                    String conf = etConfirm.getText().toString().trim();
                    if (!pass.isEmpty() && pass.equals(conf)) {
                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Passwords do not match or empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutUser() {
        com.sahayak.app.data.FirebaseManager.getInstance().signOut();
        sessionManager.clearSession();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
