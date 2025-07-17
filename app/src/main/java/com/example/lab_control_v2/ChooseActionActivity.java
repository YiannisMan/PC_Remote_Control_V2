package com.example.lab_control_v2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lab_control_v2.utils.Constants;
import com.example.lab_control_v2.utils.SoundPlayer;

public class ChooseActionActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_action);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.idEcho).setOnClickListener(this);
        findViewById(R.id.idRestart).setOnClickListener(this);
        findViewById(R.id.idShutdown).setOnClickListener(this);
        findViewById(R.id.idRestore).setOnClickListener(this);
        findViewById(R.id.idWOL).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(getApplicationContext(), PcListActivity.class); // Intent για το Activity2
        String action = null; // Η ενέργεια που θα σταλεί στο Activity2
        int id = view.getId(); // Παίρνουμε το ID του κουμπιού που πατήθηκε

        SoundPlayer.playPop(this);

        // Αντιστοίχιση του ID του κουμπιού με τη σταθερά της ενέργειας
        if (id == R.id.idEcho)
        {
            action = Constants.ACTION_ECHO;
        }
        else if (id == R.id.idRestart)
        {
            action = Constants.ACTION_RESTART;
        }
        else if (id == R.id.idShutdown)
        {
            action = Constants.ACTION_SHUTDOWN;
        }
        else if (id == R.id.idRestore)
        {
            action = Constants.ACTION_RESTORE;
        }
        else if (id == R.id.idWOL) {
            action = Constants.ACTION_WOL;
        }
        // Αν βρέθηκε μια έγκυρη ενέργεια, την προσθέτουμε στο Intent και ξεκινάμε την Activity2
        if (action != null) {
            intent.putExtra(Constants.ACTION_KEY, action); // Προσθήκη της ενέργειας ως extra
            startActivity(intent); // Εκκίνηση της Activity2
        }
    }
}