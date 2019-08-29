package com.fyp.ble.navigationinitialize.Modal;

public class Beacon {
    private String MAC;
    private String description;
    private String location;

    public Beacon() {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Beacon(String MAC, String description, String location) {
        this.MAC = MAC;
        this.description = description;
        this.location = location;
    }
}
