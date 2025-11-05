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


res/\
 ├── layout/          → XML layouts (activity_main.xml, fragment.xml, item.xml)\
 ├── drawable/        → Icons, shapes, backgrounds\
 ├── values/          → strings.xml, colors.xml, dimens.xml, styles.xml\
 ├── raw/             → Test media files (optional)\
 ├── mipmap/          → App launcher icons
