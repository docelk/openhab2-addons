package org.openhab.binding.somfytahoma.handler;

import com.google.gson.Gson;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.somfytahoma.utils.SomfyTahomaAction;
import org.openhab.binding.somfytahoma.utils.SomfyTahomaCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static org.openhab.binding.somfytahoma.SomfyTahomaBindingConstants.STARTER;

/**
 * Created by Ondrej Pecta on 14.07.2017.
 */
public class SomfyTahomaActionGroupHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SomfyTahomaActionGroupHandler.class);

    public SomfyTahomaActionGroupHandler(Thing thing) {
        super(thing);
    }

    SomfyTahomaBridgeHandler bridge = null;

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(STARTER) && command instanceof OnOffType) {
            if ("ON".equals(command.toString())) {
                String url = getThing().getConfiguration().get("url").toString();
                ArrayList<SomfyTahomaAction> actions = bridge.getTahomaActions(url);
                for (SomfyTahomaAction action : actions) {
                    sendCommand(action);
                }
            }
        }
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        bridge = (SomfyTahomaBridgeHandler) this.getBridge().getHandler();
        updateStatus(ThingStatus.ONLINE);
    }

    private void sendCommand(SomfyTahomaAction action) {

        Gson gson = new Gson();
        for (SomfyTahomaCommand command : action.getCommands()) {
            logger.debug("Sending to device {} command {} params {}", action.getDeviceURL(), command.getCommand(), gson.toJson(command.getParams()));
            bridge.sendCommand(action.getDeviceURL(), command.getCommand(), gson.toJson(command.getParams()));
        }
    }

}
