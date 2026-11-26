package com.sahayak.app.data;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Central manager for all Firebase Auth and Firestore operations related to Sangha accounts.
 *
 * <p>Uses 100% FREE built-in Firebase Authentication email service (no paid Blaze plan or extensions required).
 *
 * <p><b>Firestore structure used (collection "sanghas"):</b>
 * <pre>
 * sanghas/
 *   {docId}/
 *     sanghaId       : String   (e.g. "SNG-001")
 *     name           : String   (e.g. "Seva Bharathi Trust")
 *     registrationNo : String   (e.g. "REG-SNG-2021-042")
 *     email          : String   (e.g. "24g50.rikisha@sjec.ac.in")
 *     username       : String   (e.g. "seva_bharathi")
 *     district       : String
 *     locality       : String
 *     city           : String
 *     Address/address: String
 *     description    : String
 *     contactPerson  : String
 *     mobile         : String
 *     status         : String   ("APPROVED" / "PENDING" / "REJECTED")
 *     invitationSent : Boolean  (true once invitation email has been triggered)
 *     passwordSet    : Boolean  (true once Sangha sets their password)
 * </pre>
 */
public class FirebaseManager {

    private static FirebaseManager instance;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private final boolean firebaseReady;

    public static final String COLLECTION_SANGHAS = "sanghas";
    public static final String COLLECTION_SANGHA_APPLICATIONS = "sangha_applications";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_INFO_REQUESTED = "INFO_REQUESTED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String ADMIN_EMAIL = "sahayakadmin65@gmail.com";
    public static final long REGISTRATION_TOKEN_TTL_MS = 7L * 24L * 60L * 60L * 1000L; // 7 days

    private FirebaseManager() {
        boolean ready = false;
        try {
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            ready = true;
        } catch (IllegalStateException ignored) {
            // google-services.json not configured yet
        }
        this.firebaseReady = ready;
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public boolean isFirebaseReady() {
        return firebaseReady;
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseReady ? auth.getCurrentUser() : null;
    }

    public void signOut() {
        if (firebaseReady) {
            auth.signOut();
        }
    }

    /**
     * Deterministic temp password used when provisioning an uninvited account.
     */
    private String getTempPassword(String regNo) {
        String clean = (regNo != null && !regNo.isEmpty()) ? regNo.replaceAll("[^a-zA-Z0-9]", "") : "2026";
        return "Sahayak@" + clean;
    }

    // ─────────────────────────────────────────────
    //  Registration (Username + Password)
    // ─────────────────────────────────────────────

    public void registerWithUsername(String username, String password, RegisterCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected yet. Add google-services.json first.");
            return;
        }

        db.collection(COLLECTION_SANGHAS)
                .whereEqualTo("username", username.trim())
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        callback.onFailure("Username not recognised. Please verify the username entered.");
                        return;
                    }

                    DocumentSnapshot doc = query.getDocuments().get(0);
                    String email = doc.getString("email");
                    String rawId = doc.getString("sanghaId");
                    String sanghaId = (rawId != null && !rawId.isEmpty()) ? rawId : doc.getId();
                    String docId = doc.getId();
                    String regNo = doc.getString("registrationNo");

                    if (email == null || email.isEmpty()) {
                        callback.onFailure("Sangha email not configured in database. Contact admin.");
                        return;
                    }

                    // Attempt to create new user with chosen password
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                Map<String, Object> updates = new HashMap<>();
                                if (authResult.getUser() != null) {
                                    updates.put("authUid", authResult.getUser().getUid());
                                    // Send 100% FREE email verification to the registered email address
                                    authResult.getUser().sendEmailVerification();
                                }
                                updates.put("passwordSet", true);
                                db.collection(COLLECTION_SANGHAS).document(docId).update(updates);

                                callback.onSuccess(doc.getData(), sanghaId, email);
                            })
                            .addOnFailureListener(e -> {
                                // If the account was already provisioned during invitation, sign in with temp password and update
                                String tempPassword = getTempPassword(regNo);
                                auth.signInWithEmailAndPassword(email, tempPassword)
                                        .addOnSuccessListener(signInResult -> {
                                            FirebaseUser user = signInResult.getUser();
                                            if (user != null) {
                                                user.updatePassword(password)
                                                        .addOnSuccessListener(u -> {
                                                            // Send free email verification
                                                            user.sendEmailVerification();
                                                            Map<String, Object> updates = new HashMap<>();
                                                            updates.put("authUid", user.getUid());
                                                            updates.put("passwordSet", true);
                                                            db.collection(COLLECTION_SANGHAS).document(docId).update(updates);

                                                            callback.onSuccess(doc.getData(), sanghaId, email);
                                                        })
                                                        .addOnFailureListener(err -> callback.onFailure("Could not set password: " + err.getMessage()));
                                            } else {
                                                callback.onFailure("Registration failed. Please contact admin.");
                                            }
                                        })
                                        .addOnFailureListener(err -> {
                                            callback.onFailure("This Sangha is already registered. Please sign in, or use 'Forgot Password' to reset.");
                                        });
                            });
                })
                .addOnFailureListener(e -> callback.onFailure("Server error: " + (e.getMessage() != null ? e.getMessage() : "Check internet connection.")));
    }

    // ─────────────────────────────────────────────
    //  Login (Username + Password)
    // ─────────────────────────────────────────────

    public void loginWithUsername(String username, String password, LoginCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected yet. Add google-services.json first.");
            return;
        }

        db.collection(COLLECTION_SANGHAS)
                .whereEqualTo("username", username.trim())
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        callback.onFailure("Username not found. Please check your credentials.");
                        return;
                    }

                    DocumentSnapshot doc = query.getDocuments().get(0);
                    String email = doc.getString("email");
                    String rawId = doc.getString("sanghaId");
                    String sanghaId = (rawId != null && !rawId.isEmpty()) ? rawId : doc.getId();

                    if (email == null || email.isEmpty()) {
                        callback.onFailure("Account email error. Contact admin.");
                        return;
                    }

                    auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult ->
                                    callback.onSuccess(doc.getData(), sanghaId, email))
                            .addOnFailureListener(e -> {
                                String err = e.getMessage() != null ? e.getMessage() : "Login failed.";
                                if (err.contains("password is invalid") || err.contains("INVALID_LOGIN_CREDENTIALS") || err.contains("no user record")) {
                                    err = "Incorrect username or password.";
                                }
                                callback.onFailure(err);
                            });
                })
                .addOnFailureListener(e -> callback.onFailure("Server error: " + (e.getMessage() != null ? e.getMessage() : "Check internet connection.")));
    }

    // ─────────────────────────────────────────────
    //  Forgot Password (100% FREE Firebase Auth email)
    // ─────────────────────────────────────────────

    public void sendPasswordResetForUsername(String username, SimpleCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected yet. Add google-services.json first.");
            return;
        }

        db.collection(COLLECTION_SANGHAS)
                .whereEqualTo("username", username.trim())
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        callback.onFailure("Username not recognised.");
                        return;
                    }
                    DocumentSnapshot doc = query.getDocuments().get(0);
                    String email = doc.getString("email");
                    String regNo = doc.getString("registrationNo");
                    if (email == null || email.isEmpty()) {
                        callback.onFailure("No email associated with this username in the database.");
                        return;
                    }

                    // Try sending password reset email directly
                    auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused -> callback.onSuccess(email))
                            .addOnFailureListener(e -> {
                                // If the user was never created in Auth yet, provision with temp password first, then send
                                String tempPassword = getTempPassword(regNo);
                                auth.createUserWithEmailAndPassword(email, tempPassword)
                                        .addOnCompleteListener(task -> {
                                            auth.sendPasswordResetEmail(email)
                                                    .addOnSuccessListener(u -> callback.onSuccess(email))
                                                    .addOnFailureListener(err -> callback.onFailure(
                                                            err.getMessage() != null ? err.getMessage() : "Could not send reset email."));
                                        });
                            });
                })
                .addOnFailureListener(e -> callback.onFailure("Server error: " + (e.getMessage() != null ? e.getMessage() : "Check internet connection.")));
    }

    // ─────────────────────────────────────────────
    //  Pending Invitations (100% FREE Firebase Auth email)
    // ─────────────────────────────────────────────

    /**
     * Checks Firestore for any Sanghas added by the admin where {@code invitationSent == false}.
     * Sends an email via Firebase Authentication completely FREE (no Blaze plan / paid extensions).
     * The Sangha receives an email with a link to set their password, and then marks {@code invitationSent = true}.
     */
    public void sendPendingInvitations() {
        if (!firebaseReady) return;

        db.collection(COLLECTION_SANGHAS)
                .whereEqualTo("invitationSent", false)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String email = doc.getString("email");
                        String regNo = doc.getString("registrationNo");
                        if (email == null || email.isEmpty()) continue;

                        String tempPassword = getTempPassword(regNo);
                        // Provision Auth account and send free password reset / invitation email
                        auth.createUserWithEmailAndPassword(email, tempPassword)
                                .addOnCompleteListener(task -> {
                                    auth.sendPasswordResetEmail(email)
                                            .addOnSuccessListener(unused -> doc.getReference().update("invitationSent", true));
                                });
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  Fetch Sangha profile from Firestore
    // ─────────────────────────────────────────────

    public void fetchSanghaProfile(String sanghaId, FetchCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected.");
            return;
        }

        db.collection(COLLECTION_SANGHAS)
                .whereEqualTo("sanghaId", sanghaId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        callback.onFailure("Sangha profile not found.");
                    } else {
                        callback.onSuccess(query.getDocuments().get(0).getData());
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Could not load profile."));
    }

    // ═════════════════════════════════════════════
    //  CITIZEN MODULE (Firebase Auth + Firestore)
    // ═════════════════════════════════════════════

    public static final String COLLECTION_CITIZENS = "citizens";

    // ─────────────────────────────────────────────
    //  Citizen Registration
    // ─────────────────────────────────────────────

    /**
     * Registers a new Citizen with Firebase Auth and stores profile in Firestore "citizens" collection.
     * Sends a FREE email verification after account creation.
     */
    public void registerCitizen(String fullName, String email, String mobile, String password,
                                String dob, String gender, String address, String pinCode,
                                CitizenCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected yet.");
            return;
        }

        // Create Firebase Auth account
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onFailure("Account creation failed. Please try again.");
                        return;
                    }

                    // Send FREE email verification
                    user.sendEmailVerification();

                    String uid = user.getUid();

                    // Build citizen profile document
                    Map<String, Object> citizenData = new HashMap<>();
                    citizenData.put("authUid", uid);
                    citizenData.put("fullName", fullName);
                    citizenData.put("email", email);
                    citizenData.put("mobile", mobile);
                    citizenData.put("dob", dob);
                    citizenData.put("gender", gender);
                    citizenData.put("address", address);
                    citizenData.put("pinCode", pinCode);
                    citizenData.put("role", "CITIZEN");
                    citizenData.put("createdAt", com.google.firebase.Timestamp.now());

                    // Save to Firestore "citizens" collection using Auth UID as doc ID
                    db.collection(COLLECTION_CITIZENS).document(uid)
                            .set(citizenData)
                            .addOnSuccessListener(unused ->
                                    callback.onSuccess(citizenData, uid, email))
                            .addOnFailureListener(e ->
                                    callback.onFailure("Account created but failed to save profile: " + e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "Registration failed.";
                    if (msg.contains("email address is already in use")) {
                        msg = "This email is already registered. Please sign in or use Forgot Password.";
                    }
                    callback.onFailure(msg);
                });
    }

    // ─────────────────────────────────────────────
    //  Citizen Login (Email/Mobile + Password)
    // ─────────────────────────────────────────────

    /**
     * Logs in a Citizen. If identifier is a mobile number, looks up email from Firestore first.
     */
    public void loginCitizen(String identifier, String password, CitizenCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected yet.");
            return;
        }

        // Check if identifier is a mobile number (10 digits)
        if (identifier.matches("^[0-9]{10}$")) {
            // Look up email by mobile number in Firestore
            db.collection(COLLECTION_CITIZENS)
                    .whereEqualTo("mobile", identifier)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            callback.onFailure("No account found with this mobile number.");
                            return;
                        }
                        String email = query.getDocuments().get(0).getString("email");
                        if (email == null || email.isEmpty()) {
                            callback.onFailure("Account error. Contact support.");
                            return;
                        }
                        // Now sign in with the found email
                        signInCitizenWithEmail(email, password, callback);
                    })
                    .addOnFailureListener(e -> callback.onFailure(
                            "Server error: " + (e.getMessage() != null ? e.getMessage() : "Check internet connection.")));
        } else {
            // Identifier is an email, sign in directly
            signInCitizenWithEmail(identifier, password, callback);
        }
    }

    private void signInCitizenWithEmail(String email, String password, CitizenCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onFailure("Login failed.");
                        return;
                    }
                    // Fetch citizen profile from Firestore
                    db.collection(COLLECTION_CITIZENS).document(user.getUid())
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    callback.onSuccess(doc.getData(), user.getUid(), email);
                                } else {
                                    callback.onFailure("Citizen profile not found. Contact support.");
                                }
                            })
                            .addOnFailureListener(e -> callback.onFailure(
                                    "Could not load profile: " + e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "Login failed.";
                    if (msg.contains("password is invalid") || msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("no user record")) {
                        msg = "Invalid email/mobile or password.";
                    }
                    callback.onFailure(msg);
                });
    }

    // ─────────────────────────────────────────────
    //  Citizen Forgot Password (100% FREE)
    // ─────────────────────────────────────────────

    /**
     * Sends password reset email for a citizen. Accepts email or mobile number.
     */
    public void sendCitizenPasswordReset(String identifier, SimpleCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected yet.");
            return;
        }

        if (identifier.matches("^[0-9]{10}$")) {
            // Look up email by mobile
            db.collection(COLLECTION_CITIZENS)
                    .whereEqualTo("mobile", identifier)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            callback.onFailure("No account found with this mobile number.");
                            return;
                        }
                        String email = query.getDocuments().get(0).getString("email");
                        if (email == null || email.isEmpty()) {
                            callback.onFailure("No email associated with this account.");
                            return;
                        }
                        auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(unused -> callback.onSuccess(email))
                                .addOnFailureListener(e -> callback.onFailure(
                                        e.getMessage() != null ? e.getMessage() : "Could not send reset email."));
                    })
                    .addOnFailureListener(e -> callback.onFailure(
                            "Server error: " + (e.getMessage() != null ? e.getMessage() : "Check internet.")));
        } else {
            // Identifier is an email
            auth.sendPasswordResetEmail(identifier)
                    .addOnSuccessListener(unused -> callback.onSuccess(identifier))
                    .addOnFailureListener(e -> callback.onFailure(
                            e.getMessage() != null ? e.getMessage() : "Could not send reset email."));
        }
    }

    // ═════════════════════════════════════════════
    //  SANGHA VERIFICATION WORKFLOW
    // ═════════════════════════════════════════════

    /**
     * Submits a new Sangha verification application with supporting documents.
     * Prevents duplicate applications or accounts by checking email, registration number, and mobile.
     * Generates a unique Application ID and review token, saves to Firestore, and notifies Admin via Nodemailer.
     */
    public void submitSanghaApplication(
            Context context,
            String organizationName,
            String registrationNo,
            String contactPerson,
            String email,
            String mobile,
            String district,
            String locality,
            String address,
            String description,
            String username,
            String password,
            List<Map<String, Object>> documents,
            ApplicationSubmitCallback callback
    ) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected yet.");
            return;
        }

        final String cleanOrg = organizationName.trim();
        final String cleanRegNo = registrationNo.trim();
        final String regNoUpper = cleanRegNo.toUpperCase(Locale.ROOT);
        final String cleanContact = contactPerson.trim();
        final String cleanEmail = email.trim();
        final String emailLower = cleanEmail.toLowerCase(Locale.ROOT);
        final String cleanMobile = mobile.trim();
        final String cleanDistrict = district.trim();
        final String cleanLocality = locality.trim();
        final String cleanAddress = address.trim();
        final String cleanDesc = description.trim();
        final String cleanUsername = (username != null) ? username.trim() : "";
        final String cleanPassword = (password != null) ? password : "";

        // Duplicate prevention checks across existing Sanghas and active applications
        List<Task<QuerySnapshot>> dupChecks = new ArrayList<>();
        dupChecks.add(db.collection(COLLECTION_SANGHAS).whereEqualTo("email", cleanEmail).limit(1).get());
        dupChecks.add(db.collection(COLLECTION_SANGHAS).whereEqualTo("emailLower", emailLower).limit(1).get());
        dupChecks.add(db.collection(COLLECTION_SANGHAS).whereEqualTo("registrationNo", cleanRegNo).limit(1).get());
        dupChecks.add(db.collection(COLLECTION_SANGHAS).whereEqualTo("regNoUpper", regNoUpper).limit(1).get());
        dupChecks.add(db.collection(COLLECTION_SANGHAS).whereEqualTo("mobile", cleanMobile).limit(1).get());
        dupChecks.add(db.collection(COLLECTION_SANGHA_APPLICATIONS).whereEqualTo("emailLower", emailLower).get());
        dupChecks.add(db.collection(COLLECTION_SANGHA_APPLICATIONS).whereEqualTo("regNoUpper", regNoUpper).get());
        dupChecks.add(db.collection(COLLECTION_SANGHA_APPLICATIONS).whereEqualTo("mobile", cleanMobile).get());

        Tasks.whenAllSuccess(dupChecks)
                .addOnSuccessListener(results -> {
                    // Indexes 0..4 are COLLECTION_SANGHAS
                    for (int i = 0; i <= 4; i++) {
                        QuerySnapshot qs = (QuerySnapshot) results.get(i);
                        if (qs != null && !qs.isEmpty()) {
                            String field = (i <= 1) ? "email address" : (i <= 3 ? "registration number" : "mobile number");
                            callback.onFailure("A verified Sangha account already exists with this " + field + ".");
                            return;
                        }
                    }

                    // Indexes 5..7 are COLLECTION_SANGHA_APPLICATIONS (block non-rejected applications)
                    for (int i = 5; i <= 7; i++) {
                        QuerySnapshot qs = (QuerySnapshot) results.get(i);
                        if (qs != null) {
                            for (DocumentSnapshot doc : qs.getDocuments()) {
                                String st = doc.getString("status");
                                if (!STATUS_REJECTED.equalsIgnoreCase(st)) {
                                    String field = (i == 5) ? "email address" : (i == 6 ? "registration number" : "mobile number");
                                    callback.onFailure("An active application (" + doc.getId() + ", status: " + st
                                            + ") already exists for this " + field + ".");
                                    return;
                                }
                            }
                        }
                    }

                    String applicationId = "APP-SNG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
                    String reviewToken = UUID.randomUUID().toString().replace("-", "");
                    Timestamp now = Timestamp.now();

                    Map<String, Object> appData = new HashMap<>();
                    appData.put("applicationId", applicationId);
                    appData.put("organizationName", cleanOrg);
                    appData.put("registrationNo", cleanRegNo);
                    appData.put("regNoUpper", regNoUpper);
                    appData.put("contactPerson", cleanContact);
                    appData.put("email", cleanEmail);
                    appData.put("emailLower", emailLower);
                    appData.put("mobile", cleanMobile);
                    appData.put("district", cleanDistrict);
                    appData.put("locality", cleanLocality);
                    appData.put("address", cleanAddress);
                    appData.put("description", cleanDesc);
                    appData.put("documents", documents != null ? documents : new ArrayList<>());
                    appData.put("documentCount", documents != null ? documents.size() : 0);
                    appData.put("username", cleanUsername);
                    appData.put("tempPassword", cleanPassword); // used by server on Accept to create Auth account
                    appData.put("status", STATUS_PENDING);
                    appData.put("reviewToken", reviewToken);
                    appData.put("submittedAt", now);
                    appData.put("updatedAt", now);
                    appData.put("registrationTokenUsed", false);

                    db.collection(COLLECTION_SANGHA_APPLICATIONS).document(applicationId)
                            .set(appData)
                            .addOnSuccessListener(unused -> {
                                EmailServiceClient.notifyAdminNewApplication(
                                        context,
                                        applicationId,
                                        cleanOrg,
                                        cleanRegNo,
                                        cleanEmail,
                                        cleanMobile,
                                        cleanContact,
                                        cleanAddress,
                                        cleanDesc,
                                        documents != null ? documents.size() : 0,
                                        reviewToken,
                                        false,
                                        null,
                                        (emailSent, msg) -> callback.onSuccess(applicationId, reviewToken, emailSent, msg)
                                );
                            })
                            .addOnFailureListener(e -> callback.onFailure(
                                    "Could not submit application: " + (e.getMessage() != null ? e.getMessage() : "Unknown error")));
                })
                .addOnFailureListener(e -> callback.onFailure(
                        "Server error while checking duplicates: " + (e.getMessage() != null ? e.getMessage() : "Check internet connection.")));
    }

    /**
     * Allows a Sangha whose application is in INFO_REQUESTED state to respond and attach extra documents.
     */
    @SuppressWarnings("unchecked")
    public void resubmitSanghaApplication(
            Context context,
            String applicationId,
            String additionalNote,
            List<Map<String, Object>> newDocuments,
            ApplicationSubmitCallback callback
    ) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected.");
            return;
        }

        DocumentReference docRef = db.collection(COLLECTION_SANGHA_APPLICATIONS).document(applicationId.trim());
        docRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                callback.onFailure("Application ID not found.");
                return;
            }
            String status = doc.getString("status");
            if (!STATUS_INFO_REQUESTED.equalsIgnoreCase(status)) {
                callback.onFailure("Resubmission is only allowed when more information is requested.");
                return;
            }

            List<Map<String, Object>> mergedDocs = new ArrayList<>();
            Object existing = doc.get("documents");
            if (existing instanceof List<?>) {
                for (Object item : (List<?>) existing) {
                    if (item instanceof Map<?, ?>) {
                        mergedDocs.add((Map<String, Object>) item);
                    }
                }
            }
            if (newDocuments != null) {
                mergedDocs.addAll(newDocuments);
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("documents", mergedDocs);
            updates.put("documentCount", mergedDocs.size());
            updates.put("sanghaResponseNote", additionalNote.trim());
            updates.put("status", STATUS_PENDING);
            updates.put("updatedAt", Timestamp.now());

            String reviewToken = doc.getString("reviewToken");
            String orgName = doc.getString("organizationName");
            String regNo = doc.getString("registrationNo");
            String email = doc.getString("email");
            String mobile = doc.getString("mobile");
            String contactPerson = doc.getString("contactPerson");
            String address = doc.getString("address");
            String description = doc.getString("description");

            docRef.update(updates)
                    .addOnSuccessListener(unused -> EmailServiceClient.notifyAdminNewApplication(
                            context,
                            applicationId.trim(),
                            orgName != null ? orgName : "",
                            regNo != null ? regNo : "",
                            email != null ? email : "",
                            mobile != null ? mobile : "",
                            contactPerson != null ? contactPerson : "",
                            address != null ? address : "",
                            description != null ? description : "",
                            mergedDocs.size(),
                            reviewToken != null ? reviewToken : "",
                            true,
                            additionalNote.trim(),
                            (emailSent, msg) -> callback.onSuccess(applicationId.trim(), reviewToken, emailSent, msg)
                    ))
                    .addOnFailureListener(e -> callback.onFailure("Resubmission failed: " + e.getMessage()));
        }).addOnFailureListener(e -> callback.onFailure("Could not load application: " + e.getMessage()));
    }

    public void fetchSanghaApplication(String applicationId, ApplicationCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected.");
            return;
        }
        db.collection(COLLECTION_SANGHA_APPLICATIONS)
                .document(applicationId.trim())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || doc.getData() == null) {
                        callback.onFailure("Application ID \"" + applicationId + "\" not found.");
                    } else {
                        callback.onSuccess(doc.getData());
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Could not fetch application."));
    }

    public void fetchAllSanghaApplications(ApplicationListCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected.");
            return;
        }
        db.collection(COLLECTION_SANGHA_APPLICATIONS)
                .get()
                .addOnSuccessListener(query -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        if (doc.getData() != null) {
                            Map<String, Object> map = new HashMap<>(doc.getData());
                            if (!map.containsKey("applicationId")) {
                                map.put("applicationId", doc.getId());
                            }
                            list.add(map);
                        }
                    }
                    Collections.sort(list, (a, b) -> {
                        Timestamp ta = a.get("submittedAt") instanceof Timestamp ? (Timestamp) a.get("submittedAt") : null;
                        Timestamp tb = b.get("submittedAt") instanceof Timestamp ? (Timestamp) b.get("submittedAt") : null;
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Could not load applications."));
    }

    /**
     * Verifies an Admin review deep link using applicationId + reviewToken.
     */
    public void verifyAdminReviewLink(String applicationId, String reviewToken, ApplicationCallback callback) {
        fetchSanghaApplication(applicationId, new ApplicationCallback() {
            @Override
            public void onSuccess(Map<String, Object> appData) {
                Object stored = appData.get("reviewToken");
                String expected = stored instanceof String ? (String) stored : "";
                if (reviewToken == null || reviewToken.trim().isEmpty() || !expected.equals(reviewToken.trim())) {
                    callback.onFailure("Invalid or unauthorized review token.");
                } else {
                    callback.onSuccess(appData);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    /**
     * Admin decision: APPROVED, REJECTED (with mandatory reason), or INFO_REQUESTED.
     * Automatically emails the Sangha via Nodemailer.
     */
    public void submitAdminDecision(
            Context context,
            String applicationId,
            String decision,
            String remarks,
            AdminDecisionCallback callback
    ) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected.");
            return;
        }

        DocumentReference docRef = db.collection(COLLECTION_SANGHA_APPLICATIONS).document(applicationId.trim());
        docRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                callback.onFailure("Application not found.");
                return;
            }
            String currentStatus = doc.getString("status");
            if (STATUS_COMPLETED.equalsIgnoreCase(currentStatus)) {
                callback.onFailure("This Sangha has already completed registration.");
                return;
            }

            String orgName = doc.getString("organizationName");
            String toEmail = doc.getString("email");
            if (toEmail == null || toEmail.isEmpty()) {
                callback.onFailure("Application has no target email.");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", decision);
            updates.put("adminRemarks", remarks != null ? remarks.trim() : "");
            updates.put("reviewedBy", ADMIN_EMAIL);
            Timestamp now = Timestamp.now();
            updates.put("reviewedAt", now);
            updates.put("updatedAt", now);

            String regToken = null;
            String expiresFormatted = "";

            if (STATUS_APPROVED.equalsIgnoreCase(decision)) {
                regToken = UUID.randomUUID().toString().replace("-", "");
                long expiresAtMillis = System.currentTimeMillis() + REGISTRATION_TOKEN_TTL_MS;
                expiresFormatted = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(new Date(expiresAtMillis));

                updates.put("registrationToken", regToken);
                updates.put("registrationTokenUsed", false);
                updates.put("registrationTokenExpiresAt", expiresAtMillis);
                updates.put("registrationTokenExpiresAtStr", expiresFormatted);
            }

            final String finalToken = regToken;
            final String finalExpiresStr = expiresFormatted;

            docRef.update(updates)
                    .addOnSuccessListener(unused -> EmailServiceClient.notifySanghaDecision(
                            context,
                            applicationId.trim(),
                            orgName != null ? orgName : "Sangha Organization",
                            toEmail,
                            decision,
                            remarks,
                            finalToken,
                            finalExpiresStr,
                            (emailSent, msg) -> callback.onSuccess(decision, finalToken, finalExpiresStr, emailSent, msg)
                    ))
                    .addOnFailureListener(e -> callback.onFailure("Failed to save decision: " + e.getMessage()));
        }).addOnFailureListener(e -> callback.onFailure("Failed to load application: " + e.getMessage()));
    }

    /**
     * Validates a single-use, time-limited Sangha registration token from an approval email link.
     */
    public void validateRegistrationToken(String applicationId, String registrationToken, ApplicationCallback callback) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected.");
            return;
        }
        if (applicationId == null || applicationId.trim().isEmpty()
                || registrationToken == null || registrationToken.trim().isEmpty()) {
            callback.onFailure("Application ID and Registration Token are required.");
            return;
        }

        db.collection(COLLECTION_SANGHA_APPLICATIONS)
                .document(applicationId.trim())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || doc.getData() == null) {
                        callback.onFailure("Application ID not found.");
                        return;
                    }
                    String status = doc.getString("status");
                    String storedToken = doc.getString("registrationToken");
                    Boolean used = doc.getBoolean("registrationTokenUsed");
                    Long expiresAt = doc.getLong("registrationTokenExpiresAt");

                    if (STATUS_COMPLETED.equalsIgnoreCase(status) || Boolean.TRUE.equals(used)) {
                        callback.onFailure("This single-use registration link has already been used.");
                        return;
                    }
                    if (!STATUS_APPROVED.equalsIgnoreCase(status)) {
                        callback.onFailure("This application is not approved (Current status: " + status + ").");
                        return;
                    }
                    if (storedToken == null || !storedToken.equals(registrationToken.trim())) {
                        callback.onFailure("Invalid or unauthorized registration link/token.");
                        return;
                    }
                    if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
                        callback.onFailure("This registration link has expired. Contact " + ADMIN_EMAIL + ".");
                        return;
                    }
                    callback.onSuccess(doc.getData());
                })
                .addOnFailureListener(e -> callback.onFailure(
                        "Server error: " + (e.getMessage() != null ? e.getMessage() : "Check internet connection.")));
    }

    /**
     * Completes Sangha registration using the validated single-use registration link/token.
     * Atomically marks registrationTokenUsed=true via a Firestore Transaction and creates the Sangha doc.
     */
    public void completeRegistrationWithToken(
            String applicationId,
            String registrationToken,
            String username,
            String password,
            RegisterCallback callback
    ) {
        if (!firebaseReady) {
            callback.onFailure("Firebase is not connected.");
            return;
        }

        final String cleanAppId = applicationId.trim();
        final String cleanToken = registrationToken.trim();
        final String cleanUsername = username.trim();

        // Step 1: Ensure username is unique across sanghas
        db.collection(COLLECTION_SANGHAS)
                .whereEqualTo("username", cleanUsername)
                .limit(1)
                .get()
                .addOnSuccessListener(usernameQuery -> {
                    if (!usernameQuery.isEmpty()) {
                        callback.onFailure("Username \"" + cleanUsername + "\" is already taken. Please choose another.");
                        return;
                    }

                    // Step 2: Validate token & application state
                    validateRegistrationToken(cleanAppId, cleanToken, new ApplicationCallback() {
                        @Override
                        public void onSuccess(Map<String, Object> appData) {
                            String email = appData.get("email") instanceof String ? (String) appData.get("email") : "";
                            if (email.isEmpty()) {
                                callback.onFailure("Approved application is missing an email address.");
                                return;
                            }

                            // Step 3: Create Firebase Auth account locked to the approved application's email
                            auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener(authResult -> {
                                        FirebaseUser fbUser = authResult.getUser();
                                        if (fbUser == null) {
                                            callback.onFailure("Could not create authentication account.");
                                            return;
                                        }
                                        fbUser.sendEmailVerification();

                                        String sanghaId = "SNG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
                                        DocumentReference appRef = db.collection(COLLECTION_SANGHA_APPLICATIONS).document(cleanAppId);
                                        DocumentReference sanghaRef = db.collection(COLLECTION_SANGHAS).document(sanghaId);

                                        // Step 4: Atomic transaction to consume single-use token & create Sangha doc
                                        db.runTransaction(transaction -> {
                                            DocumentSnapshot snap = transaction.get(appRef);
                                            if (!snap.exists()) {
                                                throw new FirebaseFirestoreException("Application missing.",
                                                        FirebaseFirestoreException.Code.ABORTED);
                                            }
                                            Boolean used = snap.getBoolean("registrationTokenUsed");
                                            String st = snap.getString("status");
                                            String tok = snap.getString("registrationToken");
                                            Long exp = snap.getLong("registrationTokenExpiresAt");

                                            if (Boolean.TRUE.equals(used) || STATUS_COMPLETED.equalsIgnoreCase(st)) {
                                                throw new FirebaseFirestoreException("This single-use link has already been used.",
                                                        FirebaseFirestoreException.Code.ABORTED);
                                            }
                                            if (!STATUS_APPROVED.equalsIgnoreCase(st) || tok == null || !tok.equals(cleanToken)) {
                                                throw new FirebaseFirestoreException("Invalid registration token.",
                                                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
                                            }
                                            if (exp != null && System.currentTimeMillis() > exp) {
                                                throw new FirebaseFirestoreException("This registration link has expired.",
                                                        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED);
                                            }

                                            Timestamp now = Timestamp.now();
                                            Map<String, Object> appUpdates = new HashMap<>();
                                            appUpdates.put("registrationTokenUsed", true);
                                            appUpdates.put("status", STATUS_COMPLETED);
                                            appUpdates.put("completedAt", now);
                                            appUpdates.put("updatedAt", now);
                                            appUpdates.put("registeredUsername", cleanUsername);
                                            appUpdates.put("sanghaId", sanghaId);
                                            appUpdates.put("authUid", fbUser.getUid());
                                            transaction.update(appRef, appUpdates);

                                            Map<String, Object> sanghaDoc = new HashMap<>();
                                            sanghaDoc.put("sanghaId", sanghaId);
                                            sanghaDoc.put("applicationId", cleanAppId);
                                            sanghaDoc.put("authUid", fbUser.getUid());
                                            sanghaDoc.put("name", snap.getString("organizationName"));
                                            sanghaDoc.put("username", cleanUsername);
                                            sanghaDoc.put("email", email);
                                            sanghaDoc.put("emailLower", email.toLowerCase(Locale.ROOT));
                                            sanghaDoc.put("mobile", snap.getString("mobile"));
                                            sanghaDoc.put("registrationNo", snap.getString("registrationNo"));
                                            sanghaDoc.put("regNoUpper", snap.getString("regNoUpper"));
                                            sanghaDoc.put("contactPerson", snap.getString("contactPerson"));
                                            sanghaDoc.put("district", snap.getString("district"));
                                            sanghaDoc.put("locality", snap.getString("locality"));
                                            sanghaDoc.put("city", snap.getString("locality"));
                                            sanghaDoc.put("address", snap.getString("address"));
                                            sanghaDoc.put("description", snap.getString("description"));
                                            sanghaDoc.put("status", STATUS_APPROVED);
                                            sanghaDoc.put("passwordSet", true);
                                            sanghaDoc.put("createdAt", now);
                                            transaction.set(sanghaRef, sanghaDoc);

                                            return sanghaDoc;
                                        }).addOnSuccessListener(sanghaDoc ->
                                                callback.onSuccess(sanghaDoc, sanghaId, email)
                                        ).addOnFailureListener(err -> {
                                            // Roll back Auth user if single-use transaction failed
                                            fbUser.delete();
                                            callback.onFailure(err.getMessage() != null ? err.getMessage() : "Registration failed.");
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        String msg = e.getMessage() != null ? e.getMessage() : "Could not create account.";
                                        if (msg.contains("email address is already in use")) {
                                            msg = "An account with this email already exists. Please Sign In.";
                                        }
                                        callback.onFailure(msg);
                                    });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            callback.onFailure(errorMessage);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onFailure(
                        "Server error: " + (e.getMessage() != null ? e.getMessage() : "Check internet connection.")));
    }

    // ─────────────────────────────────────────────
    //  Callbacks
    // ─────────────────────────────────────────────

    public interface RegisterCallback {
        void onSuccess(Map<String, Object> sanghaData, String sanghaId, String email);
        void onFailure(String errorMessage);
    }

    public interface LoginCallback {
        void onSuccess(Map<String, Object> sanghaData, String sanghaId, String email);
        void onFailure(String errorMessage);
    }

    public interface CitizenCallback {
        void onSuccess(Map<String, Object> citizenData, String uid, String email);
        void onFailure(String errorMessage);
    }

    public interface FetchCallback {
        void onSuccess(Map<String, Object> sanghaData);
        void onFailure(String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess(String sentToEmail);
        void onFailure(String errorMessage);
    }

    public interface ApplicationSubmitCallback {
        void onSuccess(String applicationId, String reviewToken, boolean emailSent, String emailMessage);
        void onFailure(String errorMessage);
    }

    public interface ApplicationCallback {
        void onSuccess(Map<String, Object> applicationData);
        void onFailure(String errorMessage);
    }

    public interface ApplicationListCallback {
        void onSuccess(List<Map<String, Object>> applications);
        void onFailure(String errorMessage);
    }

    public interface AdminDecisionCallback {
        void onSuccess(String decision, String registrationToken, String expiresAtFormatted, boolean emailSent, String emailMessage);
        void onFailure(String errorMessage);
    }
}
