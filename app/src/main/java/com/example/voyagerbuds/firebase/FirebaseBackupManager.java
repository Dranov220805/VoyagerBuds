package com.example.voyagerbuds.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Capture;
import com.example.voyagerbuds.models.Expense;
import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.utils.UserSessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple helper for backing up and restoring user data to Firestore.
 * Current approach:
 * - Store trips under collection: users/{uid}/trips/{tripDoc}
 * - Child collections: schedules, expenses, captures
 * - We do not upload media binary files here — only metadata. Option to add
 * Storage later.
 */
public class FirebaseBackupManager {
    private static final String TAG = "FirebaseBackupManager";
    private static final String USERS_COLLECTION = "users";
    private static final String TRIPS_COLLECTION = "trips";
    private static final String SCHEDULE_COLLECTION = "schedules";
    private static final String EXPENSES_COLLECTION = "expenses";
    private static final String CAPTURES_COLLECTION = "captures";

    public interface Callback {
        void onSuccess();

        void onFailure(String error);
    }

    public interface PreviewCallback {
        void onPreview(BackupPreview preview);

        void onFailure(String error);
    }

    public static class BackupPreview {
        public int tripCount;
        public int scheduleCount;
        public int expenseCount;
        public int captureCount;
        public List<TripSummary> trips = new ArrayList<>();
    }

    public static class TripSummary {
        public int originalId;
        public String tripName;
        public int scheduleCount;
        public int expenseCount;
        public int captureCount;
    }

    public enum RestoreStrategy {
        APPEND,
        OVERWRITE,
        MERGE
    }

    public static void backupAllData(Context context, FirebaseAuth mAuth, DatabaseHelper dbHelper, Callback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onFailure("Please sign in to backup your data");
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        int localUserId = UserSessionManager.getCurrentUserId(context);
        if (localUserId == -1) {
            callback.onFailure("Cannot find local user id");
            return;
        }

        List<Trip> trips = dbHelper.getAllTrips(localUserId);

        if (trips == null || trips.isEmpty()) {
            callback.onFailure("No trips to backup");
            return;
        }

        CollectionReference tripsRef = firestore.collection(USERS_COLLECTION).document(uid)
                .collection(TRIPS_COLLECTION);

        // Pre-flight check: confirm we can write to the user's trips collection (helps
        // diagnose permissions)
        Map<String, Object> testWrite = new HashMap<>();
        testWrite.put("timestamp", System.currentTimeMillis());
        DocumentReference testDoc = firestore.collection(USERS_COLLECTION).document(uid).collection(TRIPS_COLLECTION)
                .document("preflight_test");
        final boolean[] preflightOk = { false };
        try {
            com.google.android.gms.tasks.Task<Void> testTask = testDoc.set(testWrite);
            testTask.addOnSuccessListener(testVoid -> {
                // Clean up the test doc
                testDoc.delete();

                // Now create actual tasks
                List<Task<?>> tasks = createWriteTasksForTrips(dbHelper, tripsRef, trips);

                // Wait for all to complete and aggregate results
                com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                        .addOnSuccessListener(aggVoid -> {
                            // Collect failed tasks and errors
                            List<String> errors = new ArrayList<>();
                            for (Task<?> t : tasks) {
                                if (!t.isSuccessful()) {
                                    Exception ex = t.getException();
                                    if (ex != null) {
                                        String msg = ex.getMessage();
                                        // Check for firestore permission denial
                                        if (ex instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                                            com.google.firebase.firestore.FirebaseFirestoreException fex = (com.google.firebase.firestore.FirebaseFirestoreException) ex;
                                            if (fex.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                                msg = "Permission denied: Ensure Firestore rules allow writes for authenticated users and that you're using the correct Firebase project config (google-services.json)";
                                            }
                                        }
                                        errors.add(msg);
                                        Log.w(TAG, "Task failed: " + msg, ex);
                                    } else {
                                        errors.add("Unknown error");
                                        Log.w(TAG, "Task failed with unknown error");
                                    }
                                }
                            }

                            if (errors.isEmpty()) {
                                callback.onSuccess();
                            } else {
                                // Attempt a single retry in case these are transient errors
                                List<Task<?>> retryTasks = createWriteTasksForTrips(dbHelper, tripsRef, trips);
                                com.google.android.gms.tasks.Tasks.whenAllComplete(retryTasks)
                                        .addOnSuccessListener(aVoid2 -> {
                                            List<String> retryErrors = new ArrayList<>();
                                            for (Task<?> rt : retryTasks) {
                                                if (!rt.isSuccessful()) {
                                                    Exception rex = rt.getException();
                                                    if (rex != null) {
                                                        String rmsg = rex.getMessage();
                                                        if (rex instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                                                            com.google.firebase.firestore.FirebaseFirestoreException rfex = (com.google.firebase.firestore.FirebaseFirestoreException) rex;
                                                            if (rfex.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                                                rmsg = "Permission denied: Ensure Firestore rules allow writes for authenticated users and that you're using the correct Firebase project config (google-services.json)";
                                                            }
                                                        }
                                                        retryErrors.add(rmsg);
                                                        Log.w(TAG, "Retry task failed: " + rmsg, rex);
                                                    } else {
                                                        retryErrors.add("Unknown error");
                                                        Log.w(TAG, "Retry task failed with unknown error");
                                                    }
                                                }
                                            }
                                            if (retryErrors.isEmpty()) {
                                                callback.onSuccess();
                                            } else {
                                                StringBuilder sb2 = new StringBuilder();
                                                sb2.append("Backup failed after retry for ").append(retryErrors.size())
                                                        .append(" item(s): ");
                                                for (String s : retryErrors) {
                                                    sb2.append(s).append("; ");
                                                }
                                                callback.onFailure(sb2.toString());
                                            }
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Log.e(TAG, "Retry aggregator failed to attach: " + e2.getMessage(), e2);
                                            callback.onFailure(
                                                    "Backup failed (retry aggregator error): " + e2.getMessage());
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            // This is a rare case where whenAllComplete fails to attach; report it
                            Log.e(TAG, "whenAllComplete failed to attach: " + e.getMessage(), e);
                            callback.onFailure("Backup failed to complete: " + e.getMessage());
                        });
            }); // end testTask.addOnSuccessListener

            testTask.addOnFailureListener(e -> {
                // Add diagnostic logs: auth UID, firebase project id, and helpful guidance
                String projectId = FirebaseApp.getInstance().getOptions().getProjectId();
                Log.e(TAG, "Preflight write failed: " + e.getMessage() + ", uid=" + uid + ", projectId=" + projectId,
                        e);
                callback.onFailure("Backup pre-flight check failed: " + e.getMessage() + " (uid=" + uid + ", projectId="
                        + projectId + ")");
            });
        } catch (Exception ex) {
            Log.e(TAG, "Exception during preflight write: " + ex.getMessage(), ex);
            callback.onFailure("Backup pre-flight failed: " + ex.getMessage());
        }
    }

    public static void preflightCheck(Context context, FirebaseAuth mAuth, DatabaseHelper dbHelper, Callback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onFailure("User is not authenticated. Please sign in before testing.");
            return;
        }
        String uid = user.getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference testDoc = firestore.collection(USERS_COLLECTION).document(uid).collection(TRIPS_COLLECTION)
                .document("preflight_test");
        Map<String, Object> data = new HashMap<>();
        data.put("test", true);
        data.put("timestamp", System.currentTimeMillis());
        testDoc.set(data).addOnSuccessListener(aVoid -> {
            testDoc.delete();
            callback.onSuccess();
        }).addOnFailureListener(e -> {
            String projectId = FirebaseApp.getInstance().getOptions().getProjectId();
            String errorMsg = e.getMessage();
            Log.e(TAG, "Preflight check failed (trips path): " + errorMsg + ", uid=" + uid + ", projectId=" + projectId,
                    e);
            callback.onFailure(
                    "Preflight check failed: " + errorMsg + " (uid=" + uid + ", projectId=" + projectId + ")");
        });
    }

    public static void fetchBackupPreview(Context context, FirebaseAuth mAuth, DatabaseHelper dbHelper,
            PreviewCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onFailure("Please sign in to check backup data");
            return;
        }
        String uid = user.getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference tripsRef = firestore.collection(USERS_COLLECTION).document(uid)
                .collection(TRIPS_COLLECTION);

        tripsRef.get().addOnSuccessListener(querySnapshot -> {
            List<com.google.firebase.firestore.DocumentSnapshot> docs = querySnapshot.getDocuments();
            if (docs == null || docs.isEmpty()) {
                callback.onFailure("No backup data found");
                return;
            }
            BackupPreview preview = new BackupPreview();
            preview.tripCount = docs.size();
            List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                TripSummary ts = new TripSummary();
                try {
                    ts.originalId = Integer.parseInt(doc.getId());
                } catch (Exception ignored) {
                    ts.originalId = -1;
                }
                ts.tripName = (String) doc.getData().getOrDefault("tripName", "");
                preview.trips.add(ts);

                // count child collections
                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> schTask = doc
                        .getReference()
                        .collection(SCHEDULE_COLLECTION).get().addOnSuccessListener(qs -> {
                            int c = qs.getDocuments().size();
                            ts.scheduleCount = c;
                            preview.scheduleCount += c;
                        }).addOnFailureListener(e -> Log.w(TAG, "Failed to count schedules for " + doc.getId(), e));
                tasks.add(schTask);

                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> expTask = doc
                        .getReference()
                        .collection(EXPENSES_COLLECTION).get().addOnSuccessListener(qs -> {
                            int c = qs.getDocuments().size();
                            ts.expenseCount = c;
                            preview.expenseCount += c;
                        }).addOnFailureListener(e -> Log.w(TAG, "Failed to count expenses for " + doc.getId(), e));
                tasks.add(expTask);

                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> capTask = doc
                        .getReference()
                        .collection(CAPTURES_COLLECTION).get().addOnSuccessListener(qs -> {
                            int c = qs.getDocuments().size();
                            ts.captureCount = c;
                            preview.captureCount += c;
                        }).addOnFailureListener(e -> Log.w(TAG, "Failed to count captures for " + doc.getId(), e));
                tasks.add(capTask);
            }

            com.google.android.gms.tasks.Tasks.whenAllComplete(tasks).addOnSuccessListener(aVoid -> {
                callback.onPreview(preview);
            }).addOnFailureListener(e -> callback.onFailure("Failed to fetch backup preview: " + e.getMessage()));
        }).addOnFailureListener(e -> callback.onFailure("Failed to fetch backup trips: " + e.getMessage()));
    }

    public static void restoreAllData(Context context, FirebaseAuth mAuth, DatabaseHelper dbHelper, Callback callback) {
        restoreAllData(context, mAuth, dbHelper, RestoreStrategy.APPEND, callback);
    }

    /**
     * Restore user data from Firestore with three strategies:
     * 
     * 1. OVERWRITE: Delete all local data first, then import everything from cloud.
     * - Destructive operation: all local data is lost
     * - Use when cloud data is the source of truth
     * 
     * 2. MERGE: Smart merge - skip duplicates, update existing matches, add new
     * items.
     * - Non-destructive: preserves local data
     * - Duplicate detection by: Trip (name+dates), Schedule (title+day+time),
     * Expense (category+amount+date+currency), Capture (mediaPath+capturedAt)
     * - Use when you want to sync data from multiple devices
     * 
     * 3. APPEND: Always add cloud data as new entries (may create duplicates).
     * - Non-destructive: preserves all local data
     * - Use when you want to keep both local and cloud data
     * 
     * Note: Images are NOT backed up to cloud yet (future update).
     * Only image metadata (paths) are synced.
     */
    public static void restoreAllData(Context context, FirebaseAuth mAuth, DatabaseHelper dbHelper,
            RestoreStrategy strategy, Callback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onFailure("Please sign in to restore your data");
            return;
        }
        String uid = user.getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.CollectionReference tripsRef = firestore.collection(USERS_COLLECTION)
                .document(uid).collection(TRIPS_COLLECTION);

        // Get local user ID
        int localUserId = UserSessionManager.getCurrentUserId(context);
        if (localUserId == -1) {
            callback.onFailure("Cannot find local user id");
            return;
        }

        // OVERWRITE strategy: Clear all local data first
        if (strategy == RestoreStrategy.OVERWRITE) {
            Log.d(TAG, "OVERWRITE mode: Clearing all local data...");
            dbHelper.clearUserData(localUserId);
        }

        // Get all trips
        tripsRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailure("Failed to fetch backup: "
                        + (task.getException() != null ? task.getException().getMessage() : "unknown"));
                return;
            }

            List<com.google.firebase.firestore.DocumentSnapshot> snapshotList = task.getResult().getDocuments();
            if (snapshotList == null || snapshotList.isEmpty()) {
                callback.onFailure("No backup data found");
                return;
            }

            // We'll insert data locally using database helper
            // Note: This basic restore does not delete existing local data — to keep safer
            // we append by default
            // Advanced behavior (merge/overwrite/clear) can be added later
            // localUserId already computed

            List<Task<?>> writeTasks = new ArrayList<>();
            List<com.google.android.gms.tasks.Task<?>> readTasks = new ArrayList<>();

            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshotList) {
                Map<String, Object> tripData = doc.getData();
                Trip trip = new Trip();
                // Try to set fields safely
                try {
                    // tripId is saved as original local trip id in backup doc id
                    int originalTripId = Integer.parseInt(doc.getId());
                    trip.setTripId(originalTripId);
                    trip.setFirebaseId(originalTripId);
                } catch (Exception e) {
                    // ignore
                }
                trip.setUserId(localUserId);
                trip.setTripName((String) tripData.getOrDefault("tripName", ""));
                trip.setStartDate((String) tripData.getOrDefault("startDate", null));
                trip.setEndDate((String) tripData.getOrDefault("endDate", null));
                trip.setDestination((String) tripData.getOrDefault("destination", null));
                trip.setNotes((String) tripData.getOrDefault("notes", null));
                trip.setPhotoUrl((String) tripData.getOrDefault("photoUrl", null));
                trip.setCreatedAt(((Number) tripData.getOrDefault("createdAt", 0)).longValue());
                trip.setUpdatedAt(((Number) tripData.getOrDefault("updatedAt", 0)).longValue());
                trip.setIsGroupTrip(((Number) tripData.getOrDefault("isGroupTrip", 0)).intValue());
                trip.setMapLatitude(((Number) tripData.getOrDefault("mapLatitude", 0.0)).doubleValue());
                trip.setMapLongitude(((Number) tripData.getOrDefault("mapLongitude", 0.0)).doubleValue());
                trip.setBudget(((Number) tripData.getOrDefault("budget", 0.0)).doubleValue());
                trip.setBudgetCurrency((String) tripData.getOrDefault("budgetCurrency", "USD"));
                trip.setParticipants((String) tripData.getOrDefault("participants", ""));

                // Check merge strategy: if MERGE, try to find existing trip by firebase id or
                // by name/date
                // MERGE: Skip duplicates, update existing matches
                // APPEND: Always add as new (default behavior)
                // OVERWRITE: Delete all local data first (handled above), then add
                long newIdLong;
                if (strategy == RestoreStrategy.MERGE) {
                    Trip existing = null;
                    // First, try to match by Firebase ID (most precise)
                    if (trip.getFirebaseId() > 0) {
                        existing = dbHelper.getTripByFirebaseId(trip.getFirebaseId(), localUserId);
                    }
                    // Fallback: match by tripName + startDate + endDate (precise duplicate
                    // detection)
                    if (existing == null) {
                        List<Trip> localTrips = dbHelper.getAllTrips(localUserId);
                        for (Trip lt : localTrips) {
                            boolean nameMatches = (lt.getTripName() != null && trip.getTripName() != null
                                    && lt.getTripName().trim().equalsIgnoreCase(trip.getTripName().trim()));
                            boolean startDateMatches = (lt.getStartDate() != null && trip.getStartDate() != null
                                    && lt.getStartDate().equals(trip.getStartDate()));
                            boolean endDateMatches = (lt.getEndDate() != null && trip.getEndDate() != null
                                    && lt.getEndDate().equals(trip.getEndDate()));

                            if (nameMatches && startDateMatches && endDateMatches) {
                                existing = lt;
                                break;
                            }
                        }
                    }
                    if (existing != null) {
                        // MERGE: Update existing trip with cloud data, preserve local ID
                        trip.setTripId(existing.getTripId());
                        trip.setFirebaseId(
                                existing.getFirebaseId() == 0 ? trip.getFirebaseId() : existing.getFirebaseId());
                        dbHelper.updateTrip(trip);
                        newIdLong = existing.getTripId();
                        Log.d(TAG, "MERGE: Updated existing trip - " + trip.getTripName());
                    } else {
                        // No match found, add as new trip
                        long newId = dbHelper.addTrip(trip);
                        newIdLong = newId;
                        Log.d(TAG, "MERGE: Added new trip - " + trip.getTripName());
                    }
                } else {
                    // APPEND or OVERWRITE: Simply add as new trip
                    long newId = dbHelper.addTrip(trip);
                    newIdLong = newId;
                    Log.d(TAG, strategy + ": Added trip - " + trip.getTripName());
                }
                int insertedTripId = (int) newIdLong;

                // Now child collections: schedules
                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> sTask = doc
                        .getReference().collection(SCHEDULE_COLLECTION).get().addOnSuccessListener(querySnapshot -> {
                            for (com.google.firebase.firestore.DocumentSnapshot scheduleDoc : querySnapshot
                                    .getDocuments()) {
                                Map<String, Object> scheduleData = scheduleDoc.getData();
                                ScheduleItem item = new ScheduleItem();
                                item.setTripId(insertedTripId);
                                item.setDay((String) scheduleData.getOrDefault("day", null));
                                item.setStartTime((String) scheduleData.getOrDefault("startTime", null));
                                item.setEndTime((String) scheduleData.getOrDefault("endTime", null));
                                item.setTitle((String) scheduleData.getOrDefault("title", null));
                                item.setNotes((String) scheduleData.getOrDefault("notes", null));
                                item.setLocation((String) scheduleData.getOrDefault("location", null));
                                item.setParticipants((String) scheduleData.getOrDefault("participants", null));
                                item.setImagePaths((String) scheduleData.getOrDefault("imagePaths", null));
                                item.setNotifyBeforeMinutes(
                                        ((Number) scheduleData.getOrDefault("notifyBeforeMinutes", 0)).intValue());
                                item.setCreatedAt(((Number) scheduleData.getOrDefault("createdAt", 0)).longValue());
                                item.setUpdatedAt(((Number) scheduleData.getOrDefault("updatedAt", 0)).longValue());
                                // For MERGE, try to avoid duplicates for schedules
                                // Match by: title + day + startTime (precise duplicate detection)
                                if (strategy == RestoreStrategy.MERGE) {
                                    List<ScheduleItem> existingSchedules = dbHelper.getSchedulesForTrip(insertedTripId);
                                    boolean found = false;
                                    for (ScheduleItem es : existingSchedules) {
                                        boolean titleMatches = (es.getTitle() != null && item.getTitle() != null
                                                && es.getTitle().trim().equalsIgnoreCase(item.getTitle().trim()));
                                        boolean dayMatches = (es.getDay() != null && item.getDay() != null
                                                && es.getDay().equals(item.getDay()));
                                        boolean startTimeMatches = (es.getStartTime() != null
                                                && item.getStartTime() != null
                                                && es.getStartTime().equals(item.getStartTime()));

                                        if (titleMatches && dayMatches && startTimeMatches) {
                                            // Found duplicate: update existing schedule with cloud data
                                            item.setId(es.getId());
                                            dbHelper.updateSchedule(item);
                                            found = true;
                                            Log.d(TAG, "MERGE: Updated schedule - " + item.getTitle());
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        dbHelper.addSchedule(item);
                                        Log.d(TAG, "MERGE: Added new schedule - " + item.getTitle());
                                    }
                                } else {
                                    dbHelper.addSchedule(item);
                                }
                            }
                        }).addOnFailureListener(
                                e -> Log.w(TAG, "Failed to restore schedules for trip: " + trip.getTripName(), e));
                readTasks.add(sTask);

                // expenses
                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> eTask = doc
                        .getReference().collection(EXPENSES_COLLECTION).get().addOnSuccessListener(querySnapshot -> {
                            for (com.google.firebase.firestore.DocumentSnapshot expenseDoc : querySnapshot
                                    .getDocuments()) {
                                Map<String, Object> expenseData = expenseDoc.getData();
                                Expense expense = new Expense();
                                expense.setTripId(insertedTripId);
                                expense.setCategory((String) expenseData.getOrDefault("category", null));
                                expense.setAmount(((Number) expenseData.getOrDefault("amount", 0)).doubleValue());
                                expense.setCurrency((String) expenseData.getOrDefault("currency", null));
                                expense.setNote((String) expenseData.getOrDefault("note", null));
                                expense.setSpentAt(((Number) expenseData.getOrDefault("spentAt", 0)).intValue());
                                expense.setImagePaths((String) expenseData.getOrDefault("imagePaths", null));
                                // For MERGE, avoid duplicates for expenses
                                // Match by: category + amount + spentAt + currency (precise duplicate
                                // detection)
                                if (strategy == RestoreStrategy.MERGE) {
                                    List<Expense> existingExpenses = dbHelper.getExpensesForTrip(insertedTripId);
                                    boolean found = false;
                                    for (Expense ee : existingExpenses) {
                                        boolean categoryMatches = (ee.getCategory() != null
                                                && expense.getCategory() != null
                                                && ee.getCategory().equals(expense.getCategory()));
                                        boolean amountMatches = (Math.abs(ee.getAmount() - expense.getAmount()) < 0.01); // Float
                                                                                                                         // comparison
                                        boolean spentAtMatches = (ee.getSpentAt() == expense.getSpentAt());
                                        boolean currencyMatches = (ee.getCurrency() != null
                                                && expense.getCurrency() != null
                                                && ee.getCurrency().equals(expense.getCurrency()));

                                        if (categoryMatches && amountMatches && spentAtMatches && currencyMatches) {
                                            // Found duplicate: update existing expense with cloud data
                                            expense.setExpenseId(ee.getExpenseId());
                                            dbHelper.updateExpense(expense);
                                            found = true;
                                            Log.d(TAG, "MERGE: Updated expense - " + expense.getCategory() + " $"
                                                    + expense.getAmount());
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        dbHelper.addExpense(expense);
                                        Log.d(TAG, "MERGE: Added new expense - " + expense.getCategory() + " $"
                                                + expense.getAmount());
                                    }
                                } else {
                                    dbHelper.addExpense(expense);
                                }
                            }
                        }).addOnFailureListener(
                                e -> Log.w(TAG, "Failed to restore expenses for trip: " + trip.getTripName(), e));
                readTasks.add(eTask);

                // captures
                com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> cTask = doc
                        .getReference().collection(CAPTURES_COLLECTION).get().addOnSuccessListener(querySnapshot -> {
                            for (com.google.firebase.firestore.DocumentSnapshot captureDoc : querySnapshot
                                    .getDocuments()) {
                                Map<String, Object> captureData = captureDoc.getData();
                                Capture capture = new Capture();
                                capture.setTripId(insertedTripId);
                                capture.setUserId(localUserId);
                                capture.setMediaPath((String) captureData.getOrDefault("mediaPath", null));
                                capture.setMediaType((String) captureData.getOrDefault("mediaType", null));
                                capture.setDescription((String) captureData.getOrDefault("description", null));
                                capture.setCapturedAt(((Number) captureData.getOrDefault("capturedAt", 0)).longValue());
                                capture.setCreatedAt(((Number) captureData.getOrDefault("createdAt", 0)).longValue());
                                capture.setUpdatedAt(((Number) captureData.getOrDefault("updatedAt", 0)).longValue());
                                // For MERGE, avoid duplicates for captures
                                // Match by: mediaPath + capturedAt (precise duplicate detection)
                                // Note: Images are not backed up to cloud yet, so mediaPath may reference local
                                // files
                                if (strategy == RestoreStrategy.MERGE) {
                                    List<Capture> existingCaptures = dbHelper.getCapturesForTrip(insertedTripId);
                                    boolean found = false;
                                    for (Capture ec : existingCaptures) {
                                        boolean mediaPathMatches = (ec.getMediaPath() != null
                                                && capture.getMediaPath() != null
                                                && ec.getMediaPath().equals(capture.getMediaPath()));
                                        boolean capturedAtMatches = (ec.getCapturedAt() == capture.getCapturedAt());

                                        if (mediaPathMatches && capturedAtMatches) {
                                            // Found duplicate: update existing capture with cloud data
                                            capture.setCaptureId(ec.getCaptureId());
                                            dbHelper.updateCapture(capture);
                                            found = true;
                                            Log.d(TAG, "MERGE: Updated capture - " + capture.getMediaPath());
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        dbHelper.addCapture(capture);
                                        Log.d(TAG, "MERGE: Added new capture - " + capture.getMediaPath());
                                    }
                                } else {
                                    dbHelper.addCapture(capture);
                                }
                            }
                        }).addOnFailureListener(
                                e -> Log.w(TAG, "Failed to restore captures for trip: " + trip.getTripName(), e));
                readTasks.add(cTask);

            }

            // Wait for all child reads to complete
            com.google.android.gms.tasks.Tasks.whenAllComplete(readTasks).addOnSuccessListener(aVoid -> {
                callback.onSuccess();
            }).addOnFailureListener(e -> callback.onFailure("Failed to complete restore reads: " + e.getMessage()));
        }).addOnFailureListener(e -> callback.onFailure("Failed to read backup data: " + e.getMessage()));
    }

    /**
     * Helper to create write tasks for each trip (and child collections) without
     * executing them multiple times.
     */
    private static List<Task<?>> createWriteTasksForTrips(DatabaseHelper dbHelper, CollectionReference tripsRef,
            List<Trip> trips) {
        List<Task<?>> tasks = new ArrayList<>();
        for (Trip trip : trips) {
            Map<String, Object> tripMap = new HashMap<>();
            tripMap.put("tripId", trip.getTripId());
            tripMap.put("tripName", trip.getTripName());
            tripMap.put("startDate", trip.getStartDate());
            tripMap.put("endDate", trip.getEndDate());
            tripMap.put("destination", trip.getDestination());
            tripMap.put("notes", trip.getNotes());
            tripMap.put("photoUrl", trip.getPhotoUrl());
            tripMap.put("createdAt", trip.getCreatedAt());
            tripMap.put("updatedAt", trip.getUpdatedAt());
            tripMap.put("isGroupTrip", trip.getIsGroupTrip());
            tripMap.put("mapLatitude", trip.getMapLatitude());
            tripMap.put("mapLongitude", trip.getMapLongitude());
            tripMap.put("budget", trip.getBudget());
            tripMap.put("budgetCurrency", trip.getBudgetCurrency());
            tripMap.put("participants", trip.getParticipants());
            tripMap.put("lastSyncedAt", System.currentTimeMillis());

            DocumentReference tripDocRef = tripsRef.document(String.valueOf(trip.getTripId()));

            Task<?> setTripTask = tripDocRef.set(tripMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Trip backed up: " + trip.getTripName()))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to backup trip " + trip.getTripName(), e));
            tasks.add(setTripTask);

            List<ScheduleItem> schedules = dbHelper.getSchedulesForTrip(trip.getTripId());
            for (ScheduleItem schedule : schedules) {
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("day", schedule.getDay());
                scheduleMap.put("startTime", schedule.getStartTime());
                scheduleMap.put("endTime", schedule.getEndTime());
                scheduleMap.put("title", schedule.getTitle());
                scheduleMap.put("notes", schedule.getNotes());
                scheduleMap.put("location", schedule.getLocation());
                scheduleMap.put("participants", schedule.getParticipants());
                scheduleMap.put("imagePaths", schedule.getImagePaths());
                scheduleMap.put("notifyBeforeMinutes", schedule.getNotifyBeforeMinutes());
                scheduleMap.put("createdAt", schedule.getCreatedAt());
                scheduleMap.put("updatedAt", schedule.getUpdatedAt());
                com.google.firebase.firestore.DocumentReference scheduleRef = tripDocRef.collection(SCHEDULE_COLLECTION)
                        .document(String.valueOf(schedule.getId()));
                Task<?> scheduleTask = scheduleRef
                        .set(scheduleMap)
                        .addOnSuccessListener(aVoid -> Log.d(TAG,
                                "Saved schedule: " + schedule.getTitle() + " at " + scheduleRef.getPath()))
                        .addOnFailureListener(e -> Log.w(TAG, "Failed to save schedule at " + scheduleRef.getPath()
                                + " [trip: " + trip.getTripName() + ", schedule: " + schedule.getTitle() + "]", e));
                tasks.add(scheduleTask);
            }

            List<Expense> expenses = dbHelper.getExpensesForTrip(trip.getTripId());
            for (Expense expense : expenses) {
                Map<String, Object> expenseMap = new HashMap<>();
                expenseMap.put("category", expense.getCategory());
                expenseMap.put("amount", expense.getAmount());
                expenseMap.put("currency", expense.getCurrency());
                expenseMap.put("note", expense.getNote());
                expenseMap.put("spentAt", expense.getSpentAt());
                expenseMap.put("imagePaths", expense.getImagePaths());
                com.google.firebase.firestore.DocumentReference expenseRef = tripDocRef.collection(EXPENSES_COLLECTION)
                        .document(String.valueOf(expense.getExpenseId()));
                Task<?> expenseTask = expenseRef
                        .set(expenseMap)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved expense at " + expenseRef.getPath()))
                        .addOnFailureListener(
                                e -> Log.w(TAG,
                                        "Failed to save expense at " + expenseRef.getPath() + " [trip: "
                                                + trip.getTripName() + ", expense: " + expense.getExpenseId() + "]",
                                        e));
                tasks.add(expenseTask);
            }

            List<Capture> captures = dbHelper.getCapturesForTrip(trip.getTripId());
            for (Capture capture : captures) {
                Map<String, Object> captureMap = new HashMap<>();
                captureMap.put("mediaPath", capture.getMediaPath());
                captureMap.put("mediaType", capture.getMediaType());
                captureMap.put("description", capture.getDescription());
                captureMap.put("capturedAt", capture.getCapturedAt());
                captureMap.put("createdAt", capture.getCreatedAt());
                captureMap.put("updatedAt", capture.getUpdatedAt());
                com.google.firebase.firestore.DocumentReference captureRef = tripDocRef.collection(CAPTURES_COLLECTION)
                        .document(String.valueOf(capture.getCaptureId()));
                Task<?> captureTask = captureRef
                        .set(captureMap)
                        .addOnSuccessListener(aVoid -> Log.d(TAG,
                                "Saved capture: " + capture.getCaptureId() + " at " + captureRef.getPath()))
                        .addOnFailureListener(
                                e -> Log.w(TAG,
                                        "Failed to save capture at " + captureRef.getPath() + " [trip: "
                                                + trip.getTripName() + ", capture: " + capture.getCaptureId() + "]",
                                        e));
                tasks.add(captureTask);
            }
        }
        return tasks;
    }
}
