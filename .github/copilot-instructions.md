# Copilot Instructions for VoyagerBuds

## Project Overview
VoyagerBuds is an Android trip planner app written in Java. The codebase is organized by feature and responsibility, following standard Android architecture with some project-specific conventions.

## Key Structure
- `app/src/main/java/com/example/voyagerbuds/`
  - `activities/` — Main entry points for screens (e.g., `HomeActivity`, `MainActivity`).
  - `fragments/` — UI components for navigation and modular screens (e.g., `HomeFragment`, `ScheduleFragment`, `MapFragment`).
  - `adapters/` — RecyclerView/ListView adapters for displaying lists.
  - `models/` — Data classes, often matching database tables.
  - `services/` — Background and utility services.
  - `utils/` — Helper classes for common tasks.
  - `interfaces/` — Custom listeners and callbacks.
  - `MainApplication.java` — App-level initialization.
- `res/layout/` — XML layouts for activities, fragments, and list items.
- `res/values/` — Strings, colors, dimensions, and styles.

## Developer Workflows
- **Build:** Use Gradle (`gradlew build` or via Android Studio). Main build files: `build.gradle.kts`, `app/build.gradle.kts`.
- **Run/Debug:** Use Android Studio's run/debug tools. Entry activity is typically `HomeActivity`.
- **Database:** Uses SQLite via `DatabaseHelper` in `database/`. Models in `models/` map to tables.
- **Testing:** No dedicated test suite found; manual testing is typical. If adding tests, use `app/src/test/java/`.
- **Navigation:** Fragment transitions are managed in `HomeActivity` with custom slide animations (`setCustomAnimations`).

## Project-Specific Patterns
- **Fragment Animation:** All major fragments use slide-in/slide-out transitions. For internal UI, fade-in animations are applied to root views in `onCreateView`.
- **Trip Data:** Central to the app; managed via `Trip` model and related fragments/adapters.
- **Adapters:** Always use context and a listener/callback for item actions.
- **Database Access:** Use `DatabaseHelper` for all CRUD operations. Avoid direct SQLite queries elsewhere.
- **Resource Naming:** Layouts and resources are named by feature (e.g., `fragment_home.xml`, `item_trip.xml`).

## Integration Points
- **Google Play Services:** Location features use `FusedLocationProviderClient`.
- **OSMDroid:** Map features use OSMDroid (`MapView`, overlays) in `MapFragment`.
- **Material Components:** UI uses Material Design components (e.g., `FloatingActionButton`).

## Examples
- To add a new trip, update `TripAdapter`, `HomeFragment`, and database logic in `DatabaseHelper`.
- To add a new fragment, create the class in `fragments/`, add its layout, and update navigation in `HomeActivity`.
- For new list screens, create an adapter in `adapters/` and a model in `models/`.

## Conventions
- Keep UI logic in fragments/activities, business/data logic in helpers/services.
- Use resource IDs and context-aware methods for UI updates.
- Follow existing naming and directory patterns for new files.

---

If any section is unclear or missing, please provide feedback for improvement.