package com.example.lab_control_v2.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    /*
    Παρέχει έναν απλό μηχανισμό για τη διαχείριση των threads της εφαρμογής.

    Δημιουργεί και διαχειρίζεται ένα thread pool για την εκτέλεση εργασιών στο παρασκήνιο (background I/O operations, όπως οι κλήσεις δικτύου)
    και έναν Handler για την εκτέλεση κώδικα στο κύριο UI thread.
    * */
    private static final Object LOCK = new Object();
    private static AppExecutors sInstance;
    private final ExecutorService diskIO;
    private final Handler mainThreadHandler; // Handler για το UI thread

    //private constructor για την υλοποίηση του Singleton
    private AppExecutors(ExecutorService diskIO, Handler mainThreadHandler) {
        this.diskIO = diskIO;
        this.mainThreadHandler = mainThreadHandler;
    }

    /*
    Επιστρέφει τη μοναδική παρουσία (instance) της κλάσης AppExecutors.
    Δημιουργεί την παρουσία αν δεν υπάρχει ήδη (thread-safe).
    * */
    public static AppExecutors getInstance() {
        if (sInstance == null)
        {
            synchronized (LOCK) // Κλείδωμα για ασφάλεια thread κατά την πρώτη δημιουργία
            {
                if (sInstance == null)
                {
                    sInstance = new AppExecutors(Executors.newFixedThreadPool(3), // Pool με 3 threads
                            new Handler(Looper.getMainLooper())); // Handler για το κύριο thread
                }
            }
        }
        return sInstance;
    }

    //Επιστρέφει τον executor για εργασίες παρασκηνίου.
    public ExecutorService diskIO()
    {
        return diskIO;
    }

    //Εκτελεί ένα Runnable στο κύριο UI thread (το λέει και το όνομα τις μεθόδου)
    public void runOnMainThread(Runnable runnable)
    {
        mainThreadHandler.post(runnable);
    }
}
