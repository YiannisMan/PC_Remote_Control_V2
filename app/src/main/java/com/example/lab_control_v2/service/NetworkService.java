package com.example.lab_control_v2.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.lab_control_v2.utils.AppExecutors;
import com.example.lab_control_v2.utils.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class NetworkService extends Service {
    /*
   Αυτή είναι η κεντρική υπηρεσία (Service) της εφαρμογής για όλες τις δικτυακές λειτουργίες.
   Ο ρόλος της είναι να απομονώσει την πολυπλοκότητα της επικοινωνίας δικτύου (TCP για εντολές και UDP για Wake-on-LAN) από τις Activities και λειτουργεί στο παρασκήνιο.

   Δέχεται αιτήματα από την Activity2 για την αποστολή εντολών στους Η/Υ ή για την εκκίνηση τους (WOL).
   Εκτελεί όλες τις χρονοβόρες δικτυακές λειτουργίες σε background threads για να μην "παγώνει" το UI της εφαρμογής.
   Επιστρέφει τα αποτελέσματα (απαντήσεις, πρόοδο, σφάλματα) πίσω στην Activity2 χρησιμοποιώντας Callbacks.

   Παροχή Επικοινωνίας (Binding): Υλοποιεί τον μηχανισμό IBinder ώστε η Activity2 να μπορεί να "δεθεί" (bind) πάνω της και να αποκτήσει μια αναφορά (instance) για να καλεί τις μεθόδους της.
   Εκτέλεση TCP Εντολών: Αναλαμβάνει τη δημιουργία Socket, τη σύνδεση στον Η/Υ-στόχο (χρησιμοποιώντας τη σταθερή IP από την Constants.java), την αποστολή της εντολής (Echo, Restart, κλπ.), και τη λήψη της απάντησης.
   Εκτέλεση Wake-on-LAN: Αναλαμβάνει τη δημιουργία και την αποστολή του UDP "Magic Packet" στην broadcast διεύθυνση του δικτύου.
   Ασύγχρονη Λειτουργία: Χρησιμοποιεί την κλάση AppExecutors για να εκτελεί τις παραπάνω λειτουργίες σε background threads.
   Επικοινωνία Αποτελεσμάτων: Καλεί τις κατάλληλες μεθόδους των interfaces CommandCallback και WolCallback για να ενημερώσει την Activity2 όταν μια λειτουργία ολοκληρωθεί ή αποτύχει.
   * */
    private static final String TAG = "NetworkService";

    // --- Binding Mechanism ---
    private final IBinder localBinder = new LocalBinder();

    public class LocalBinder extends Binder
    {
        public NetworkService getService()
        {
            // Επιστρέφει την παρουσία του NetworkService ώστε οι clients να μπορούν να καλούν public μεθόδους
            return NetworkService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent)
    {
        ShowMessage("NetworkService: Someone binded");
        return localBinder;
    }
    // Καλείται όταν όλοι οι clients έχουν κάνει unbind από το service.
    @Override
    public boolean onUnbind(Intent intent) {
        ShowMessage("NetworkService: Someone Unbinded");
        return false;
    }
    // --- Τέλος Μηχανισμού Binding ---

    // --- Callbacks Interfaces  ---
    public interface CommandCallback
    {
        void onResponse(String hostname, String rawResponse, String derivedStatus, String derivedOs);
        void onProgress(String hostname, String progressMessage);
        void onError(String hostname, String errorMsg, String localizedErrorMsg);
    }

    public interface WolCallback
    {
        void onWolSent(String macAddress);
        void onError(String macAddress, String errorDetails, String localizedError);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ShowMessage("NetworkService: Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ShowMessage("NetworkService: Destroyed");
    }
    private void ShowMessage(String message) {
        Log.d(TAG, message);
        AppExecutors.getInstance().runOnMainThread(() -> Toast.makeText(NetworkService.this, message, Toast.LENGTH_SHORT).show());
    }

    /*
     * Εκτελεί μια εντολή TCP σε έναν Η/Υ.
     * hostname Το όνομα του Η/Υ-στόχου.
     * actionConstant Η σταθερά της ενέργειας (π.χ., Constants.ACTION_SHUTDOWN).
     * callback Το interface για την επιστροφή του αποτελέσματος στην καλούσα Activity.
     */
    public void executePcCommand(String hostname, String actionConstant, CommandCallback callback)
    {
        // Εκτέλεση σε background thread
        AppExecutors.getInstance().diskIO().submit(() -> {
            String serverCommand = commandConstantToServerString(actionConstant); // Μετατροπή της σταθεράς σε εντολή για τον server

            if (serverCommand.isEmpty())
            {
                if (callback != null)
                {
                    AppExecutors.getInstance().runOnMainThread(() -> callback.onError(hostname, "Invalid action: " + actionConstant, "Άκυρη ενέργεια."));
                }
                return;
            }
            String rawResponse = null;
            String derivedStatus = "Offline"; // Προεπιλεγμένη κατάσταση σε περίπτωση σφάλματος
            String derivedOs = "";

            String ipAddress = Constants.getIpForHostname(hostname); // Λήψη της IP από τις σταθερές
            if (ipAddress == null)
            {
                Log.e(TAG, "No IP Address found in Constants for hostname: " + hostname);
                if (callback != null)
                {
                    AppExecutors.getInstance().runOnMainThread(() -> callback.onError(hostname, "No IP Address configured", "Δεν βρέθηκε IP για το " + hostname));
                }
                return;
            }
            Log.d(TAG, "Attempting to connect to " + hostname + " via IP: " + ipAddress);

            try (Socket socket = new Socket())
            {
                // Χρήση της IP διεύθυνσης για τη δημιουργία του InetAddress
                InetAddress serverAddr = InetAddress.getByName(ipAddress); // Μετατροπή String IP σε InetAddress
                InetSocketAddress socketAddress = new InetSocketAddress(serverAddr, Constants.SERVER_PORT);
                socket.connect(socketAddress, Constants.TIMEOUT_MS); // Σύνδεση με timeout

                // Δημιουργία streams για αποστολή και λήψη δεδομένων
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(serverCommand); // Αποστολή της εντολής στον server
                Log.d(TAG, "Sent to " + hostname + " (" + ipAddress + "): " + serverCommand);

                // Ειδικός χειρισμός για την εντολή "Restore" που στέλνει δύο απαντήσεις
                if (serverCommand.equals("Restore"))
                {
                    String firstResponse = in.readLine(); // Ανάγνωση της πρώτης απάντησης ("Restoring...")
                    Log.d(TAG, "Receive (1/2 Restore) " + hostname + ": " + firstResponse);

                    if (callback != null)
                    {
                        String progMsg;
                        if(firstResponse != null)
                        {
                            progMsg = firstResponse;
                        }
                        else
                        {
                            progMsg = hostname + "- Restoring...";
                        }
                        AppExecutors.getInstance().runOnMainThread(() -> callback.onProgress(hostname, progMsg)); // Ενημέρωση προόδου
                    }
                    rawResponse = in.readLine(); // Ανάγνωση της μοναδικής απάντησης για τις άλλες εντολές
                    Log.d(TAG, "Receive (2/2 Restore) " + hostname + ": " + rawResponse);

                } else
                {
                    rawResponse = in.readLine();
                    Log.d(TAG, "Receive from " + hostname + ": " + rawResponse);
                }

                // Επεξεργασία της απάντησης
                if (rawResponse != null && !rawResponse.trim().isEmpty())
                {
                    String[] parts = rawResponse.split(" - ", 2); // Χωρισμός hostname - detail
                    String responseDetail;
                    if(parts.length > 1)
                    {
                        responseDetail = parts[1].trim();
                    }
                    else
                    {
                        responseDetail = "Online (No details)";
                    }

                    if (serverCommand.equals("Echo")) {
                        derivedStatus = "Online";
                        derivedOs = responseDetail; // Το YYY είναι το OS
                    } else {
                        derivedStatus = responseDetail;
                        derivedOs = ""; // Δεν γνωρίζουμε το OS μετά από άλλες εντολές
                    }
                } else {
                    // Αν είμαστε συνδεδεμένοι αλλά δεν λάβαμε απάντηση
                    if(socket.isConnected()) {
                        derivedStatus = "Empty Response";
                    } else {
                        derivedStatus = "No Response";
                    }
                }

                // Επιστροφή του αποτελέσματος μέσω του callback στο UI thread
                final String fDerivedStatus = derivedStatus;
                final String fDerivedOs = derivedOs;
                final String fRawResponse = rawResponse; // Η "ακατέργαστη" απάντηση από τον server

                if (callback != null)
                {
                    AppExecutors.getInstance().runOnMainThread(() -> callback.onResponse(hostname, fRawResponse, fDerivedStatus, fDerivedOs));
                }
            }
            catch (UnknownHostException e) // Αυτό μπορεί να συμβεί αν η IP είναι άκυρη μορφή, αν και δεν θα έπρεπε με τις σταθερές
            {
                Log.w(TAG, "UnknownHostException for IP: " + ipAddress + " (Hostname: " + hostname + ")", e);
                if (callback != null)
                {
                    AppExecutors.getInstance().runOnMainThread(() -> callback.onError(hostname, "Invalid IP Address format", "Μη έγκυρη μορφή IP."));
                }
            }
            catch (SocketTimeoutException e) // Σφάλμα timeout κατά τη σύνδεση
            {
                Log.w(TAG, "Timeout connecting to " + hostname + " (IP: " + ipAddress + ")", e);
                if (callback != null)
                {
                    AppExecutors.getInstance().runOnMainThread(() -> callback.onError(hostname, "Timeout", "Timeout σύνδεσης."));
                }
            }
            catch (IOException e) // Γενικό σφάλμα I/O
            {
                Log.e(TAG, "IOException with " + hostname + " (IP: " + ipAddress + ")", e);

                String errorMsg;
                if(e.getMessage() != null)
                {
                    errorMsg = e.getMessage();
                } else
                {
                    errorMsg = "IO Error";
                }
                if (callback != null) {
                    AppExecutors.getInstance().runOnMainThread(() -> callback.onError(hostname, errorMsg, "Σφάλμα επικοινωνίας."));
                }
            }
        });
    }

    private String commandConstantToServerString(String actionConstant)
    {
        switch (actionConstant)
        {
            case Constants.ACTION_ECHO: return "Echo";
            case Constants.ACTION_RESTART: return "Restart";
            case Constants.ACTION_SHUTDOWN: return "Shutdown";
            case Constants.ACTION_RESTORE: return "Restore";
            default: return "";
        }
    }

    /*
     * Στέλνει ένα WOL Magic Packet στην καθορισμένη MAC διεύθυνση.
     * macAddress Η MAC διεύθυνση του Η/Υ-στόχου.
     * callback Το interface για την επιστροφή του αποτελέσματος.
     */
    public void sendWolPacket(String macAddress, WolCallback callback)
    {
        // Εκτέλεση σε background thread
        AppExecutors.getInstance().diskIO().submit(() -> {
            try {
                byte[] macBytes = getMacBytes(macAddress); // Λήψη των bytes της MAC
                byte[] bytes = new byte[6 + 16 * macBytes.length]; // Δημιουργία του Magic Packet (6x FF, ακολουθούμενο από 16x MAC bytes)

                for (int i = 0; i < 6; i++) {
                    bytes[i] = (byte) 0xFF;
                }

                for (int i = 6; i < bytes.length; i += macBytes.length) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
                }

                InetAddress address = InetAddress.getByName(Constants.BROADCAST_IP_WOL); // Η broadcast IP
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, Constants.WOL_PORT); // Το UDP packet

                try (DatagramSocket socket = new DatagramSocket())
                {
                    socket.setBroadcast(true); // Επιτρέπει την αποστολή broadcast
                    socket.send(packet); // Αποστολή του πακέτου
                }

                Log.d(TAG, "Wake-on-LAN packet sent for MAC: " + macAddress);
                if (callback != null) {
                    AppExecutors.getInstance().runOnMainThread(() -> callback.onWolSent(macAddress)); // Ενημέρωση επιτυχίας
                }
            }
            catch (IllegalArgumentException e) // Σφάλμα από την getMacBytes
            {
                Log.e(TAG, "Invalid MAC for WOL: " + macAddress, e);
                if (callback != null)
                {
                    AppExecutors.getInstance().runOnMainThread(() -> callback.onError(macAddress, "Invalid MAC: " + e.getMessage(), e.getMessage()));
                }
            }
            catch (Exception e) // Άλλα σφάλματα (π.χ. network)
            {
                Log.e(TAG, "Failed to send Wake-on-LAN packet: " + macAddress, e);
                String errorMsg;
                if(e.getMessage() != null) {
                    errorMsg = e.getMessage();
                }
                else
                {
                    errorMsg = "Unknown WOL error";
                }
                AppExecutors.getInstance().runOnMainThread(() -> callback.onError(macAddress, "Network error: " + errorMsg, "Σφάλμα δικτύου WOL."));
            }
        });
    }

    private byte[] getMacBytes(String macStr) throws IllegalArgumentException
    {
        if (macStr == null || macStr.trim().isEmpty())
        {
            throw new IllegalArgumentException("Η διεύθυνση MAC δεν μπορεί να είναι κενή.");
        }

        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");

        if (hex.length != 6)
        {
            throw new IllegalArgumentException("Invalid MAC address.");
        }

        try {
            for (int i = 0; i < 6; i++)
            {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid hex digit in MAC address: " + e.getMessage());
        }
        return bytes;
    }
}
