package com.example.lab_control_v2.model;

public class CardModel {
    private String hostname;
    private String os;
    private String status;
    private boolean isSelected;

    public CardModel(String hostname, String os, String status) {
        this.hostname = hostname;
        this.os = os;
        this.status = status;
        this.isSelected = false;
    }

    public String getHostname() {
        return hostname;
    }
    public String getOs() {
        return os;
    }
    public String getStatus() {
        return status;
    }
    public boolean isSelected() {
        return isSelected;
    }

    public void setOs(String os) {
        this.os = os;
    }
    public void setStatus(String status) { this.status = status; }
    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}