package com.example.lab_control_v2.repository;

import com.example.lab_control_v2.model.CardModel;
import com.example.lab_control_v2.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PcRepository {
    /*
    Λειτουργεί ως πηγή δεδομένων για τις πληροφορίες των υπολογιστών, κυρίως για την αρχική λίστα των ονομάτων τους
    και τις MAC διευθύνσεις τους.

    Κατά την αρχικοποίησή του, φορτώνει τις MAC διευθύνσεις ( καλώντας την Constants.getMacAddresses()).
    Δημιουργεί την αρχική λίστα των αντικειμένων PcStatus (ή θα μπορούσε να δημιουργεί CardModel αντικείμενα, ανάλογα με την υλοποίηση)
    με βάση το Constants.PC_NAME_PREFIX και Constants.NUM_PCS.
    * */
    private List<CardModel> pcListCache;
    private Map<String, String> macAddresses;
    private Map<String, String> ipAddresses;

    public PcRepository() {
        macAddresses = Constants.getMacAddresses();
        ipAddresses = Constants.getIpAddresses();
        initializePcList();
    }

    private void initializePcList() {
        pcListCache = new ArrayList<>();
        for (int i = 1; i <= Constants.NUM_PCS; i++)
        {
            String hostname = String.format("%s%02d", Constants.PC_NAME_PREFIX, i);
            pcListCache.add(new CardModel(hostname, "", "Άγνωστη"));
        }
    }

    public String getMacAddress(String hostname)
    {
        if (hostname == null)
        {
            return null;
        }
        return macAddresses.get(hostname);
    }
}