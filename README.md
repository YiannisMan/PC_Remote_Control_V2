# PC Remote Control V2 (GREEK)

Το **PC Remote Control V2** είναι ένα έργο που επιτρέπει τον απομακρυσμένο έλεγχο ηλεκτρονικών υπολογιστών μέσω μιας εφαρμογής Android. Αποτελείται από μια εφαρμογή Android που λειτουργεί ως πελάτης και έναν server σε Python που εκτελείται στους υπολογιστές-στόχους.

---

## Χαρακτηριστικά

* **Απομακρυσμένες Εντολές:**
    * **Echo:** Ελέγχει την κατάσταση (online/offline) και το λειτουργικό σύστημα των υπολογιστών.
    * **Restart:** Επανεκκινεί τους επιλεγμένους υπολογιστές.
    * **Shutdown:** Τερματίζει τη λειτουργία των επιλεγμένους υπολογιστών.
    * **Restore:** Προσομοιώνει μια ενέργεια επαναφοράς.
    * **Wake-on-LAN (WOL):** Εκκινεί τους υπολογιστές από κατάσταση αναστολής λειτουργίας ή τερματισμού.
* **Διεπαφή Χρήστη Android:**
    * Οπτική αναπαράσταση των διαθέσιμων υπολογιστών.
    * Δυνατότητα επιλογής πολλαπλών υπολογιστών για μαζική εκτέλεση εντολών.
    * Κονσόλα εξόδου για την παρακολούθηση των απαντήσεων από τους servers.
    * Δυναμική ενημέρωση της κατάστασης των υπολογιστών (π.χ., "Online", "Offline", "Rebooting...").

---

## Τεχνολογίες

* **Android (Java):**
    * RecyclerView για την εμφάνιση της λίστας των υπολογιστών.
    * Android Services για τη διαχείριση της δικτυακής επικοινωνίας στο παρασκήνιο.
    * Executors για ασύγχρονες δικτυακές κλήσεις χωρίς να "παγώνει" το UI.
* **Python:**
    * `socket` programming για τη δημιουργία του TCP server.
    * `threading` για την ταυτόχρονη εξυπηρέτηση πολλαπλών πελατών.

---

## Δομή του Έργου

```
.
├── app/                  # Φάκελος της εφαρμογής Android
│   ├── src/main/
│   │   ├── java/com/example/lab_control_v2/
│   │   │   ├── adapter/      # Adapters για το RecyclerView
│   │   │   ├── model/        # Μοντέλα δεδομένων
│   │   │   ├── service/      # Network Service για την επικοινωνία
│   │   │   ├── utils/        # Βοηθητικές κλάσεις (Constants, AppExecutors, SoundPlayer)
│   │   │   ├── ChooseActionActivity.java  # Αρχική οθόνη επιλογής ενέργειας
│   │   │   └── PcListActivity.java        # Οθόνη λίστας υπολογιστών
│   │   ├── res/              # Πόροι της εφαρμογής (layouts, strings, drawables)
│   │   └── AndroidManifest.xml # Manifest της εφαρμογής
│   └── build.gradle.kts      # Build script για την εφαρμογή Android
├── gradle/               # Wrapper του Gradle
├── gmeleServer.py        # Το script του Python server
└── ...
```

---

## Εγκατάσταση και Ρύθμιση

### Server (Python)

1.  **Απαιτήσεις:** Βεβαιωθείτε ότι έχετε εγκατεστημένη την Python στον υπολογιστή-στόχο.
2.  **Ρύθμιση:**
    * Ανοίξτε το αρχείο `gmeleServer.py`.
    * Τροποποιήστε το dictionary `IP_TO_HOSTNAME_MAP` για να αντιστοιχίσετε τη διεύθυνση IP του server με το επιθυμητό όνομα υπολογιστή (hostname).

        ```python
        IP_TO_HOSTNAME_MAP = {
            "ΧΧΧ.ΧΧΧ.Χ.ΧΧΧ": "PRPC01",
            "ΧΧΧ.ΧΧΧ.Χ.ΧΧΧ": "PRPC02",
            # ... προσθέστε περισσότερους υπολογιστές
        }
        ```
3.  **Εκτέλεση:**
    * Εκκινήστε τον server εκτελώντας την παρακάτω εντολή στο τερματικό:
        ```bash
        python gmeleServer.py
        ```
    * Ο server θα ξεκινήσει να "ακούει" για συνδέσεις στη πόρτα **41007**.

### Client (Android)

1.  **Ρύθμιση:**
    * Ανοίξτε το έργο στο Android Studio.
    * Πλοηγηθείτε στο αρχείο `app/src/main/java/com/example/lab_control_v2/utils/Constants.java`.
    * Ενημερώστε τους χάρτες `IP_ADDRESSES_MAP` και `MAC_ADDRESSES_MAP` με τις σωστές διευθύνσεις IP και MAC των υπολογιστών-στόχων.

        ```java
        // Αρχικοποίηση IP Addresses
        IP_ADDRESSES_MAP = new HashMap<>();
        IP_ADDRESSES_MAP.put("PRPC01", "ΧΧΧ.ΧΧΧ.Χ.ΧΧΧ");

        // Αρχικοποίηση MAC Addresses
        MAC_ADDRESSES_MAP = new HashMap<>();
        MAC_ADDRESSES_MAP.put("PRPC01", "ΥΥ:ΥΥ:ΥΥ:ΥΥ:ΥΥ:ΥΥ");
        ```
    * Τροποποιήστε τη σταθερά `NUM_PCS` για να αντιστοιχεί στον συνολικό αριθμό των υπολογιστών.
2.  **Build και Εγκατάσταση:**
    * Κάντε build το έργο και εγκαταστήστε το αρχείο `.apk` στην Android συσκευή σας.

---

## Χρήση

1.  **Εκκίνηση:** Ανοίξτε την εφαρμογή "Lab Control" στη συσκευή σας.
2.  **Επιλογή Ενέργειας:** Στην αρχική οθόνη, επιλέξτε την ενέργεια που θέλετε να εκτελέσετε (π.χ., Echo, Restart, Shutdown).
3.  **Λίστα Υπολογιστών:** Θα μεταφερθείτε στην οθόνη λίστας υπολογιστών. Η εφαρμογή θα προσπαθήσει αυτόματα να ελέγξει την κατάσταση όλων των υπολογιστών.
4.  **Επιλογή Υπολογιστών:** Χρησιμοποιήστε τα checkboxes για να επιλέξετε τους υπολογιστές στους οποίους θέλετε να εφαρμόσετε την ενέργεια. Μπορείτε να χρησιμοποιήσετε τα κουμπιά "Select All" και "Deselect All" για ευκολία.
5.  **Εκτέλεση:** Πατήστε το κουμπί **"Continue"** και επιβεβαιώστε την ενέργεια στο παράθυρο διαλόγου που θα εμφανιστεί.
6.  **Παρακολούθηση:** Παρακολουθήστε την πρόοδο και τις απαντήσεις από τους υπολογιστές στην κονσόλα στο κάτω μέρος της οθόνης.

---

# PC Remote Control V2 (ENGLISH)

**PC Remote Control V2** is a project that allows for the remote control of computers via an Android application. It consists of an Android application that acts as a client and a Python server that runs on the target computers.

---

## Features

* **Remote Commands:**
    * **Echo:** Checks the status (online/offline) and operating system of the computers.
    * **Restart:** Reboots the selected computers.
    * **Shutdown:** Shuts down the selected computers.
    * **Restore:** Simulates a restore action.
    * **Wake-on-LAN (WOL):** Starts up computers from a sleep or shutdown state.
* **Android User Interface:**
    * Visual representation of available computers.
    * Ability to select multiple computers for bulk command execution.
    * An output console to monitor responses from the servers.
    * Dynamic updating of computer statuses (e.g., "Online", "Offline", "Rebooting...").

---

## Technologies

* **Android (Java):**
    * RecyclerView for displaying the list of computers.
    * Android Services to manage network communication in the background.
    * Executors for asynchronous network calls without freezing the UI.
* **Python:**
    * `socket` programming for creating the TCP server.
    * `threading` to handle multiple clients simultaneously.

---

## Project Structure

```
.
├── app/                  # Android application folder
│   ├── src/main/
│   │   ├── java/com/example/lab_control_v2/
│   │   │   ├── adapter/      # Adapters for the RecyclerView
│   │   │   ├── model/        # Data models
│   │   │   ├── service/      # Network Service for communication
│   │   │   ├── utils/        # Utility classes (Constants, AppExecutors, SoundPlayer)
│   │   │   ├── ChooseActionActivity.java  # Initial action selection screen
│   │   │   └── PcListActivity.java        # Computer list screen
│   │   ├── res/              # Application resources (layouts, strings, drawables)
│   │   └── AndroidManifest.xml # Application manifest
│   └── build.gradle.kts      # Build script for the Android app
├── gradle/               # Gradle wrapper
├── gmeleServer.py        # The Python server script
└── ...
```

---

## Installation and Setup

### Server (Python)

1.  **Requirements:** Ensure you have Python installed on the target computer.
2.  **Configuration:**
    * Open the `gmeleServer.py` file.
    * Modify the `IP_TO_HOSTNAME_MAP` dictionary to map the server's IP address to the desired hostname.

        ```python
        IP_TO_HOSTNAME_MAP = {
            "ΧΧΧ.ΧΧΧ.Χ.ΧΧΧ": "PRPC01",
            "ΧΧΧ.ΧΧΧ.Χ.ΧΧΧ": "PRPC02",
            # ... add more computers
        }
        ```
3.  **Execution:**
    * Start the server by running the following command in the terminal:
        ```bash
        python gmeleServer.py
        ```
    * The server will start listening for connections on port **41007**.

### Client (Android)

1.  **Setup:**
    * Open the project in Android Studio.
    * Navigate to the `app/src/main/java/com/example/lab_control_v2/utils/Constants.java` file.
    * Update the `IP_ADDRESSES_MAP` and `MAC_ADDRESSES_MAP` with the correct IP and MAC addresses of the target computers.

        ```java
        // Initialize IP Addresses
        IP_ADDRESSES_MAP = new HashMap<>();
        IP_ADDRESSES_MAP.put("PRPC01", "ΧΧΧ.ΧΧΧ.Χ.ΧΧΧ")

        // Initialize MAC Addresses
        MAC_ADDRESSES_MAP = new HashMap<>();
        MAC_ADDRESSES_MAP.put("PRPC01", "ΥΥ:ΥΥ:ΥΥ:ΥΥ:ΥΥ:ΥΥ");
        ```
    * Modify the `NUM_PCS` constant to match the total number of computers.
2.  **Build and Install:**
    * Build the project and install the `.apk` file on your Android device.

---

## Usage

1.  **Launch:** Open the "Lab Control" application on your device.
2.  **Select Action:** On the main screen, choose the action you want to perform (e.g., Echo, Restart, Shutdown).
3.  **Computer List:** You will be taken to the computer list screen. The application will automatically try to check the status of all computers.
4.  **Select Computers:** Use the checkboxes to select the computers on which you want to apply the action. You can use the "Select All" and "Deselect All" buttons for convenience.
5.  **Execute:** Press the **"Continue"** button and confirm the action in the dialog box that appears.
6.  **Monitor:** Track the progress and responses from the computers in the console at the bottom of the screen.
