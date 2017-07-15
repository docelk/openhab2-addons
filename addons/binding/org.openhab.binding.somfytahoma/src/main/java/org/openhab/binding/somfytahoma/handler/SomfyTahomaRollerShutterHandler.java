package org.openhab.binding.somfytahoma.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.somfytahoma.SomfyTahomaBindingConstants.CONTROL;
import static org.openhab.binding.somfytahoma.SomfyTahomaBindingConstants.POSITION;

/**
 * Created by Ondrej Pecta on 14.07.2017.
 */
public class SomfyTahomaRollerShutterHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SomfyTahomaRollerShutterHandler.class);

    public SomfyTahomaRollerShutterHandler(Thing thing) {
        super(thing);
    }

    SomfyTahomaBridgeHandler bridge = null;

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("Received command {} for channel {}", command, channelUID);
        if (!channelUID.getId().equals(POSITION) && !channelUID.getId().equals(CONTROL)) {
            return;
        }

        String url = getThing().getConfiguration().get("url").toString();
        if (command.equals(RefreshType.REFRESH)) {
            bridge.updateRollerShutterState(channelUID, url);
        } else {
            String cmd = getTahomaCommand(command.toString());
            //Check if the rollershutter is not moving
            String executionId = bridge.getCurrentExecutions(url);
            if (executionId != null) {
                //STOP command should be interpreted if rollershutter moving
                //otherwise do nothing
                if (cmd.equals("my")) {
                    bridge.cancelExecution(executionId);
                }
            } else {
                String param = cmd.equals("setClosure") ? "[" + command.toString() + "]" : "[]";
                bridge.sendCommand(url, cmd, param);
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

    private String getTahomaCommand(String command) {

        switch (command) {
            case "DOWN":
                return "down";
            case "UP":
                return "up";
            case "STOP":
                return "my";
            default:
                return "setClosure";
        }
    }

}
