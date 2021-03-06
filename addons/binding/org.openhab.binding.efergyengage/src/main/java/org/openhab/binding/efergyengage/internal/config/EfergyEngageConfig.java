package org.openhab.binding.efergyengage.internal.config;

/**
 * Created by Ondřej Pečta on 12. 8. 2016.
 */
public class EfergyEngageConfig {

    private String email;
    private String password;
    private String device;
    private int utcOffset;
    private int refresh;

    public int getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(int utcOffset) {
        this.utcOffset = utcOffset;
    }

    private String thingUid;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRefresh() {
        return refresh;
    }

    public void setRefresh(int refresh) {
        this.refresh = refresh;
    }


    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getThingUid() {
        return thingUid;
    }

    public void setThingUid(String thingUid) {
        this.thingUid = thingUid;
    }
}
