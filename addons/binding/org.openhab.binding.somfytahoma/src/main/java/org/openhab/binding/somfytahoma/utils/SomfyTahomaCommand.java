package org.openhab.binding.somfytahoma.utils;

import java.util.ArrayList;

/**
 * Created by Ondrej Pecta on 14.07.2017.
 */
public class SomfyTahomaCommand {

    String command;
    ArrayList<String> params = new ArrayList<String>();

    public SomfyTahomaCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public ArrayList<String> getParams() {
        return params;
    }

    public void addParam(String param) {
        this.params.add(param);
    }
}

