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

    public static void restoreAllData(Context context, FirebaseAuth mAuth, DatabaseHelper dbHelper, Callback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onFailure("Please sign in to restore your data");
            return;
        }
        String uid = user.getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.CollectionReference tripsRef = firestore.collection(USERS_COLLECTION)
                .document(uid).collection(TRIPS_COLLECTION);

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
            int localUserId = UserSessionManager.getCurrentUserId(context);

            List<Task<?>> writeTasks = new ArrayList<>();

            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshotList) {
                Map<String, Object> tripData = doc.getData();
                Trip trip = new Trip();
                // Try to set fields safely
                try {
                    // tripId is saved as original local trip id in backup doc id
                    int originalTripId = Integer.parseInt(doc.getId());
                    trip.setTripId(originalTripId);
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

                // Insert locally
                long newId = dbHelper.addTrip(trip);
                int insertedTripId = (int) newId;

                // Now child collections: schedules
                doc.getReference().collection(SCHEDULE_COLLECTION).get().addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot scheduleDoc : querySnapshot.getDocuments()) {
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
                        dbHelper.addSchedule(item);
                    }
                }).addOnFailureListener(
                        e -> Log.w(TAG, "Failed to restore schedules for trip: " + trip.getTripName(), e));

                // expenses
                doc.getReference().collection(EXPENSES_COLLECTION).get().addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot expenseDoc : querySnapshot.getDocuments()) {
                        Map<String, Object> expenseData = expenseDoc.getData();
                        Expense expense = new Expense();
                        expense.setTripId(insertedTripId);
                        expense.setCategory((String) expenseData.getOrDefault("category", null));
                        expense.setAmount(((Number) expenseData.getOrDefault("amount", 0)).doubleValue());
                        expense.setCurrency((String) expenseData.getOrDefault("currency", null));
                        expense.setNote((String) expenseData.getOrDefault("note", null));
                        expense.setSpentAt(((Number) expenseData.getOrDefault("spentAt", 0)).intValue());
                        expense.setImagePaths((String) expenseData.getOrDefault("imagePaths", null));
                        dbHelper.addExpense(expense);
                    }
                }).addOnFailureListener(
                        e -> Log.w(TAG, "Failed to restore expenses for trip: " + trip.getTripName(), e));

                // captures
                doc.getReference().collection(CAPTURES_COLLECTION).get().addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot captureDoc : querySnapshot.getDocuments()) {
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
                        dbHelper.addCapture(capture);
                    }
                }).addOnFailureListener(
                        e -> Log.w(TAG, "Failed to restore captures for trip: " + trip.getTripName(), e));

            }

            // restore completed
            callback.onSuccess();
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
