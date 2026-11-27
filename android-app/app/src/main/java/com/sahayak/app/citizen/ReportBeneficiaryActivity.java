package com.sahayak.app.citizen;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sahayak.app.R;
import com.sahayak.app.data.NotificationRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportBeneficiaryActivity extends AppCompatActivity {

    private TextInputLayout tilBeneficiaryName, tilAge, tilGender, tilContactNumber, tilFamilyDetails,
            tilHouseAddress, tilLocality, tilCity, tilDistrict, tilPincode, tilCategory, tilDescription;
    private TextInputEditText etBeneficiaryName, etAge, etContactNumber, etFamilyDetails,
            etHouseAddress, etLocality, etCity, etDistrict, etPincode, etDescription;
    private AutoCompleteTextView actvGender, actvCategory;
    private CheckBox cbConsent;
    private TextView tvAttachedFilesList;
    private MaterialButton btnSelectMapLocation;

    private final List<String> attachedFiles = new ArrayList<>();
    private SessionManager sessionManager;
    private NotificationRepository notificationRepository;
    private FusedLocationProviderClient fusedLocationClient;

    private Double selectedLat = 12.8756;
    private Double selectedLng = 74.8432;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fetchCurrentGpsLocation(null);
                } else {
                    Toast.makeText(this, "Location permission is required for auto-detecting GPS location.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> documentPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) return;
                for (Uri uri : uris) {
                    try {
                        String fileName = null;
                        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                if (idx >= 0) fileName = cursor.getString(idx);
                            }
                        } catch (Exception ignored) {}
                        if (fileName == null || fileName.isEmpty()) {
                            fileName = uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "Evidence_File";
                        }
                        if (!attachedFiles.contains(fileName)) {
                            attachedFiles.add(fileName);
                        }
                    } catch (Exception e) {
                        attachedFiles.add("Evidence_File_" + (attachedFiles.size() + 1));
                    }
                }
                updateAttachedFilesUI();
                Toast.makeText(this, uris.size() + " evidence file(s) attached from phone", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_beneficiary);

        sessionManager = new SessionManager(this);
        notificationRepository = NotificationRepository.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupDropdowns();
    }

    private void initViews() {
        tilBeneficiaryName = findViewById(R.id.tilBeneficiaryName);
        tilAge = findViewById(R.id.tilAge);
        tilGender = findViewById(R.id.tilGender);
        tilContactNumber = findViewById(R.id.tilContactNumber);
        tilFamilyDetails = findViewById(R.id.tilFamilyDetails);
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
        btnSelectMapLocation = findViewById(R.id.btnSelectMapLocation);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSelectMapLocation.setOnClickListener(v -> showMapPickerDialog());

        View.OnClickListener uploadListener = v ->
                documentPickerLauncher.launch(new String[]{"image/*", "video/*", "application/pdf"});

        findViewById(R.id.btnUploadPhoto).setOnClickListener(uploadListener);
        findViewById(R.id.btnUploadDoc).setOnClickListener(uploadListener);

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
                .setCancelable(true)
                .create();

        TextView tvPickedPin = dialogView.findViewById(R.id.tvPickedPin);
        EditText etMapSearch = dialogView.findViewById(R.id.etMapSearch);
        EditText etPasteLink = dialogView.findViewById(R.id.etPasteLink);

        TextView tvTitle = dialogView.findViewById(R.id.tvMapPickerTitle);
        if (tvTitle != null) {
            tvTitle.setText("Select Beneficiary Location on Map");
        }

        dialogView.findViewById(R.id.btnSearchMapPlace).setOnClickListener(v -> {
            String query = etMapSearch != null ? etMapSearch.getText().toString().trim() : "";
            if (query.isEmpty()) {
                Toast.makeText(this, "Enter an area, city, or landmark to search", Toast.LENGTH_SHORT).show();
                return;
            }
            searchLocationByName(query, tvPickedPin);
        });

        if (etPasteLink != null) {
            etPasteLink.setText("");
            etPasteLink.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s != null && s.length() > 3) {
                        parseAndSetPastedLocation(s.toString().trim(), tvPickedPin);
                    }
                }
            });
        }

        dialogView.findViewById(R.id.btnDetectCurrentLocation).setOnClickListener(v ->
                fetchCurrentGpsLocation(tvPickedPin));

        dialogView.findViewById(R.id.btnOpenGoogleMapsApp).setOnClickListener(v ->
                openGoogleMapsApp());

        dialogView.findViewById(R.id.btnCancelMap).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnConfirmMap).setOnClickListener(v -> {
            dialog.dismiss();
            String city = textOf(etCity);
            String locality = textOf(etLocality);
            if (!city.isEmpty() || !locality.isEmpty()) {
                btnSelectMapLocation.setText("Location Pinned: " + (!locality.isEmpty() ? locality : city) + " ✓");
                Toast.makeText(this, "Beneficiary Location confirmed!", Toast.LENGTH_SHORT).show();
            } else {
                btnSelectMapLocation.setText("Location Selected ✓");
                Toast.makeText(this, "Please enter or verify the beneficiary address below.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void parseAndSetPastedLocation(String raw, TextView tvPickedPin) {
        if (raw == null || raw.trim().isEmpty()) return;

        Matcher matcher = Pattern.compile("(-?\\d+\\.\\d+)[\\s,]+(-?\\d+\\.\\d+)").matcher(raw);
        if (matcher.find() && matcher.group(1) != null && matcher.group(2) != null) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                resolveAndSetAddressFromCoords(lat, lng, tvPickedPin);
                return;
            } catch (Exception ignored) {}
        }

        String cleanQuery = raw;
        if (cleanQuery.contains("http://") || cleanQuery.contains("https://")) {
            cleanQuery = cleanQuery.replaceAll("https?://\\S+", "").trim();
        }
        if (!cleanQuery.isEmpty()) {
            searchLocationByName(cleanQuery, tvPickedPin);
        }
    }

    private void searchLocationByName(String query, TextView tvPickedPin) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(query, 5);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                populateAddressFieldsFromAddress(addr, tvPickedPin);
                Toast.makeText(this, "Location found & address & pincode populated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not resolve address for \"" + query + "\". Try adding city name.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCurrentGpsLocation(TextView tvPickedPin) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        Toast.makeText(this, "Fetching high-accuracy GPS coordinates...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        resolveAndSetAddressFromCoords(lat, lng, tvPickedPin);
                    } else {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                resolveAndSetAddressFromCoords(lastLoc.getLatitude(), lastLoc.getLongitude(), tvPickedPin);
                            } else {
                                Toast.makeText(this, "GPS location unavailable. Please ensure location services are enabled or search manually.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not get GPS location: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void resolveAndSetAddressFromCoords(double lat, double lng, TextView tvPickedPin) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                populateAddressFieldsFromAddress(addr, tvPickedPin);
            } else {
                String summary = String.format(Locale.US, "GPS: %.4f N, %.4f E", lat, lng);
                if (tvPickedPin != null) tvPickedPin.setText(summary);
            }
        } catch (Exception e) {
            String coordsStr = String.format(Locale.US, "%.4f N, %.4f E", lat, lng);
            if (tvPickedPin != null) tvPickedPin.setText("Pinned GPS: " + coordsStr);
            Toast.makeText(this, "GPS Coords: " + coordsStr, Toast.LENGTH_SHORT).show();
        }
    }

    private String extractTrueDistrict(Address addr, String city, String locality) {
        String subAdmin = addr.getSubAdminArea() != null ? addr.getSubAdminArea().trim() : "";
        String fullAddressLine = addr.getAddressLine(0) != null ? addr.getAddressLine(0) : "";

        if (subAdmin.isEmpty() || subAdmin.toLowerCase(Locale.ROOT).contains("division")) {
            String combined = (fullAddressLine + " " + city + " " + locality).toLowerCase(Locale.ROOT);
            if (combined.contains("mangalore") || combined.contains("mangaluru") || combined.contains("dakshina kannada")
                    || combined.contains("mulki") || combined.contains("thokur") || combined.contains("pakshikere")
                    || combined.contains("surathkal") || combined.contains("bantwal") || combined.contains("puttur")
                    || combined.contains("belthangady") || combined.contains("kadri") || combined.contains("moodabidri")) {
                return "Dakshina Kannada";
            }
            if (combined.contains("udupi") || combined.contains("manipal") || combined.contains("kundapura")
                    || combined.contains("karkala") || combined.contains("kaup")) {
                return "Udupi";
            }
            if (combined.contains("mysore") || combined.contains("mysuru")) {
                return "Mysuru";
            }
            if (combined.contains("bangalore") || combined.contains("bengaluru")) {
                return "Bengaluru Urban";
            }
            if (combined.contains("uttara kannada") || combined.contains("karwar")) {
                return "Uttara Kannada";
            }
            if (combined.contains("shimoga") || combined.contains("shivamogga")) {
                return "Shivamogga";
            }
            String admin = addr.getAdminArea() != null ? addr.getAdminArea().trim() : "";
            if (!admin.toLowerCase(Locale.ROOT).contains("division") && !admin.isEmpty()) {
                return admin;
            }
            return !city.isEmpty() ? city : "Dakshina Kannada";
        }
        return subAdmin;
    }

    private void populateAddressFieldsFromAddress(Address addr, TextView tvPickedPin) {
        if (addr == null) return;

        if (addr.hasLatitude()) selectedLat = addr.getLatitude();
        if (addr.hasLongitude()) selectedLng = addr.getLongitude();

        String street = addr.getThoroughfare() != null ? addr.getThoroughfare() :
                (addr.getFeatureName() != null ? addr.getFeatureName() : addr.getAddressLine(0));
        String loc = addr.getSubLocality() != null ? addr.getSubLocality() :
                (addr.getLocality() != null ? addr.getLocality() : "");
        String city = addr.getLocality() != null ? addr.getLocality() :
                (addr.getSubAdminArea() != null && !addr.getSubAdminArea().toLowerCase(Locale.ROOT).contains("division") ? addr.getSubAdminArea() : "");
        String dist = extractTrueDistrict(addr, city, loc);
        String pin = addr.getPostalCode() != null ? addr.getPostalCode() : "";

        if (street != null && !street.isEmpty()) etHouseAddress.setText(street);
        if (loc != null && !loc.isEmpty()) etLocality.setText(loc);
        if (city != null && !city.isEmpty()) etCity.setText(city);
        if (dist != null && !dist.isEmpty()) etDistrict.setText(dist);
        if (pin != null && !pin.isEmpty()) etPincode.setText(pin);

        String fullLine = (addr.getAddressLine(0) != null ? addr.getAddressLine(0) :
                ((street != null ? street + ", " : "") + (city != null ? city : "")))
                + (selectedLat != 0.0 ? String.format(Locale.US, " (%.4f° N, %.4f° E)", selectedLat, selectedLng) : "");

        if (tvPickedPin != null) {
            tvPickedPin.setText(fullLine);
        }
        if (btnSelectMapLocation != null) {
            btnSelectMapLocation.setText("Location Pinned: " + (city != null && !city.isEmpty() ? city : "Confirmed")
                    + (selectedLat != 0.0 ? String.format(Locale.US, " (%.2f, %.2f)", selectedLat, selectedLng) : ""));
        }
        Toast.makeText(this, "Location details & Pincode populated successfully!", Toast.LENGTH_SHORT).show();
    }

    private void openGoogleMapsApp() {
        try {
            String city = textOf(etCity);
            String locality = textOf(etLocality);
            String query = !houseAddressQuery().isEmpty() ? houseAddressQuery() :
                    (!locality.isEmpty() ? locality + ", " + city : (!city.isEmpty() ? city : "Karnataka"));

            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)));
                startActivity(webIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not launch Google Maps.", Toast.LENGTH_SHORT).show();
        }
    }

    private String houseAddressQuery() {
        String house = textOf(etHouseAddress);
        String locality = textOf(etLocality);
        String city = textOf(etCity);
        if (!house.isEmpty()) return house + ", " + locality + ", " + city;
        return "";
    }

    private String textOf(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void updateAttachedFilesUI() {
        if (attachedFiles.isEmpty()) {
            tvAttachedFilesList.setText("No evidence attached yet.");
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

        String bName = textOf(etBeneficiaryName);
        String ageStr = textOf(etAge);
        String gender = actvGender.getText().toString().trim();
        String contactNo = textOf(etContactNumber);
        String family = textOf(etFamilyDetails);
        String house = textOf(etHouseAddress);
        String locality = textOf(etLocality);
        String city = textOf(etCity);
        String district = textOf(etDistrict);
        String pincode = textOf(etPincode);
        String category = actvCategory.getText().toString().trim();
        String description = textOf(etDescription);

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

        if (contactNo.isEmpty()) {
            tilContactNumber.setError("Contact number is required");
            isValid = false;
        } else if (!contactNo.matches("^[0-9]{10}$")) {
            tilContactNumber.setError("Enter a valid 10-digit mobile number");
            isValid = false;
        }

        if (family.isEmpty()) {
            tilFamilyDetails.setError("Family details are required");
            isValid = false;
        }

        if (house.isEmpty()) {
            tilHouseAddress.setError("Place/Street/Landmark is required");
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

        if (pincode.isEmpty()) {
            tilPincode.setError("Pincode is required");
            isValid = false;
        } else if (!pincode.matches("^[0-9]{6}$")) {
            tilPincode.setError("Enter a valid 6-digit Pincode");
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
                "Contact: " + contactNo + "\n" +
                "Category: " + category + "\n" +
                "Address: " + house + ", " + locality + ", " + city + ", " + district + " - " + pincode + "\n" +
                "GPS Coordinates: " + String.format(Locale.US, "%.4f N, %.4f E", selectedLat, selectedLng) + "\n" +
                "Evidence Files: " + (attachedFiles.isEmpty() ? "None" : attachedFiles.size() + " files");

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
        r.contactNumber = textOf(etContactNumber);
        r.familyDetails = textOf(etFamilyDetails);
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
        r.targetAmount = 10000;
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
        tilContactNumber.setError(null);
        tilFamilyDetails.setError(null);
        tilHouseAddress.setError(null);
        tilLocality.setError(null);
        tilCity.setError(null);
        tilDistrict.setError(null);
        tilPincode.setError(null);
        tilCategory.setError(null);
        tilDescription.setError(null);
    }
}
