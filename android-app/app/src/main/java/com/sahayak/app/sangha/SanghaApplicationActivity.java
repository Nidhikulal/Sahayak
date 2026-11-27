package com.sahayak.app.sangha;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.sahayak.app.data.FirebaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sangha Verification Application Form.
 * Collects all Sangha organization details, location details, login credentials (username/password),
 * and supporting documents, then emails everything to the admin for review.
 */
public class SanghaApplicationActivity extends AppCompatActivity {

    public static final String PREFS_APP = "sahayak_sangha_app_prefs";
    public static final String KEY_LAST_APP_ID = "last_application_id";
    private static final int MAX_DOCS = 4;

    private TextInputLayout tilOrgName, tilRegNo, tilContactPerson, tilEmail, tilMobile,
            tilStreetAddress, tilLocality, tilCity, tilDistrict, tilPincode, tilDescription,
            tilUsername, tilPassword, tilConfirmPassword;
    private TextInputEditText etOrgName, etRegNo, etContactPerson, etEmail, etMobile,
            etStreetAddress, etLocality, etCity, etDistrict, etPincode, etDescription,
            etUsername, etPassword, etConfirmPassword;

    private MaterialButton btnSelectLocationOnMap;
    private LinearLayout layoutAttachedDocs;
    private ProgressBar progressBar;
    private MaterialButton btnSubmitApplication;

    private final List<Map<String, Object>> attachedDocuments = new ArrayList<>();
    private FirebaseManager firebaseManager;
    private FusedLocationProviderClient fusedLocationClient;

    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private String selectedState = "";

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
                    if (attachedDocuments.size() >= MAX_DOCS) {
                        Toast.makeText(this, "Maximum " + MAX_DOCS + " documents allowed.", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    try {
                        Map<String, Object> docMap = DocumentAttachmentHelper.readUriAsDocumentMap(this, uri);
                        attachedDocuments.add(docMap);
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Could not attach file", Toast.LENGTH_LONG).show();
                    }
                }
                renderAttachedDocs();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_application);

        firebaseManager = FirebaseManager.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
    }

    private void initViews() {
        tilOrgName = findViewById(R.id.tilOrgName);
        tilRegNo = findViewById(R.id.tilRegNo);
        tilContactPerson = findViewById(R.id.tilContactPerson);
        tilEmail = findViewById(R.id.tilEmail);
        tilMobile = findViewById(R.id.tilMobile);

        btnSelectLocationOnMap = findViewById(R.id.btnSelectLocationOnMap);
        tilStreetAddress = findViewById(R.id.tilStreetAddress);
        tilLocality = findViewById(R.id.tilLocality);
        tilCity = findViewById(R.id.tilCity);
        tilDistrict = findViewById(R.id.tilDistrict);
        tilPincode = findViewById(R.id.tilPincode);

        tilDescription = findViewById(R.id.tilDescription);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etOrgName = findViewById(R.id.etOrgName);
        etRegNo = findViewById(R.id.etRegNo);
        etContactPerson = findViewById(R.id.etContactPerson);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);

        etStreetAddress = findViewById(R.id.etStreetAddress);
        etLocality = findViewById(R.id.etLocality);
        etCity = findViewById(R.id.etCity);
        etDistrict = findViewById(R.id.etDistrict);
        etPincode = findViewById(R.id.etPincode);

        etDescription = findViewById(R.id.etDescription);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        layoutAttachedDocs = findViewById(R.id.layoutAttachedDocs);
        progressBar = findViewById(R.id.progressBar);
        btnSubmitApplication = findViewById(R.id.btnSubmitApplication);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSelectLocationOnMap.setOnClickListener(v -> showMapPickerDialog());
        findViewById(R.id.btnUploadDocuments).setOnClickListener(v ->
                documentPickerLauncher.launch(new String[]{"application/pdf", "image/*"}));
        btnSubmitApplication.setOnClickListener(v -> attemptSubmit());
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
                btnSelectLocationOnMap.setText("Location Pinned: " + (!locality.isEmpty() ? locality : city) + " ✓");
                Toast.makeText(this, "Sangha Location confirmed!", Toast.LENGTH_SHORT).show();
            } else {
                btnSelectLocationOnMap.setText("Location Selected ✓");
                Toast.makeText(this, "Please enter or verify your Sangha address below.", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Location found & address populated!", Toast.LENGTH_SHORT).show();
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

        if (addr.hasLatitude()) selectedLatitude = addr.getLatitude();
        if (addr.hasLongitude()) selectedLongitude = addr.getLongitude();
        selectedState = addr.getAdminArea() != null ? addr.getAdminArea() : "";

        String street = addr.getThoroughfare() != null ? addr.getThoroughfare() :
                (addr.getFeatureName() != null ? addr.getFeatureName() : addr.getAddressLine(0));
        String loc = addr.getSubLocality() != null ? addr.getSubLocality() :
                (addr.getLocality() != null ? addr.getLocality() : "");
        String city = addr.getLocality() != null ? addr.getLocality() :
                (addr.getSubAdminArea() != null && !addr.getSubAdminArea().toLowerCase(Locale.ROOT).contains("division") ? addr.getSubAdminArea() : "");
        String dist = extractTrueDistrict(addr, city, loc);
        String pin = addr.getPostalCode() != null ? addr.getPostalCode() : "";

        if (street != null && !street.isEmpty()) etStreetAddress.setText(street);
        if (loc != null && !loc.isEmpty()) etLocality.setText(loc);
        if (city != null && !city.isEmpty()) etCity.setText(city);
        if (dist != null && !dist.isEmpty()) etDistrict.setText(dist);
        if (pin != null && !pin.isEmpty()) etPincode.setText(pin);

        String fullLine = (addr.getAddressLine(0) != null ? addr.getAddressLine(0) :
                ((street != null ? street + ", " : "") + (city != null ? city : "")))
                + (selectedLatitude != 0.0 ? String.format(Locale.US, " (%.4f° N, %.4f° E)", selectedLatitude, selectedLongitude) : "");

        if (tvPickedPin != null) {
            tvPickedPin.setText(fullLine);
        }
        btnSelectLocationOnMap.setText("Location Pinned: " + (city != null && !city.isEmpty() ? city : "Confirmed")
                + (selectedLatitude != 0.0 ? String.format(Locale.US, " (%.2f, %.2f)", selectedLatitude, selectedLongitude) : ""));
        Toast.makeText(this, "Location details & coordinates updated!", Toast.LENGTH_SHORT).show();
    }

    private void openGoogleMapsApp() {
        try {
            String city = textOf(etCity);
            String locality = textOf(etLocality);
            String query = !streetAddressQuery().isEmpty() ? streetAddressQuery() :
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

    private String streetAddressQuery() {
        String street = textOf(etStreetAddress);
        String locality = textOf(etLocality);
        String city = textOf(etCity);
        if (!street.isEmpty()) return street + ", " + locality + ", " + city;
        return "";
    }

    private void renderAttachedDocs() {
        layoutAttachedDocs.removeAllViews();
        for (int i = 0; i < attachedDocuments.size(); i++) {
            final int index = i;
            Map<String, Object> doc = attachedDocuments.get(i);
            String name = String.valueOf(doc.get("fileName"));
            long size = doc.get("sizeBytes") instanceof Number ? ((Number) doc.get("sizeBytes")).longValue() : 0L;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(24, 16, 16, 16);
            row.setBackgroundColor(ContextCompat.getColor(this, R.color.sahayak_surface_variant));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 12;
            row.setLayoutParams(lp);

            TextView tvInfo = new TextView(this);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvInfo.setText(name + " (" + DocumentAttachmentHelper.formatSize(size) + ")");
            tvInfo.setTextSize(12f);
            tvInfo.setTextColor(ContextCompat.getColor(this, R.color.sahayak_text_primary));

            TextView btnRemove = new TextView(this);
            btnRemove.setText("Remove");
            btnRemove.setTextSize(12f);
            btnRemove.setPadding(16, 8, 16, 8);
            btnRemove.setTextColor(ContextCompat.getColor(this, R.color.status_rejected));
            btnRemove.setOnClickListener(v -> {
                attachedDocuments.remove(index);
                renderAttachedDocs();
            });

            row.addView(tvInfo);
            row.addView(btnRemove);
            layoutAttachedDocs.addView(row);
        }
    }

    private void attemptSubmit() {
        clearErrors();

        String orgName = textOf(etOrgName);
        String regNo = textOf(etRegNo);
        String contactPerson = textOf(etContactPerson);
        String email = textOf(etEmail);
        String mobile = textOf(etMobile);

        String streetAddress = textOf(etStreetAddress);
        String locality = textOf(etLocality);
        String city = textOf(etCity);
        String district = textOf(etDistrict);
        String pincode = textOf(etPincode);

        String description = textOf(etDescription);
        String username = textOf(etUsername);
        String password = textOf(etPassword);
        String confirmPassword = textOf(etConfirmPassword);

        boolean valid = true;

        if (orgName.isEmpty()) { tilOrgName.setError("Organization name is required"); valid = false; }
        if (regNo.isEmpty()) { tilRegNo.setError("Registration number is required"); valid = false; }
        if (contactPerson.isEmpty()) { tilContactPerson.setError("Contact person is required"); valid = false; }
        if (email.isEmpty()) {
            tilEmail.setError("Official email is required"); valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address"); valid = false;
        }
        if (mobile.isEmpty()) {
            tilMobile.setError("Mobile number is required"); valid = false;
        } else if (!mobile.matches("^[0-9]{10}$")) {
            tilMobile.setError("Enter a valid 10-digit mobile number"); valid = false;
        }

        if (streetAddress.isEmpty()) { tilStreetAddress.setError("Place/Street/Landmark is required"); valid = false; }
        if (locality.isEmpty()) { tilLocality.setError("Locality/Area is required"); valid = false; }
        if (city.isEmpty()) { tilCity.setError("City is required"); valid = false; }
        if (district.isEmpty()) { tilDistrict.setError("District is required"); valid = false; }
        if (pincode.isEmpty()) {
            tilPincode.setError("Pincode is required"); valid = false;
        } else if (!pincode.matches("^[0-9]{6}$")) {
            tilPincode.setError("Enter a valid 6-digit Pincode"); valid = false;
        }

        if (description.isEmpty()) { tilDescription.setError("Description is required"); valid = false; }

        // Credentials validation
        if (username.isEmpty()) {
            tilUsername.setError("Username is required"); valid = false;
        } else if (username.length() < 4 || username.contains(" ")) {
            tilUsername.setError("Username: min 4 chars, no spaces"); valid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required"); valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters"); valid = false;
        }
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password"); valid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Passwords do not match"); valid = false;
        }

        if (attachedDocuments.isEmpty()) {
            Toast.makeText(this, "Please attach at least one supporting document.", Toast.LENGTH_LONG).show();
            valid = false;
        }

        if (!valid) return;

        String fullAddress = streetAddress + ", " + locality + ", " + city + ", " + district + " - " + pincode;

        setLoading(true);
        firebaseManager.submitSanghaApplication(
                this,
                orgName, regNo, contactPerson, email, mobile,
                district, locality,
                fullAddress, description,
                username, password,
                new ArrayList<>(attachedDocuments),
                new FirebaseManager.ApplicationSubmitCallback() {
                    @Override
                    public void onSuccess(String applicationId, String reviewToken, boolean emailSent, String emailMessage) {
                        setLoading(false);

                        getSharedPreferences(PREFS_APP, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_LAST_APP_ID, applicationId)
                                .apply();

                        String statusMsg = emailSent
                                ? "Admin (" + FirebaseManager.ADMIN_EMAIL + ") has been notified by email with Accept & Reject buttons."
                                : "Application saved! Note: Email server returned: " + emailMessage;

                        new AlertDialog.Builder(SanghaApplicationActivity.this)
                                .setTitle("Application Submitted!")
                                .setMessage("Your Application ID is:\n\n" + applicationId
                                        + "\n\nKeep this ID to track your application status.\n\n"
                                        + statusMsg)
                                .setCancelable(false)
                                .setNeutralButton("Copy ID", (d, w) -> {
                                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    if (cm != null) {
                                        cm.setPrimaryClip(ClipData.newPlainText("Application ID", applicationId));
                                    }
                                    Toast.makeText(SanghaApplicationActivity.this, "Copied " + applicationId, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .setPositiveButton("Done", (d, w) -> finish())
                                .show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(SanghaApplicationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private String textOf(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrors() {
        tilOrgName.setError(null);
        tilRegNo.setError(null);
        tilContactPerson.setError(null);
        tilEmail.setError(null);
        tilMobile.setError(null);
        if (tilStreetAddress != null) tilStreetAddress.setError(null);
        if (tilLocality != null) tilLocality.setError(null);
        if (tilCity != null) tilCity.setError(null);
        if (tilDistrict != null) tilDistrict.setError(null);
        if (tilPincode != null) tilPincode.setError(null);
        tilDescription.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitApplication.setEnabled(!loading);
    }
}
