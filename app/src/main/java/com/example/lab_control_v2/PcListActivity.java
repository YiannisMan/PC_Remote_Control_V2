package com.example.lab_control_v2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab_control_v2.adapter.CardAdapter;
import com.example.lab_control_v2.model.CardModel;
import com.example.lab_control_v2.repository.PcRepository;
import com.example.lab_control_v2.service.NetworkService;
import com.example.lab_control_v2.utils.AppExecutors;
import com.example.lab_control_v2.utils.Constants;
import com.example.lab_control_v2.utils.SoundPlayer;

import java.util.ArrayList;
import java.util.List;

public class PcListActivity extends AppCompatActivity {

    /*
    Αυτή είναι η κύρια οθόνη αλληλεπίδρασης του χρήστη με τους υπολογιστές.
    Ο ρόλος της είναι να εμφανίσει μια λειτουργική διεπαφή (UI) για την επιλογή των Η/Υ και την εκτέλεση της ενέργειας που έχει ήδη επιλεγεί από το MainActivity.
    Συντονίζει τα δεδομένα, την εμφάνιση και τις ενέργειες του χρήστη.

    Κατά την έναρξή της, συνδέεται με το NetworkService.
    Εμφανίζει τη λίστα των 27 Η/Υ σε ένα RecyclerView, χρησιμοποιώντας τον CardAdapter για την εμφάνιση κάθε Η/Υ.
    Διαχειρίζεται την κατάσταση της διεπαφής: ποιοι Η/Υ είναι επιλεγμένοι, ποια ενέργεια εκτελείται, και τι μηνύματα εμφανίζονται στην κονσόλα.
    Όταν ο χρήστης ζητήσει μια ενέργεια, το Activity2 δίνει την εντολή στο συνδεδεμένο NetworkService να την εκτελέσει.
    "Ακούει" για τα αποτελέσματα από το NetworkService (μέσω των callbacks) και ενημερώνει δυναμικά το RecyclerView και την κονσόλα.
    * */

    private Button backBtn;
    private Button continueBtn;
    private Button refreshAllBtn;
    private Button selectAllBtn;
    private Button deselectAllBtn;
    private TextView consoleOutputTV;
    private TextView actionTitleTV;
    private RecyclerView cardRV;
    private CardAdapter cardAdapter;

    private ArrayList<CardModel> cardModelArrayList;

    // Services and Repository
    private PcRepository pcRepository;

    private NetworkService networkService; // Η παρουσία του συνδεδεμένου NetworkService
    private boolean isServiceBound = false; // Flag για την κατάσταση της σύνδεσης με το Service

    private String currentAction; // Η ενέργεια που επιλέχθηκε από το MainActivity
    private static final String TAG = "Activity2_Lab";

    // ServiceConnection για το binding με το NetworkService
    private ServiceConnection serviceConnection = new ServiceConnection() {
        /*
         * Καλείται όταν η σύνδεση με το Service έχει εγκαθιδρυθεί.
         * Παρέχει το IBinder για την αλληλεπίδραση με το Service.
         */
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            NetworkService.LocalBinder binder = (NetworkService.LocalBinder) service;
            networkService = binder.getService(); // Λήψη της παρουσίας του NetworkService
            isServiceBound = true; // Ορισμός του flag ότι το service είναι συνδεδεμένο
            Log.d(TAG, "NetworkService connected to Activity2");
            // Μόλις συνδεθεί το service, κάνουμε έναν αρχικό έλεγχο κατάστασης
            performRefreshAllStatus(false); // false για να μην εμφανίσει μήνυμα "Ανανέωση..."
        }

        /*
         * Καλείται όταν η σύνδεση με το Service έχει χαθεί
         * (π.χ., crash ή terminated).
         * Δεν καλείται κατά το unbind.
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            networkService = null;
            Log.d(TAG, "NetworkService disconnected from Activity2");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pc_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Λήψη της ενέργειας από το MainActivity
        currentAction = getIntent().getStringExtra(Constants.ACTION_KEY);
        if (currentAction == null)
        {
            Toast.makeText(this, "Δεν επιλέχθηκε ενέργεια!", Toast.LENGTH_LONG).show();
            finish(); // Κλείσιμο αν δεν υπάρχει ενέργεια
            return;
        }

        pcRepository = new PcRepository(); // Αυτό θα φορτώσει και τις MAC από την κλάση Constants
        RecyclerView cardRV = findViewById(R.id.idRVCard);

        cardRV.setLayoutManager(new LinearLayoutManager(this));

        cardModelArrayList = new ArrayList<>();
        for(int i = 1; i <= Constants.NUM_PCS ; i++)
        {
            // Χρησιμοποιούμε την κλάση Constants για το prefix και τον αριθμό των PC
            cardModelArrayList.add(new CardModel(Constants.PC_NAME_PREFIX + String.format("%02d", i),"","Άγνωστη"));
        }

        setupUi();
        updateActionTitleAndButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind στο NetworkService όταν το Activity γίνεται ορατή
        Intent intent = new Intent(this, NetworkService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE); // Δημιουργεί το service αν δεν εκτελείται ήδη.
        Log.d(TAG, "Attempting to bind NetworkService...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind από το Service όταν το Activity δεν είναι πλέον ορατό
        if (isServiceBound)
        {
            unbindService(serviceConnection);
            isServiceBound = false; // Ορισμός του flag ότι δεν είμαστε πλέον συνδεδεμένοι
            networkService = null; // Αποδέσμευση της αναφοράς στο service
            Log.d(TAG, "Unbinding NetworkService...");
        }
    }

    private void setupUi()
    {
        cardRV = findViewById(R.id.idRVCard);
        backBtn = findViewById(R.id.buttonBack);
        continueBtn = findViewById(R.id.buttonContinue);

        consoleOutputTV = findViewById(R.id.idEchoText);
        actionTitleTV = findViewById(R.id.idTitle);
        refreshAllBtn = findViewById(R.id.buttonRefresh);
        selectAllBtn = findViewById(R.id.buttonSelectAll);
        deselectAllBtn = findViewById(R.id.buttonDeselect);

        if (consoleOutputTV != null)
        {
            consoleOutputTV.setMovementMethod(new ScrollingMovementMethod());
            consoleOutputTV.setText(""); // Καθαρισμός κονσόλας κατά την έναρξη
        }
        else Log.e(TAG, "setupUi: consoleOutputTV is null.");

        cardRV.setLayoutManager(new LinearLayoutManager(this));
        cardAdapter = new CardAdapter(this, cardModelArrayList, (position, isChecked) -> {
            if (position >= 0 && position < cardModelArrayList.size())
            {
                cardModelArrayList.get(position).setSelected(isChecked);
            }
        });
        cardRV.setAdapter(cardAdapter);

        backBtn.setOnClickListener(v -> {
            SoundPlayer.playPop(this);
            finish();
        });
        continueBtn.setOnClickListener(v -> {
            SoundPlayer.playPop(this);
            showConfirmationDialog();
        });


        if (refreshAllBtn != null)
        {
            refreshAllBtn.setOnClickListener(v -> {
                SoundPlayer.playPop(this);
                performRefreshAllStatus(true);
            });
        }
        else Log.e(TAG, "setupUi: buttonRefreshAll is null.");

        if (selectAllBtn != null)
        {
            selectAllBtn.setOnClickListener(v -> {
                SoundPlayer.playPop(this);
                selectOrDeselectAll(true);
            });
        }
        else Log.e(TAG, "setupUi: buttonSelectAll is null.");

        if (deselectAllBtn != null)
        {
            deselectAllBtn.setOnClickListener(v -> {
                SoundPlayer.playPop(this);
                selectOrDeselectAll(false);
            });
        }
        else Log.e(TAG, "setupUi: buttonDeselectAll is null.");
    }

    private void selectOrDeselectAll(boolean select)
    {
        for (CardModel model : cardModelArrayList)
        {
            model.setSelected(select); // Ορισμός της κατάστασης isSelected για κάθε μοντέλο
        }

        if(cardAdapter != null)
        {
            cardAdapter.notifyDataSetChanged(); // Ενημέρωση του adapter για να ανανεωθεί η εμφάνιση όλων των items
        }
        else {
            Log.e(TAG, "selectOrDeselectAll: cardAdapter is null");
        }
    }

    /*
     * Ενημερώνει το TextView του τίτλου της ενέργειας (tvActionTitle)
     */
    private void updateActionTitleAndButton()
    {
        // Χρήση των string resources που ορίσαμε στο strings.xml
        String titlePrefix = getString(R.string.selected_action_prefix);
        String actionDescription = getString(R.string.action_unknown_desc); // Default

        if (currentAction != null) // Έλεγχος για null currentAction
        {
            switch (currentAction)
            {
                case Constants.ACTION_ECHO: actionDescription = getString(R.string.action_echo_desc); break;
                case Constants.ACTION_RESTART: actionDescription = getString(R.string.action_restart_desc); break;
                case Constants.ACTION_SHUTDOWN: actionDescription = getString(R.string.action_shutdown_desc); break;
                case Constants.ACTION_RESTORE: actionDescription = getString(R.string.action_restore_desc); break;
                case Constants.ACTION_WOL: actionDescription = getString(R.string.action_wol_desc); break;
            }
        }
        else
        {
            Log.e(TAG, "updateActionTitleAndButton: currentAction is null!");
        }

        if (actionTitleTV != null)
        {
            actionTitleTV.setText(titlePrefix + actionDescription);
        }
    }

    /*
     * Εμφανίζει ένα AlertDialog για να επιβεβαιώσει ο χρήστης την ενέργεια που πρόκειται να εκτελέσει.
     */
    private void showConfirmationDialog()
    {
        List<String> selectedPcNames = new ArrayList<>();
        for (CardModel model : cardModelArrayList)
        {
            if (model.isSelected())
            {
                selectedPcNames.add(model.getHostname());
            }
        }

        if (selectedPcNames.isEmpty()) // Αν δεν έχει επιλεγεί κανένας Η/Υ
        {
            Toast.makeText(this, "Δεν έχετε επιλέξει υπολογιστές.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Δημιουργία του μηνύματος για το AlertDialog
        String message = "Θέλετε να εκτελέσετε την ενέργεια '" + currentActionToUserFriendlyString() +
                "' στους ακόλουθους " + selectedPcNames.size() + " υπολογιστές;\n" +
                String.join(", ", selectedPcNames);

        AlertDialog.Builder builder = new AlertDialog.Builder(PcListActivity.this);
        builder.setMessage(message);
        builder.setTitle("Επιβεβαίωση Ενέργειας");
        builder.setCancelable(false); // Ο χρήστης πρέπει να επιλέξει ένα από τα κουμπιά
        builder.setPositiveButton("Εκτέλεση", (dialog, which) -> {
            performSelectedAction(selectedPcNames); // Κλήση της μεθόδου για την εκτέλεση της ενέργειας
        });
        builder.setNegativeButton("Άκυρο", (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /*
     * Μετατρέπει τη σταθερά της τρέχουσας ενέργειας (π.χ., Constants.ACTION_SHUTDOWN)
     * σε μια πιο φιλική προς τον χρήστη περιγραφή (π.χ., "Τερματισμού").
     * Κάνει return Η φιλική περιγραφή της ενέργειας.
     */
    private String currentActionToUserFriendlyString()
    {
        // Χρησιμοποιούμε string resources για μετάφραση και καλύτερη διαχείριση
        if (currentAction == null)
        {
            return getString(R.string.action_unknown_desc);
        }
        switch (currentAction)
        {
            case Constants.ACTION_ECHO: return getString(R.string.action_echo_desc);
            case Constants.ACTION_RESTART: return getString(R.string.action_restart_desc);
            case Constants.ACTION_SHUTDOWN: return getString(R.string.action_shutdown_desc);
            case Constants.ACTION_RESTORE: return getString(R.string.action_restore_desc);
            case Constants.ACTION_WOL: return getString(R.string.action_wol_desc);
            default: return getString(R.string.action_unknown_desc);
        }
    }

    /*
     * Εκτελεί την τρέχουσα επιλεγμένη ενέργεια (currentAction) στους Η/Υ
     * των οποίων τα hostnames δίνονται στη λίστα selectedPcHostnames.
     * Καλεί τις κατάλληλες μεθόδους του NetworkService.
     */
    private void performSelectedAction(List<String> selectedPcHostnames) {
        // Έλεγχος αν το service είναι συνδεδεμένο πριν την κλήση των μεθόδων του
        if (!isServiceBound || networkService == null)
        {
            appendToConsole("Σφάλμα: Η υπηρεσία δικτύου δεν είναι συνδεδεμένη.");
            Toast.makeText(this, "Η υπηρεσία δεν είναι έτοιμη, δοκιμάστε ξανά.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Χρήση string resource για το μήνυμα, με placeholders για την ενέργεια και τον αριθμό των Η/Υ
        appendToConsole(String.format(getString(R.string.executing_action_on_pcs), currentActionToUserFriendlyString(), selectedPcHostnames.size()));
        for (String hostname : selectedPcHostnames)
        {
            appendToConsole("--- " + hostname + " ---"); // Διαχωριστικό για κάθε Η/Υ στην κονσόλα
            CardModel pcModel = findCardModelByHostname(hostname); // Εύρεση του CardModel για πιθανή ενημέρωση

            if (Constants.ACTION_WOL.equals(currentAction)) // Αν η ενέργεια είναι WOL
            {
                String mac = pcRepository.getMacAddress(hostname); // Λήψη της MAC από το PcRepository
                if (mac != null && !mac.equalsIgnoreCase("XX:XX:XX:XX:XX:XX") && !mac.trim().isEmpty())
                {
                    networkService.sendWolPacket(mac, wolCallback); // Κλήση της μεθόδου του NetworkService
                }
                else
                {
                    // Χρήση string resource για το μήνυμα σφάλματος
                    String errorMsg = String.format(getString(R.string.wol_error_no_mac), hostname, mac);
                    appendToConsole(errorMsg);
                    if (pcModel != null) // Ενημέρωση UI
                    {
                        updateCardModelStatusInList(pcModel, "WOL Error: No MAC", "");
                    }
                }
            }
            else // Για όλες τις άλλες ενέργειες (Echo, Restart, Shutdown, Restore)
            {
                networkService.executePcCommand(hostname, currentAction, commandCallback);  // Κλήση της μεθόδου του NetworkService
            }
        }
    }

    /*
     * Στέλνει την εντολή "Echo" σε όλους τους Η/Υ της λίστας για να ανανεώσει την κατάστασή τους στο UI.
     * Η showMessage true αν πρέπει να εμφανιστεί μήνυμα έναρξης στην κονσόλα.
     */
    private void performRefreshAllStatus(boolean showMessage)
    {
        if (!isServiceBound || networkService == null)
        {
            // Δεν κάνουμε τίποτα αν το service δεν είναι έτοιμο.
            // Η onServiceConnected θα καλέσει αυτή τη μέθοδο όταν το service είναι έτοιμο.
            if (showMessage) appendToConsole("Η υπηρεσία δικτύου δεν είναι έτοιμη για ανανέωση.");
            return;
        }
        if (showMessage)
        {
            appendToConsole(getString(R.string.refreshing_all_status));
        }
        for (CardModel model : cardModelArrayList)
        {
            // Για την ανανέωση, πάντα στέλνουμε την εντολή ACTION_ECHO
            networkService.executePcCommand(model.getHostname(), Constants.ACTION_ECHO, commandCallback);
        }
    }

    /*
     * Προσθέτει ένα μήνυμα στο TextView της κονσόλας (tvConsoleOutput).
     * Η εκτέλεση γίνεται στο UI thread για ασφάλεια.
     */
    private void appendToConsole(String message)
    {
        AppExecutors.getInstance().runOnMainThread(() -> { // Εξασφαλίζει εκτέλεση στο UI thread
            if (consoleOutputTV != null)
            {
                consoleOutputTV.append(message + "\n"); // Προσθήκη του μηνύματος και αλλαγή γραμμής
            }
            Log.d(TAG, "Console: " + message);
        });
    }

    /*
     * Βρίσκει ένα CardModel στη λίστα cardModelArrayList με βάση το hostname.
     * hostname Το όνομα του Η/Υ προς αναζήτηση.
     * Και κάνει return Το CardModel αντικείμενο αν βρεθεί, αλλιώς null.
     */
    private CardModel findCardModelByHostname(String hostname)
    {
        for (CardModel model : cardModelArrayList)
        {
            if (model.getHostname().equals(hostname))
            {
                return model;
            }
        }
        return null;
    }

    /*
     * Ενημερώνει την κατάσταση (status) και το λειτουργικό σύστημα (os) ενός CardModel
     * στη λίστα cardModelArrayList και ειδοποιεί τον CardAdapter για την αλλαγή.
     * Η εκτέλεση γίνεται στο UI thread.
     */
    private void updateCardModelStatusInList(CardModel model, String status, String os)
    {
        if (model == null) return;
        // Η κλήση γίνεται ήδη από το UI thread (μέσω των callbacks που χρησιμοποιούν AppExecutors),
        // αλλά μια επιπλέον κλήση στο AppExecutors.getInstance().runOnMainThread() δεν βλάπτει,
        // απλά εξασφαλίζουμε ότι είμαστε σίγουρα στο σωστό thread.
        AppExecutors.getInstance().runOnMainThread(() -> {
            model.setStatus(status);
            model.setOs(os);
            // Βρίσκουμε τη θέση του model στη λίστα για να ενημερώσουμε μόνο αυτό το item
            int position = cardModelArrayList.indexOf(model);
            if (position != -1 && cardAdapter != null)
            {
                cardAdapter.notifyItemChanged(position);
            }
            else if (cardAdapter != null)
            {
                // Fallback: αν δεν βρεθεί η θέση (δεν θα έπρεπε να συμβεί) ή ο adapter είναι null (επίσης δεν θα έπρεπε να συμβεί). (Αν συμβεί ο Θεός βοηθός)
                Log.w(TAG, "CardModel not found in list for update or adapter is null, refreshing all.");
                cardAdapter.notifyDataSetChanged();
            }
        });
    }

    // --- Υλοποιήσεις των Callbacks από το NetworkService ---

    /*
     * Callback για τα αποτελέσματα της αποστολής WOL πακέτων.
     */
    private final NetworkService.WolCallback wolCallback = new NetworkService.WolCallback()
    {
        @Override
        public void onWolSent(String macAddress)
        {
            appendToConsole(String.format(getString(R.string.wol_packet_sent_to_mac), macAddress));
        }
        @Override
        public void onError(String macAddress, String errorDetails, String localizedError)
        {
            appendToConsole(String.format(getString(R.string.wol_error_for_mac), macAddress, localizedError, errorDetails));
        }
    };

    /*
     * Callback για τις απαντήσεις από τις εντολές TCP που στάλθηκαν στους Η/Υ.
     */
    private final NetworkService.CommandCallback commandCallback = new NetworkService.CommandCallback()
    {
        /*
         * Καλείται όταν ληφθεί η τελική απάντηση από τον server.
         */
        @Override
        public void onResponse(String hostname, String rawResponse, String derivedStatus, String derivedOs)
        {
            String message;
            if(rawResponse != null)
            {
                message = hostname + ": " + rawResponse.trim();
            }
            else
            {
                message = hostname + ": " + "N/A";
            }

            if(derivedOs.isEmpty())
            {
                message += " -> Status: [" + derivedStatus + "], OS: [" + "N/A" + "]";
            }
            else
            {
                message += " -> Status: [" + derivedStatus + "], OS: [" + derivedOs + "]";
            }
            appendToConsole(message);
            CardModel model = findCardModelByHostname(hostname);

            if (model != null)
            {
                updateCardModelStatusInList(model, derivedStatus, derivedOs); // Ενημερώνουμε το UI
            }
        }

        /*
         * Καλείται κατά τη διάρκεια μιας εντολής που έχει πολλαπλά στάδια (π.χ., "Restore").
         */
        @Override
        public void onProgress(String hostname, String progressMessage)
        {
            appendToConsole(hostname + " Progress: " + progressMessage.trim());
            CardModel model = findCardModelByHostname(hostname);

            if (model != null)
            {
                // Εμφάνιση του μηνύματος προόδου ως την τρέχουσα κατάσταση
                String statusFromProgress;
                if(progressMessage.contains("-"))
                {
                    statusFromProgress = progressMessage.split("-",2)[1].trim();
                }
                else
                {
                    statusFromProgress = progressMessage;
                }
                updateCardModelStatusInList(model, statusFromProgress, model.getOs()); // Κρατάμε το προηγούμενο OS κατά την πρόοδο
            }
        }

        /*
         * Καλείται αν παρουσιαστεί σφάλμα κατά την επικοινωνία.
         */
        @Override
        public void onError(String hostname, String errorMsg, String localizedErrorMsg)
        {
            appendToConsole(hostname + " Σφάλμα: " + localizedErrorMsg + " (Λεπτομέρειες: " + errorMsg + ")");
            CardModel model = findCardModelByHostname(hostname);
            if (model != null)
            {
                updateCardModelStatusInList(model, "Error: " + localizedErrorMsg, ""); // Εμφάνιση του σφάλματος στο UI
            }
        }
    };
}