package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.NotificationRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class ReportBeneficiaryActivity extends AppCompatActivity {

    private TextInputLayout tilBeneficiaryName, tilAge, tilGender, tilHouseAddress, tilLocality, tilCity, tilDistrict, tilPincode, tilCategory, tilDescription;
    private TextInputEditText etBeneficiaryName, etAge, etContactNumber, etFamilyDetails, etHouseAddress, etLocality, etCity, etDistrict, etPincode, etDescription;
    private AutoCompleteTextView actvGender, actvCategory;
    private CheckBox cbConsent;
    private TextView tvAttachedFilesList;

    private final List<String> attachedFiles = new ArrayList<>();
    private SessionManager sessionManager;
    private NotificationRepository notificationRepository;

    private Double selectedLat = 12.8756;
    private Double selectedLng = 74.8432;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_beneficiary);

        sessionManager = new SessionManager(this);
        notificationRepository = NotificationRepository.getInstance();

        initViews();
        setupDropdowns();
    }

    private void initViews() {
        tilBeneficiaryName = findViewById(R.id.tilBeneficiaryName);
        tilAge = findViewById(R.id.tilAge);
        tilGender = findViewById(R.id.tilGender);
        tilHouseAddress = findViewById(R.id.tilHouseAddress);
        tilLocality = findViewById(R.id.tilLocality);
        tilCity = findViewById(R.id.tilCity);
        tilDistrict = findViewById(R.id.tilDistrict);
        tilPincode = findViewById(R.id.tilPincode);
        tilCategory = findViewById(R.id.tilCategory);
        tilDescription = findViewById(R.id.tilDescription);

        etBeneficiaryName = findViewById(R.id.etBeneficiaryName);
        etAge = findViewById(R.id.etAge);
        etContactNumber = findViewById(R.id.etContactNumber);
        etFamilyDetails = findViewById(R.id.etFamilyDetails);
        etHouseAddress = findViewById(R.id.etHouseAddress);
        etLocality = findViewById(R.id.etLocality);
        etCity = findViewById(R.id.etCity);
        etDistrict = findViewById(R.id.etDistrict);
        etPincode = findViewById(R.id.etPincode);
        etDescription = findViewById(R.id.etDescription);

        actvGender = findViewById(R.id.actvGender);
        actvCategory = findViewById(R.id.actvCategory);
        cbConsent = findViewById(R.id.cbConsent);
        tvAttachedFilesList = findViewById(R.id.tvAttachedFilesList);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnSelectMapLocation).setOnClickListener(v -> showMapPickerDialog());

        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> {
            attachedFiles.add("Beneficiary_Photo_" + (attachedFiles.size() + 1) + ".jpg");
            updateAttachedFilesUI();
            Toast.makeText(this, "Photograph attached successfully", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnUploadDoc).setOnClickListener(v -> {
            attachedFiles.add("Medical_Certificate_" + (attachedFiles.size() + 1) + ".pdf");
            updateAttachedFilesUI();
            Toast.makeText(this, "Document attached successfully", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnReviewSubmit).setOnClickListener(v -> reviewAndSubmit());
    }

    private void setupDropdowns() {
        String[] genders = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        actvGender.setAdapter(genderAdapter);

        String[] categories = new String[]{
                "Medical Treatment",
                "Groceries and Food",
                "Education Fees",
                "Financial Assistance",
                "Support for Elderly People",
                "Support for Persons with Disabilities",
                "Other Emergency Needs"
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(categoryAdapter);
    }

    private void showMapPickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_map_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancelMap).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirmMap).setOnClickListener(v -> {
            etLocality.setText("Kadri");
            etCity.setText("Mangaluru");
            etDistrict.setText("Dakshina Kannada");
            etPincode.setText("575002");
            selectedLat = 12.8756;
            selectedLng = 74.8432;
            Toast.makeText(this, "Location selected from Map: Kadri, Mangaluru", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateAttachedFilesUI() {
        if (attachedFiles.isEmpty()) {
            tvAttachedFilesList.setText("No documents attached yet.");
        } else {
            StringBuilder sb = new StringBuilder("Attached Evidence Files:\n");
            for (String file : attachedFiles) {
                sb.append("• ").append(file).append("\n");
            }
            tvAttachedFilesList.setText(sb.toString().trim());
        }
    }

    private void reviewAndSubmit() {
        clearErrors();

        String bName = etBeneficiaryName.getText() != null ? etBeneficiaryName.getText().toString().trim() : "";
        String ageStr = etAge.getText() != null ? etAge.getText().toString().trim() : "";
        String gender = actvGender.getText().toString().trim();
        String house = etHouseAddress.getText() != null ? etHouseAddress.getText().toString().trim() : "";
        String locality = etLocality.getText() != null ? etLocality.getText().toString().trim() : "";
        String city = etCity.getText() != null ? etCity.getText().toString().trim() : "";
        String district = etDistrict.getText() != null ? etDistrict.getText().toString().trim() : "";
        String pincode = etPincode.getText() != null ? etPincode.getText().toString().trim() : "";
        String category = actvCategory.getText().toString().trim();
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        boolean isValid = true;

        if (bName.isEmpty()) {
            tilBeneficiaryName.setError("Beneficiary name is required");
            isValid = false;
        }

        if (ageStr.isEmpty()) {
            tilAge.setError("Age is required");
            isValid = false;
        }

        if (gender.isEmpty()) {
            tilGender.setError("Select gender");
            isValid = false;
        }

        if (house.isEmpty()) {
            tilHouseAddress.setError("House address is required");
            isValid = false;
        }

        if (locality.isEmpty()) {
            tilLocality.setError("Locality is required");
            isValid = false;
        }

        if (city.isEmpty()) {
            tilCity.setError("City is required");
            isValid = false;
        }

        if (district.isEmpty()) {
            tilDistrict.setError("District is required");
            isValid = false;
        }

        if (pincode.isEmpty() || pincode.length() < 6) {
            tilPincode.setError("Enter valid 6-digit pincode");
            isValid = false;
        }

        if (category.isEmpty()) {
            tilCategory.setError("Select assistance category");
            isValid = false;
        }

        if (description.isEmpty()) {
            tilDescription.setError("Provide situation description");
            isValid = false;
        }

        if (!cbConsent.isChecked()) {
            Toast.makeText(this, "Please confirm mandatory consent checkbox before submitting.", Toast.LENGTH_LONG).show();
            isValid = false;
        }

        if (!isValid) return;

        // Show Review Dialog
        String summary = "Beneficiary: " + bName + " (" + ageStr + " yrs, " + gender + ")\n" +
                "Category: " + category + "\n" +
                "Address: " + house + ", " + locality + ", " + city + " - " + pincode + "\n" +
                "Evidence Docs: " + (attachedFiles.isEmpty() ? "None" : attachedFiles.size() + " files");

        new AlertDialog.Builder(this)
                .setTitle("Review Request Information")
                .setMessage(summary)
                .setNegativeButton("Edit Details", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Confirm & Submit", (dialog, which) -> processSubmission(bName, Integer.parseInt(ageStr), gender, house, locality, city, district, pincode, category, description))
                .show();
    }

    private void processSubmission(String bName, int age, String gender, String house, String locality, String city, String district, String pincode, String category, String description) {
        User user = sessionManager.getCurrentUser();
        String cId = user != null ? user.getUserId() : "usr-cit-001";
        String cName = user != null ? user.getFullName() : "Citizen";

        AssistanceRequest r = new AssistanceRequest();
        r.citizenId = cId;
        r.citizenName = cName;
        r.beneficiaryName = bName;
        r.age = age;
        r.gender = gender;
        r.contactNumber = etContactNumber.getText() != null ? etContactNumber.getText().toString().trim() : "";
        r.familyDetails = etFamilyDetails.getText() != null ? etFamilyDetails.getText().toString().trim() : "";
        r.houseAddress = house;
        r.locality = locality;
        r.city = city;
        r.district = district;
        r.pincode = pincode;
        r.latitude = selectedLat;
        r.longitude = selectedLng;
        r.assistanceType = category;
        r.description = description;
        r.consentConfirmed = true;
        r.evidenceDocuments = new ArrayList<>(attachedFiles);
        r.targetAmount = 10000; // default for sponsorship
        r.raisedAmount = 0;

        AssistanceRequest submitted = MockRequestRepository.submit(r);

        // Send Notification
        notificationRepository.addNotification(
                cId,
                "Request Submitted: " + submitted.requestId,
                "Your request for " + bName + " (" + category + ") has been assigned to " + submitted.assignedSanghaName + " for verification.",
                "REQUEST_UPDATE"
        );

        // Success Dialog
        String successMsg = "Request ID: " + submitted.requestId + "\n" +
                "Status: " + RequestStatus.friendlyLabel(submitted.status) + "\n" +
                "Assigned Sangha: " + submitted.assignedSanghaName;

        new AlertDialog.Builder(this)
                .setTitle("Assistance Request Submitted!")
                .setMessage(successMsg)
                .setCancelable(false)
                .setPositiveButton("Track Request", (dialog, which) -> {
                    Intent intent = new Intent(ReportBeneficiaryActivity.this, RequestDetailsActivity.class);
                    intent.putExtra("REQUEST_ID", submitted.requestId);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Back to Dashboard", (dialog, which) -> finish())
                .show();
    }

    private void clearErrors() {
        tilBeneficiaryName.setError(null);
        tilAge.setError(null);
        tilGender.setError(null);
        tilHouseAddress.setError(null);
        tilLocality.setError(null);
        tilCity.setError(null);
        tilDistrict.setError(null);
        tilPincode.setError(null);
        tilCategory.setError(null);
        tilDescription.setError(null);
    }
}
