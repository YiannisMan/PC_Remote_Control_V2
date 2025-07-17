package com.example.lab_control_v2.utils;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    /*
    Παρέχει ένα κεντρικό σημείο για την αποθήκευση όλων των σταθερών τιμών που χρησιμοποιούνται σε διάφορα μέρη της εφαρμογής.

    Ορίζει σταθερές όπως αριθμούς πόρτας, timeouts, προθέματα ονομάτων, κλειδιά για Intent extras, σταθερές για τύπους ενεργειών,
    και τη λίστα των MAC διευθύνσεων.
    * */

    public static final int SERVER_PORT = 41007;
    public static final int TIMEOUT_MS = 3000;
    public static final String PC_NAME_PREFIX = "PRPC";
    public static final int NUM_PCS = 1;

    // Σταθερές για τη λειτουργία Wake-on-LAN (WOL)
    public static final String BROADCAST_IP_WOL = "255.255.255.255"; // Γενική broadcast.
    public static final int WOL_PORT = 9;

    // Σταθερές που αντιπροσωπεύουν τις ενέργειες
    public static final String ACTION_KEY = "SELECTED_ACTION";
    public static final String ACTION_ECHO = "ACTION_ECHO";
    public static final String ACTION_RESTART = "ACTION_RESTART";
    public static final String ACTION_SHUTDOWN = "ACTION_SHUTDOWN";
    public static final String ACTION_RESTORE = "ACTION_RESTORE";
    public static final String ACTION_WOL = "ACTION_WOL";

    //Επιστρέφει έναν χάρτη με τα hostnames των υπολογιστών και τις αντίστοιχες MAC διευθύνσεις τους.
    private static final Map<String, String> MAC_ADDRESSES_MAP;
    // Χάρτης για τις IP διευθύνσεις
    private static final Map<String, String> IP_ADDRESSES_MAP;

    static {
        // Αρχικοποίηση MAC Addresses
        MAC_ADDRESSES_MAP = new HashMap<>();
        MAC_ADDRESSES_MAP.put("PRPC01", "74:56:3C:71:32:63");

        // Αρχικοποίηση IP Addresses
        IP_ADDRESSES_MAP = new HashMap<>();
        IP_ADDRESSES_MAP.put("PRPC01", "192.168.1.63");
    }
    public static Map<String, String> getMacAddresses()
    {
        return new HashMap<>(MAC_ADDRESSES_MAP); // Επιστροφή αντιγράφου
    }

    public static String getMacAddress(String hostname)
    {
        if (hostname == null)
        {
            return null;
        }
        return MAC_ADDRESSES_MAP.get(hostname);
    }

    // Η μέθοδος που έχουμε για τις IPs (επιστρέφει ολόκληρο τον χάρτη)
    public static Map<String, String> getIpAddresses()
    {
        return new HashMap<>(IP_ADDRESSES_MAP); // Επιστροφή αντιγράφου
    }

    // Βοηθητική μέθοδος για λήψη συγκεκριμένης IP
    public static String getIpForHostname(String hostname)
    {
        if (hostname == null) {
            return null;
        }
        return IP_ADDRESSES_MAP.get(hostname);
    }
}