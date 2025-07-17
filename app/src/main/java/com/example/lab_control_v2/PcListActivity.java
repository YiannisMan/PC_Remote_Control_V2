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
import com.example.lab_control_v2.service.NetworkService;
import com.example.lab_control_v2.utils.AppExecutors;
import com.example.lab_control_v2.utils.Constants;
import com.example.lab_control_v2.utils.SoundPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PcListActivity extends AppCompatActivity {

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

    private NetworkService networkService;
    private boolean isServiceBound = false;

    private String currentAction;
    private static final String TAG = "PcListActivity_Lab";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            NetworkService.LocalBinder binder = (NetworkService.LocalBinder) service;
            networkService = binder.getService();
            isServiceBound = true;
            Log.d(TAG, "NetworkService connected to PcListActivity");

            // Enable buttons and perform initial status check now that service is bound
            setUiEnabled(true);
            performRefreshAllStatus(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            networkService = null;
            Log.d(TAG, "NetworkService disconnected from PcListActivity");
            // Disable buttons if the service disconnects unexpectedly
            setUiEnabled(false);
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

        currentAction = getIntent().getStringExtra(Constants.ACTION_KEY);
        if (currentAction == null) {
            Toast.makeText(this, "Δεν επιλέχθηκε ενέργεια!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cardModelArrayList = new ArrayList<>();
        for(int i = 1; i <= Constants.NUM_PCS ; i++) {
            cardModelArrayList.add(new CardModel(Constants.PC_NAME_PREFIX + String.format("%02d", i),"","Άγνωστη"));
        }

        setupUi();
        updateActionTitleAndButton();
        setUiEnabled(false); // Disable UI until service is connected
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, NetworkService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Attempting to bind NetworkService...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
            networkService = null;
            Log.d(TAG, "Unbinding NetworkService...");
        }
    }

    private void setupUi() {
        cardRV = findViewById(R.id.idRVCard);
        backBtn = findViewById(R.id.buttonBack);
        continueBtn = findViewById(R.id.buttonContinue);
        consoleOutputTV = findViewById(R.id.idEchoText);
        actionTitleTV = findViewById(R.id.idTitle);
        refreshAllBtn = findViewById(R.id.buttonRefresh);
        selectAllBtn = findViewById(R.id.buttonSelectAll);
        deselectAllBtn = findViewById(R.id.buttonDeselect);

        if (consoleOutputTV != null) {
            consoleOutputTV.setMovementMethod(new ScrollingMovementMethod());
            consoleOutputTV.setText("");
        } else Log.e(TAG, "setupUi: consoleOutputTV is null.");

        cardRV.setLayoutManager(new LinearLayoutManager(this));
        cardAdapter = new CardAdapter(this, cardModelArrayList, (position, isChecked) -> {
            if (position >= 0 && position < cardModelArrayList.size()) {
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

        refreshAllBtn.setOnClickListener(v -> {
            SoundPlayer.playPop(this);
            performRefreshAllStatus(true);
        });

        selectAllBtn.setOnClickListener(v -> {
            SoundPlayer.playPop(this);
            selectOrDeselectAll(true);
        });

        deselectAllBtn.setOnClickListener(v -> {
            SoundPlayer.playPop(this);
            selectOrDeselectAll(false);
        });
    }

    private void setUiEnabled(boolean enabled) {
        continueBtn.setEnabled(enabled);
        refreshAllBtn.setEnabled(enabled);
        selectAllBtn.setEnabled(enabled);
        deselectAllBtn.setEnabled(enabled);
        if (!enabled) {
            consoleOutputTV.append("Connecting to service...\n");
        }
    }


    private void selectOrDeselectAll(boolean select) {
        for (CardModel model : cardModelArrayList) {
            model.setSelected(select);
        }
        if (cardAdapter != null) {
            cardAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "selectOrDeselectAll: cardAdapter is null");
        }
    }

    private void updateActionTitleAndButton() {
        String titlePrefix = getString(R.string.selected_action_prefix);
        String actionDescription = getString(R.string.action_unknown_desc);

        if (currentAction != null) {
            switch (currentAction) {
                case Constants.ACTION_ECHO: actionDescription = getString(R.string.action_echo_desc); break;
                case Constants.ACTION_RESTART: actionDescription = getString(R.string.action_restart_desc); break;
                case Constants.ACTION_SHUTDOWN: actionDescription = getString(R.string.action_shutdown_desc); break;
                case Constants.ACTION_RESTORE: actionDescription = getString(R.string.action_restore_desc); break;
                case Constants.ACTION_WOL: actionDescription = getString(R.string.action_wol_desc); break;
            }
        } else {
            Log.e(TAG, "updateActionTitleAndButton: currentAction is null!");
        }

        if (actionTitleTV != null) {
            actionTitleTV.setText(titlePrefix + actionDescription);
        }
    }

    private void showConfirmationDialog() {
        List<String> selectedPcNames = new ArrayList<>();
        for (CardModel model : cardModelArrayList) {
            if (model.isSelected()) {
                selectedPcNames.add(model.getHostname());
            }
        }

        if (selectedPcNames.isEmpty()) {
            Toast.makeText(this, "Δεν έχετε επιλέξει υπολογιστές.", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Θέλετε να εκτελέσετε την ενέργεια '" + currentActionToUserFriendlyString() +
                "' στους ακόλουθους " + selectedPcNames.size() + " υπολογιστές;\n" +
                String.join(", ", selectedPcNames);

        AlertDialog.Builder builder = new AlertDialog.Builder(PcListActivity.this);
        builder.setMessage(message);
        builder.setTitle("Επιβεβαίωση Ενέργειας");
        builder.setCancelable(false);
        builder.setPositiveButton("Εκτέλεση", (dialog, which) -> performSelectedAction(selectedPcNames));
        builder.setNegativeButton("Άκυρο", (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private String currentActionToUserFriendlyString() {
        if (currentAction == null) {
            return getString(R.string.action_unknown_desc);
        }
        switch (currentAction) {
            case Constants.ACTION_ECHO: return getString(R.string.action_echo_desc);
            case Constants.ACTION_RESTART: return getString(R.string.action_restart_desc);
            case Constants.ACTION_SHUTDOWN: return getString(R.string.action_shutdown_desc);
            case Constants.ACTION_RESTORE: return getString(R.string.action_restore_desc);
            case Constants.ACTION_WOL: return getString(R.string.action_wol_desc);
            default: return getString(R.string.action_unknown_desc);
        }
    }

    private void performSelectedAction(List<String> selectedPcHostnames) {
        if (!isServiceBound || networkService == null) {
            Log.e(TAG, "Σφάλμα: Η υπηρεσία δικτύου δεν είναι συνδεδεμένη.");
            Toast.makeText(this, "Η υπηρεσία δεν είναι έτοιμη, δοκιμάστε ξανά.", Toast.LENGTH_SHORT).show();
            return;
        }

        appendToConsole(String.format(getString(R.string.executing_action_on_pcs), currentActionToUserFriendlyString(), selectedPcHostnames.size()));
        for (String hostname : selectedPcHostnames) {
            appendToConsole("--- " + hostname + " ---");
            CardModel pcModel = findCardModelByHostname(hostname);

            if (Constants.ACTION_WOL.equals(currentAction)) {
                String mac = Constants.getMacAddress(hostname);
                if (mac != null && !mac.equalsIgnoreCase("XX:XX:XX:XX:XX:XX") && !mac.trim().isEmpty()) {
                    networkService.sendWolPacket(mac, wolCallback);
                } else {
                    String errorMsg = String.format(getString(R.string.wol_error_no_mac), hostname, mac);
                    Log.e(TAG, errorMsg);
                    if (pcModel != null) {
                        updateCardModelStatusInList(pcModel, "WOL Error: No MAC", "");
                    }
                }
            } else {
                networkService.executePcCommand(hostname, currentAction, commandCallback);
            }
        }
    }

    private void performRefreshAllStatus(boolean showMessage) {
        if (!isServiceBound || networkService == null) {
            if (showMessage) Log.w(TAG, "Η υπηρεσία δικτύου δεν είναι έτοιμη για ανανέωση.");
            return;
        }
        if (showMessage) {
            appendToConsole(getString(R.string.refreshing_all_status));
        }
        for (CardModel model : cardModelArrayList) {
            networkService.executePcCommand(model.getHostname(), Constants.ACTION_ECHO, commandCallback);
        }
    }

    private void appendToConsole(String message) {
        AppExecutors.getInstance().runOnMainThread(() -> {
            if (consoleOutputTV != null) {
                consoleOutputTV.append(message + "\n");
            }
            Log.d(TAG, "Console: " + message);
        });
    }

    private CardModel findCardModelByHostname(String hostname) {
        for (CardModel model : cardModelArrayList) {
            if (model.getHostname().equals(hostname)) {
                return model;
            }
        }
        return null;
    }

    private void updateCardModelStatusInList(CardModel model, String status, String os) {
        if (model == null) return;
        AppExecutors.getInstance().runOnMainThread(() -> {
            model.setStatus(status);
            model.setOs(os);
            int position = cardModelArrayList.indexOf(model);
            if (position != -1 && cardAdapter != null) {
                cardAdapter.notifyItemChanged(position);
            } else if (cardAdapter != null) {
                Log.w(TAG, "CardModel not found in list for update or adapter is null, refreshing all.");
                cardAdapter.notifyDataSetChanged();
            }
        });
    }

    private final NetworkService.WolCallback wolCallback = new NetworkService.WolCallback() {
        @Override
        public void onWolSent(String macAddress) {
            appendToConsole(String.format(getString(R.string.wol_packet_sent_to_mac), macAddress));
        }
        @Override
        public void onError(String macAddress, String errorDetails, String localizedError) {
            Log.e(TAG, String.format(getString(R.string.wol_error_for_mac), macAddress, localizedError, errorDetails));
        }
    };

    private final NetworkService.CommandCallback commandCallback = new NetworkService.CommandCallback() {
        private final List<String> errorStates = Arrays.asList("error", "offline", "timeout", "no response", "empty response");

        @Override
        public void onResponse(String hostname, String rawResponse, String derivedStatus, String derivedOs) {
            String statusLower = derivedStatus.toLowerCase();
            boolean isError = errorStates.stream().anyMatch(statusLower::contains);

            if (!isError) {
                String message = (rawResponse != null) ? hostname + ": " + rawResponse.trim() : hostname + ": " + "N/A";
                message += " -> Status: [" + derivedStatus + "], OS: [" + (derivedOs.isEmpty() ? "N/A" : derivedOs) + "]";
                appendToConsole(message);
            } else {
                Log.e(TAG, hostname + " Error Status: " + derivedStatus + " (Raw: " + rawResponse + ")");
            }

            CardModel model = findCardModelByHostname(hostname);
            if (model != null) {
                updateCardModelStatusInList(model, derivedStatus, derivedOs);
            }
        }

        @Override
        public void onProgress(String hostname, String progressMessage) {
            appendToConsole(hostname + " Progress: " + progressMessage.trim());
            CardModel model = findCardModelByHostname(hostname);
            if (model != null) {
                String statusFromProgress = progressMessage.contains("-") ? progressMessage.split("-",2)[1].trim() : progressMessage;
                updateCardModelStatusInList(model, statusFromProgress, model.getOs());
            }
        }

        @Override
        public void onError(String hostname, String errorMsg, String localizedErrorMsg) {
            Log.e(TAG, hostname + " Σφάλμα: " + localizedErrorMsg + " (Λεπτομέρειες: " + errorMsg + ")");
            CardModel model = findCardModelByHostname(hostname);
            if (model != null) {
                updateCardModelStatusInList(model, "Error: " + localizedErrorMsg, "");
            }
        }
    };
}