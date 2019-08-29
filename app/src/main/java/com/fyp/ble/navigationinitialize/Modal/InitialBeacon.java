package com.fyp.ble.navigationinitialize.Modal;

public class InitialBeacon {
    private String MAC;
    private String description;

    public InitialBeacon() {
    }

    public String getMAC() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InitialBeacon(String MAC, String description) {
        this.MAC = MAC;
        this.description = description;
    }
}
