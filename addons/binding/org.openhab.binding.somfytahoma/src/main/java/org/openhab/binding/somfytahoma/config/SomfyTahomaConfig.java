package org.openhab.binding.somfytahoma.config;

/**
 * Created by Ondrej Pecta on 14.7.2017.
 */
public class SomfyTahomaConfig {
    private String email;
    private String password;
    private String thingUid;
    private int refresh = 30;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getThingUid() {
        return thingUid;
    }

    public void setThingUid(String thingUid) {
        this.thingUid = thingUid;
    }

    public int getRefresh() {
        return refresh;
    }
}
