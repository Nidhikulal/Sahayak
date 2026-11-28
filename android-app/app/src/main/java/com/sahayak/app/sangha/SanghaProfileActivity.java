package com.sahayak.app.sangha;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.User;
import com.sahayak.app.sangha.data.SanghaDataRepository;
import com.sahayak.app.sangha.data.SanghaRepositoryInterface;

import java.util.HashMap;
import java.util.Map;

public class SanghaProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private SanghaRepositoryInterface sanghaRepository;

    private String currentSanghaId = "SNG-001";
    private Sangha currentSangha;

    private LinearLayout layoutViewMode, layoutEditMode;
    private ProgressBar progressBar;

    // View Mode TextViews
    private TextView tvHeaderOrgName, tvHeaderRegNo, tvHeaderStatus;
    private TextView tvDisplayOrgName, tvDisplayRegNo, tvDisplayOrgType, tvDisplayEstablishedYear,
            tvDisplayMobile, tvDisplayEmail, tvDisplayPresident, tvDisplaySecretary,
            tvDisplayAddress, tvDisplayDescription;

    // Edit Mode Inputs
    private TextInputLayout tilOrgName, tilRegNo, tilOrgType, tilEstablishedYear,
            tilMobile, tilEmail, tilPresident, tilSecretary, tilAddress, tilDescription;
    private TextInputEditText etOrgName, etRegNo, etEstablishedYear,
            etMobile, etEmail, etPresident, etSecretary, etAddress, etDescription;
    private AutoCompleteTextView actvOrgType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_profile);

        sessionManager = new SessionManager(this);
        sanghaRepository = SanghaDataRepository.getInstance();

        currentSanghaId = getIntent().getStringExtra("SANGHA_ID");
        if (currentSanghaId == null || currentSanghaId.trim().isEmpty()) {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser != null && currentUser.getSanghaId() != null && !currentUser.getSanghaId().trim().isEmpty()) {
                currentSanghaId = currentUser.getSanghaId();
            } else {
                currentSanghaId = "SNG-001";
            }
        }

        initViews();
        setupDropdowns();
        loadProfileData();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        layoutViewMode = findViewById(R.id.layoutViewMode);
        layoutEditMode = findViewById(R.id.layoutEditMode);
        progressBar = findViewById(R.id.progressBar);

        // Header
        tvHeaderOrgName = findViewById(R.id.tvHeaderOrgName);
        tvHeaderRegNo = findViewById(R.id.tvHeaderRegNo);
        tvHeaderStatus = findViewById(R.id.tvHeaderStatus);

        // View Mode TextViews
        tvDisplayOrgName = findViewById(R.id.tvDisplayOrgName);
        tvDisplayRegNo = findViewById(R.id.tvDisplayRegNo);
        tvDisplayOrgType = findViewById(R.id.tvDisplayOrgType);
        tvDisplayEstablishedYear = findViewById(R.id.tvDisplayEstablishedYear);
        tvDisplayMobile = findViewById(R.id.tvDisplayMobile);
        tvDisplayEmail = findViewById(R.id.tvDisplayEmail);
        tvDisplayPresident = findViewById(R.id.tvDisplayPresident);
        tvDisplaySecretary = findViewById(R.id.tvDisplaySecretary);
        tvDisplayAddress = findViewById(R.id.tvDisplayAddress);
        tvDisplayDescription = findViewById(R.id.tvDisplayDescription);

        // Edit Mode Inputs
        tilOrgName = findViewById(R.id.tilOrgName);
        tilRegNo = findViewById(R.id.tilRegNo);
        tilOrgType = findViewById(R.id.tilOrgType);
        tilEstablishedYear = findViewById(R.id.tilEstablishedYear);
        tilMobile = findViewById(R.id.tilMobile);
        tilEmail = findViewById(R.id.tilEmail);
        tilPresident = findViewById(R.id.tilPresident);
        tilSecretary = findViewById(R.id.tilSecretary);
        tilAddress = findViewById(R.id.tilAddress);
        tilDescription = findViewById(R.id.tilDescription);

        etOrgName = findViewById(R.id.etOrgName);
        etRegNo = findViewById(R.id.etRegNo);
        actvOrgType = findViewById(R.id.actvOrgType);
        etEstablishedYear = findViewById(R.id.etEstablishedYear);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etPresident = findViewById(R.id.etPresident);
        etSecretary = findViewById(R.id.etSecretary);
        etAddress = findViewById(R.id.etAddress);
        etDescription = findViewById(R.id.etDescription);

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> enterEditMode());
        findViewById(R.id.btnCancelEdit).setOnClickListener(v -> exitEditMode());
        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> saveProfileChanges());
    }

    private void setupDropdowns() {
        String[] types = {"Trust / NGO", "Registered NGO", "Public Charitable Trust", "Registered Society", "Charitable Foundation"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        actvOrgType.setAdapter(adapter);
    }

    private void loadProfileData() {
        currentSangha = sanghaRepository.getSanghaProfile(currentSanghaId);
        if (currentSangha == null) {
            User user = sessionManager.getCurrentUser();
            String name = user != null ? user.getFullName() : "Seva Bharathi Welfare Trust";
            String email = user != null ? user.getEmail() : "sevabharathi@gmail.com";
            String phone = user != null ? user.getMobile() : "+91 9876543210";
            currentSangha = new Sangha(currentSanghaId, name, "REG-SNG-2021-042", "Kadri", "Mangaluru", "Dakshina Kannada",
                    "Kadri Temple Road, Mangaluru, Dakshina Kannada - 575002", "Social welfare and community support",
                    "Dedicated to social support, health drives, education support and emergency relief across Karnataka.", phone, email, "APPROVED");
        }

        // Try loading live profile from Firebase Firestore
        fetchProfileFromFirebase();
    }

    private void fetchProfileFromFirebase() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("sanghas")
                .whereEqualTo("sanghaId", currentSanghaId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (!query.isEmpty()) {
                        Map<String, Object> data = query.getDocuments().get(0).getData();
                        if (data != null) {
                            applyFirestoreMapToSangha(data);
                        }
                    }
                    populateViewModeData();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    populateViewModeData();
                });
    }

    private void applyFirestoreMapToSangha(Map<String, Object> data) {
        if (data.containsKey("name")) currentSangha.setName(str(data.get("name")));
        if (data.containsKey("organizationName")) currentSangha.setName(str(data.get("organizationName")));
        if (data.containsKey("registrationNo")) currentSangha.setRegistrationNo(str(data.get("registrationNo")));
        if (data.containsKey("organizationType")) currentSangha.setOrganizationType(str(data.get("organizationType")));
        if (data.containsKey("establishedYear")) currentSangha.setEstablishedYear(str(data.get("establishedYear")));
        if (data.containsKey("mobile")) currentSangha.setContactPhone(str(data.get("mobile")));
        if (data.containsKey("contactPhone")) currentSangha.setContactPhone(str(data.get("contactPhone")));
        if (data.containsKey("email")) currentSangha.setContactEmail(str(data.get("email")));
        if (data.containsKey("contactEmail")) currentSangha.setContactEmail(str(data.get("contactEmail")));
        if (data.containsKey("presidentName")) currentSangha.setPresidentName(str(data.get("presidentName")));
        if (data.containsKey("secretaryName")) currentSangha.setSecretaryName(str(data.get("secretaryName")));
        if (data.containsKey("fullAddress")) currentSangha.setFullAddress(str(data.get("fullAddress")));
        if (data.containsKey("address")) currentSangha.setFullAddress(str(data.get("address")));
        if (data.containsKey("fullDescription")) currentSangha.setFullDescription(str(data.get("fullDescription")));
        if (data.containsKey("description")) currentSangha.setFullDescription(str(data.get("description")));
    }

    private String str(Object obj) {
        return obj instanceof String ? (String) obj : (obj != null ? obj.toString() : "");
    }

    private void populateViewModeData() {
        tvHeaderOrgName.setText(currentSangha.getName());
        tvHeaderRegNo.setText(currentSangha.getRegistrationNo());
        tvHeaderStatus.setText("VERIFIED NGO PARTNER ✓");

        tvDisplayOrgName.setText(currentSangha.getName());
        tvDisplayRegNo.setText(currentSangha.getRegistrationNo());
        tvDisplayOrgType.setText(currentSangha.getOrganizationType());
        tvDisplayEstablishedYear.setText(currentSangha.getEstablishedYear());
        tvDisplayMobile.setText(currentSangha.getContactPhone());
        tvDisplayEmail.setText(currentSangha.getContactEmail());
        tvDisplayPresident.setText(currentSangha.getPresidentName());
        tvDisplaySecretary.setText(currentSangha.getSecretaryName());
        tvDisplayAddress.setText(currentSangha.getFullAddress());
        tvDisplayDescription.setText(currentSangha.getFullDescription());
    }

    private void enterEditMode() {
        clearErrors();

        etOrgName.setText(currentSangha.getName());
        etRegNo.setText(currentSangha.getRegistrationNo());
        actvOrgType.setText(currentSangha.getOrganizationType(), false);
        etEstablishedYear.setText(currentSangha.getEstablishedYear());
        etMobile.setText(currentSangha.getContactPhone());
        etEmail.setText(currentSangha.getContactEmail());
        etPresident.setText(currentSangha.getPresidentName());
        etSecretary.setText(currentSangha.getSecretaryName());
        etAddress.setText(currentSangha.getFullAddress());
        etDescription.setText(currentSangha.getFullDescription());

        layoutViewMode.setVisibility(View.GONE);
        layoutEditMode.setVisibility(View.VISIBLE);
    }

    private void exitEditMode() {
        clearErrors();
        layoutEditMode.setVisibility(View.GONE);
        layoutViewMode.setVisibility(View.VISIBLE);
    }

    private void saveProfileChanges() {
        clearErrors();

        String name = textOf(etOrgName);
        String regNo = textOf(etRegNo);
        String orgType = actvOrgType.getText().toString().trim();
        String year = textOf(etEstablishedYear);
        String mobile = textOf(etMobile);
        String email = textOf(etEmail);
        String president = textOf(etPresident);
        String secretary = textOf(etSecretary);
        String address = textOf(etAddress);
        String desc = textOf(etDescription);

        boolean isValid = true;

        if (name.isEmpty()) {
            tilOrgName.setError("Organization name is required");
            isValid = false;
        }

        if (regNo.isEmpty()) {
            tilRegNo.setError("Registration number is required");
            isValid = false;
        }

        if (orgType.isEmpty()) {
            tilOrgType.setError("Select organization type");
            isValid = false;
        }

        if (year.isEmpty()) {
            tilEstablishedYear.setError("Established year required");
            isValid = false;
        } else if (!year.matches("^[0-9]{4}$")) {
            tilEstablishedYear.setError("Enter valid 4-digit year");
            isValid = false;
        }

        if (mobile.isEmpty()) {
            tilMobile.setError("Mobile number is required");
            isValid = false;
        } else if (!mobile.matches("^[0-9]{10}$")) {
            tilMobile.setError("Enter valid 10-digit mobile number");
            isValid = false;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Email address is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            isValid = false;
        }

        if (president.isEmpty()) {
            tilPresident.setError("President name is required");
            isValid = false;
        }

        if (secretary.isEmpty()) {
            tilSecretary.setError("Secretary name is required");
            isValid = false;
        }

        if (address.isEmpty()) {
            tilAddress.setError("Organization address is required");
            isValid = false;
        }

        if (desc.isEmpty()) {
            tilDescription.setError("Organization description is required");
            isValid = false;
        }

        if (!isValid) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Update local object
        currentSangha.setName(name);
        currentSangha.setRegistrationNo(regNo);
        currentSangha.setOrganizationType(orgType);
        currentSangha.setEstablishedYear(year);
        currentSangha.setContactPhone(mobile);
        currentSangha.setContactEmail(email);
        currentSangha.setPresidentName(president);
        currentSangha.setSecretaryName(secretary);
        currentSangha.setFullAddress(address);
        currentSangha.setFullDescription(desc);

        // Update local repository & user session
        sanghaRepository.updateSanghaProfile(currentSangha);

        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            currentUser.setFullName(name);
            currentUser.setRegistrationNo(regNo);
            currentUser.setMobile(mobile);
            currentUser.setEmail(email);
            currentUser.setAddress(address);
            sessionManager.createSession(currentUser);
        }

        // Prepare updates for Firebase Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("sanghaId", currentSanghaId);
        updates.put("name", name);
        updates.put("organizationName", name);
        updates.put("registrationNo", regNo);
        updates.put("organizationType", orgType);
        updates.put("establishedYear", year);
        updates.put("mobile", mobile);
        updates.put("contactPhone", mobile);
        updates.put("email", email);
        updates.put("contactEmail", email);
        updates.put("presidentName", president);
        updates.put("secretaryName", secretary);
        updates.put("fullAddress", address);
        updates.put("address", address);
        updates.put("fullDescription", desc);
        updates.put("description", desc);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("sanghas")
                .whereEqualTo("sanghaId", currentSanghaId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (!query.isEmpty()) {
                        query.getDocuments().get(0).getReference().update(updates);
                    } else {
                        FirebaseFirestore.getInstance().collection("sanghas")
                                .document(currentSanghaId)
                                .set(updates, com.google.firebase.firestore.SetOptions.merge());
                    }
                    populateViewModeData();
                    exitEditMode();
                    Toast.makeText(SanghaProfileActivity.this, "Organization Profile updated successfully!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    populateViewModeData();
                    exitEditMode();
                    Toast.makeText(SanghaProfileActivity.this, "Profile updated locally! (Firebase sync pending)", Toast.LENGTH_SHORT).show();
                });
    }

    private String textOf(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrors() {
        tilOrgName.setError(null);
        tilRegNo.setError(null);
        tilOrgType.setError(null);
        tilEstablishedYear.setError(null);
        tilMobile.setError(null);
        tilEmail.setError(null);
        tilPresident.setError(null);
        tilSecretary.setError(null);
        tilAddress.setError(null);
        tilDescription.setError(null);
    }
}
