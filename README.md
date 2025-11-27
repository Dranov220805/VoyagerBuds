# VoyagerBuds
VoyagerBuds - an android trip planner app based on Java

# Project Structure

app/\
 └── java/\
      └── com.example.voyagerbuds/\
           ├── activities/          → Activities (MainActivity, OtherActivity)\
           ├── adapters/            → RecyclerView/ListView adapters\
           ├── fragments/           → UI fragments (Fragment)\
           ├── models/              → Data classes (provide database)\
           ├── services/            → Background services (UserService)\
           ├── utils/               → Helper classes (MediaUtils, PermissionUtils)\
           ├── interfaces/          → Custom listeners/callbacks\
           └── MainApplication.java → App-level initialization

## Backup & Restore

Users can back up their trip data to Firebase Firestore and restore it later. The feature is available in the Profile screen, under Support.

Notes:
- Requires the user to be logged in (Firebase Auth). The login screen uses Firebase authentication (Email/Google).
- The backup currently stores trip metadata, schedules, expenses and capture metadata (media paths) to Firestore under `users/{uid}/trips`.
- Media file upload/restore (binary files) is not performed by default, but can be added by uploading the `mediaPath` files to Firebase Storage in a future enhancement.

### Firestore Security Rules

Ensure your Firestore security rules allow authenticated users to read and write only their own documents. Example rules:

```
rules_version = '2';
service cloud.firestore {
     match /databases/{database}/documents {
          match /users/{userId}/trips/{tripId} {
               allow read, write: if request.auth != null && request.auth.uid == userId;
          }
     }
}
```

If you see PERMISSION_DENIED during backup, verify the following:
- The Firebase project in `google-services.json` matches the project where Firestore is enabled.
- Your Firestore rules above are applied and published (not the default open or locked rules).
- The user is properly signed in before backup (Profile shows user email and name).
- For Google sign-in: make sure OAuth client and SHA fingerprints are configured in Firebase Console.

Important for subcollections (schedules/expenses/captures):
If your data model creates child collections under the trip document (for example `users/{uid}/trips/{tripId}/schedules/{scheduleId}` or `users/{uid}/trips/{tripId}/expenses/{expenseId}`), make sure your Firestore rules allow writes to these subcollections too. A single rule for `users/{userId}/trips/{tripId}` does not automatically apply to nested collections.

Example rules covering subcollections:

```
rules_version = '2';
service cloud.firestore {
     match /databases/{database}/documents {
          match /users/{userId}/trips/{tripId} {
               allow read, write: if request.auth != null && request.auth.uid == userId;
          }
          match /users/{userId}/trips/{tripId}/{subCollection}/{docId} {
               allow read, write: if request.auth != null && request.auth.uid == userId;
          }
     }
}
```

Alternative wildcard rule covering subcollections (use carefully):
```
match /users/{userId}/{document=**} {
     allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

### How to use
1. Open Profile > Support > Backup Data to upload your trips to Firebase (must be logged in)
2. Open Profile > Support > Restore Data to import backed up trips to local database (appends to existing data).


res/\
 ├── layout/          → XML layouts (activity_main.xml, fragment.xml, item.xml)\
 ├── drawable/        → Icons, shapes, backgrounds\
 ├── values/          → strings.xml, colors.xml, dimens.xml, styles.xml\
 ├── raw/             → Test media files (optional)\
 ├── mipmap/          → App launcher icons
