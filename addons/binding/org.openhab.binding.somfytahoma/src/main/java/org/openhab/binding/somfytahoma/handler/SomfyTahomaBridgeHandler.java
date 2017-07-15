/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.somfytahoma.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.somfytahoma.config.SomfyTahomaConfig;
import org.openhab.binding.somfytahoma.internal.discovery.SomfyTahomaItemDiscoveryService;
import org.openhab.binding.somfytahoma.utils.SomfyTahomaAction;
import org.openhab.binding.somfytahoma.utils.SomfyTahomaCommand;
import org.openhab.binding.somfytahoma.utils.SomfyTahomaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.somfytahoma.SomfyTahomaBindingConstants.*;

/**
 * The {@link SomfyTahomaBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class SomfyTahomaBridgeHandler extends ConfigStatusBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(SomfyTahomaBridgeHandler.class);

    private String cookie;
    private boolean loggedIn = false;

    /**
     * Future to poll for updated
     */
    private ScheduledFuture<?> pollFuture;

    /**
     * Our configuration
     */
    protected SomfyTahomaConfig thingConfig;

    //Gson parser
    private final JsonParser parser = new JsonParser();
    private SomfyTahomaItemDiscoveryService discoveryService = null;

    public SomfyTahomaBridgeHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(VERSION)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        String thingUid = getThing().getUID().toString();
        thingConfig = getConfigAs(SomfyTahomaConfig.class);
        thingConfig.setThingUid(thingUid);

        login();
        scheduler.schedule(() -> startDiscovery(), 1, TimeUnit.SECONDS);
        initPolling(thingConfig.getRefresh());
    }

    /**
     * starts this things polling future
     */
    private void initPolling(int refresh) {
        stopPolling();
        pollFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (loggedIn) {
                        updateTahomaStates();
                    }
                } catch (Exception e) {
                    logger.debug("Exception during poll : {}", e);
                }
            }
        }, 30, refresh, TimeUnit.SECONDS);

    }

    private void login() {
        String url = null;

        if ((thingConfig.getEmail() != null && thingConfig.getEmail().isEmpty()) || (thingConfig.getPassword() != null && thingConfig.getPassword().isEmpty())) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Can not access device as username and/or password are null");
            return;
        }

        try {
            url = TAHOMA_URL + "login";
            String urlParameters = "userId=" + thingConfig.getEmail() + "&userPassword=" + thingConfig.getPassword();
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            URL cookieUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            setConnectionDefaults(connection);
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(postData);
            }

            //get cookie
            String headerName;
            for (int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++) {
                if (headerName.equals("Set-Cookie")) {
                    cookie = connection.getHeaderField(i);
                    break;
                }
            }

            InputStream response = connection.getInputStream();
            String line = readResponse(response);

            JsonObject jobject = parser.parse(line).getAsJsonObject();
            boolean success = jobject.get("success").getAsBoolean();

            if (success) {
                String version = jobject.get(VERSION).getAsString();
                logger.debug("SomfyTahoma cookie: {}", cookie);
                logger.info("SomfyTahoma version: {}", version);

                for (Channel channel : getThing().getChannels()) {
                    if (channel.getUID().getId().equals(VERSION)) {
                        updateState(channel.getUID(), new StringType(version));
                    }
                }
                loggedIn = true;
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.debug("Login response: {}", line);
                loggedIn = false;
                throw new SomfyTahomaException(line);
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "The URL '" + url + "' is malformed: ");
        } catch (Exception e) {
            logger.error("Cannot get login cookie: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED, "Cannot get login cookie");
        }
    }

    @Override
    public void handleRemoval() {
        super.handleRemoval();
        logout();
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        return Collections.emptyList();
    }

    public void startDiscovery() {
        if (discoveryService != null) {
            listDevices();
            listActionGroups();
        }
    }

    @Override
    public void dispose() {
        stopPolling();
    }

    /**
     * Stops this thing's polling future
     */
    private void stopPolling() {
        if (pollFuture != null && !pollFuture.isCancelled()) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
    }


    private void listActionGroups() {
        String groups = getGroups();
        //StringBuilder sb = new StringBuilder();

        JsonObject jobject = parser.parse(groups).getAsJsonObject();
        JsonArray jactionGroups = jobject.get("actionGroups").getAsJsonArray();
        for (JsonElement jactionGroup : jactionGroups) {
            jobject = jactionGroup.getAsJsonObject();
            String oid = jobject.get("oid").getAsString();
            String label = jobject.get("label").getAsString();
            //actiongroups uses oid as deviceURL
            String deviceURL = oid;
            discoveryService.actionGroupDiscovered(label, deviceURL, oid);
        }
    }

    private String getGroups() {
        String url = null;

        try {
            url = TAHOMA_URL + "getActionGroups";
            String urlParameters = "";

            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            InputStream response = sendDataToTahomaWithCookie(url, postData);

            return readResponse(response);

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
        } catch (IOException e) {
            if (e.toString().contains(UNAUTHORIZED)) {
                loggedIn = false;
            }
            logger.error("Cannot send getActionGroups command: {}", e.toString());
        } catch (Exception e) {
            logger.error("Cannot send getActionGroups command: {}", e.toString());
        }
        return "";
    }

    private void listDevices() {
        String url = null;

        try {
            url = TAHOMA_URL + "getSetup";
            String urlParameters = "";
            //StringBuilder sb = new StringBuilder();

            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            InputStream response = sendDataToTahomaWithCookie(url, postData);

            String line = readResponse(response);

            JsonObject jobject = parser.parse(line).getAsJsonObject();
            jobject = jobject.get("setup").getAsJsonObject();
            for (JsonElement el : jobject.get("devices").getAsJsonArray()) {
                JsonObject obj = el.getAsJsonObject();
                if ("RollerShutter".equals(obj.get("uiClass").getAsString())) {
                    String label = obj.get("label").getAsString();
                    String deviceURL = obj.get("deviceURL").getAsString();
                    String oid = obj.get("oid").getAsString();
                    discoveryService.rollershutterDiscovered(label, deviceURL, oid);
                }
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
        } catch (IOException e) {
            if (e.toString().contains(UNAUTHORIZED)) {
                loggedIn = false;
            }
            logger.error("Cannot send listDevices command: {}", e.toString());
        } catch (Exception e) {
            logger.error("Cannot send listDevices command: {}", e.toString());
        }

    }

    public void setDiscoveryService(SomfyTahomaItemDiscoveryService somfyTahomaItemDiscoveryService) {
        this.discoveryService = somfyTahomaItemDiscoveryService;
    }

    private int getState(String io) {
        String url = null;

        logger.debug("Getting state for roller shutter: {}", io);
        try {
            url = TAHOMA_URL + "getStates";
            String urlParameters = "[{\"deviceURL\": \"" + io + "\", \"states\": [{\"name\": \"core:ClosureState\"}]}]";

            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            InputStream response = sendDataToTahomaWithCookie(url, postData);
            String line = readResponse(response);

            JsonObject jobject = parser.parse(line).getAsJsonObject();
            jobject = jobject.get("devices").getAsJsonArray().get(0).getAsJsonObject();
            jobject = jobject.get("states").getAsJsonArray().get(0).getAsJsonObject();
            int state = jobject.get("value").getAsInt();

            if (state >= 0) {
                logger.debug("State: {}", state);
                return state;
            } else {
                logger.debug("GetState response: {}", line);
                throw new SomfyTahomaException(line);
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
        } catch (IOException e) {
            if (e.toString().contains(UNAUTHORIZED)) {
                loggedIn = false;
                return -1;
            }
        } catch (Exception e) {
            logger.error("Cannot send getStates command: {}", e.toString());
        }

        return 0;
    }

    private void updateTahomaStates() {

        logger.debug("Updating Tahoma States...");
        for(Thing thing : getThing().getThings()) {
            logger.debug("Updating thing {} with UID {}", thing.getLabel(), thing.getThingTypeUID());
            if(!thing.getThingTypeUID().equals(THING_TYPE_ROLLERSHUTTER))
                continue;
            String url = thing.getConfiguration().get("url").toString();
            updateRollerShutterState(thing, url);
        }
    }

    public void updateRollerShutterState(Thing thing, String url) {
        int state = getState(url);
        if (state == -1) {
            //relogin
            login();
            state = getState(url);
        }
        for (Channel channel : thing.getChannels()) {
            updateState(channel.getUID(), new PercentType(state));
        }
    }

    public void updateRollerShutterState(ChannelUID channelUID, String url) {
        int state = getState(url);
        if (state == -1) {
            //relogin
            login();
            state = getState(url);
        }
        updateState(channelUID, new PercentType(state));

    }

    private void logout() {
        try {
            sendToTahomaWithCookie(TAHOMA_URL + "logout");
            cookie = "";
            loggedIn = false;
        } catch (Exception e) {
            logger.error("Cannot send logout command!");
        }
    }

    public static String readResponse(InputStream response) throws Exception {
        String line;
        StringBuilder body = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response));

        while ((line = reader.readLine()) != null) {
            body.append(line).append("\n");
        }
        line = body.toString();
        return line;
    }

    public InputStream sendToTahomaWithCookie(String url) throws Exception {

        URL cookieUrl = new URL(url);
        HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
        connection.setDoOutput(false);
        connection.setRequestMethod("GET");
        setConnectionDefaults(connection);
        connection.setRequestProperty("Cookie", cookie);

        return connection.getInputStream();
    }

    public InputStream sendDataToTahomaWithCookie(String url, byte[] postData) throws Exception {

        URL cookieUrl = new URL(url);
        HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        setConnectionDefaults(connection);
        connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
        connection.setRequestProperty("Cookie", cookie);
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.write(postData);
        }

        return connection.getInputStream();
    }

    public void setConnectionDefaults(HttpsURLConnection connection) {
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", TAHOMA_AGENT);
        connection.setRequestProperty("Accept-Language", "de-de");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setUseCaches(false);
    }

    public InputStream sendDeleteToTahomaWithCookie(String url) throws Exception {

        URL cookieUrl = new URL(url);
        HttpsURLConnection connection = (HttpsURLConnection) cookieUrl.openConnection();
        connection.setDoOutput(false);
        connection.setRequestMethod("DELETE");
        setConnectionDefaults(connection);
        connection.setRequestProperty("Cookie", cookie);

        return connection.getInputStream();
    }

    public void sendCommand(String io, String command, String params) {
        String url = null;

        try {
            url = TAHOMA_URL + "apply";

            String urlParameters = "{\"actions\": [{\"deviceURL\": \"" + io + "\", \"commands\": [{ \"name\": \"" + command + "\", \"parameters\": " + params + "}]}]}";
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            InputStream response = sendDataToTahomaWithCookie(url, postData);
            String line = readResponse(response);

            JsonObject jobject = parser.parse(line).getAsJsonObject();
            String execId = jobject.get("execId").getAsString();

            if (!"".equals(execId)) {
                logger.debug("Exec id: {}", execId);
            } else {
                logger.debug("Command response: {}", line);
                throw new SomfyTahomaException(line);
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
        } catch (IOException e) {
            if (e.toString().contains(UNAUTHORIZED)) {
                loggedIn = false;
            }
            logger.error("Cannot send apply command: {}", e.toString());
        } catch (Exception e) {
            logger.error("Cannot send apply command: {]", e.toString());
        }
    }

    public String getCurrentExecutions(String type) {
        String url = null;

        try {
            url = TAHOMA_URL + "getCurrentExecutions";

            String urlParameters = "";
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            InputStream response = sendDataToTahomaWithCookie(url, postData);
            String line = readResponse(response);

            JsonObject jobject = parser.parse(line).getAsJsonObject();
            JsonArray jarray = jobject.get("executions").getAsJsonArray();

            return parseExecutions(type, jarray);

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
        } catch (IOException e) {
            if (e.toString().contains(UNAUTHORIZED)) {
                loggedIn = false;
            }
            logger.error("Cannot send getCurrentExecutions command: {}", e.toString());
        } catch (Exception e) {
            logger.error("Cannot send getCurrentExecutions command: {}", e.toString());
        }

        return null;
    }

    private String parseExecutions(String type, JsonArray executions) {
        for (JsonElement execution : executions) {
            JsonObject jobject = execution.getAsJsonObject().get("actionGroup").getAsJsonObject();
            String execId = execution.getAsJsonObject().get("id").getAsString();
            JsonArray actions = jobject.get("actions").getAsJsonArray();
            for (JsonElement action : actions) {
                jobject = action.getAsJsonObject();
                if (jobject.get("deviceURL").getAsString().equals(type))
                    return execId;
            }
        }
        return null;
    }

    public void cancelExecution(String executionId) {
        String url = null;

        try {
            url = DELETE_URL + executionId;
            sendDeleteToTahomaWithCookie(url);

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: {}", url, e.toString());
        } catch (IOException e) {
            if (e.toString().contains(UNAUTHORIZED)) {
                loggedIn = false;
            }
            logger.error("Cannot cancel execution: {}", e.toString());
        } catch (Exception e) {
            logger.error("Cannot cancel execution: {}", e.toString());
        }
    }

    public ArrayList<SomfyTahomaAction> getTahomaActions(String actionGroup) {
        String groups = getGroups();
        ArrayList<SomfyTahomaAction> actions = new ArrayList<>();

        JsonObject jobject = parser.parse(groups).getAsJsonObject();
        JsonArray jactionGroups = jobject.get("actionGroups").getAsJsonArray();
        for (JsonElement jactionGroup : jactionGroups) {
            jobject = jactionGroup.getAsJsonObject();
            String oid = jobject.get("oid").getAsString();
            if (actionGroup.equals(oid)) {
                JsonArray jactions = jobject.get("actions").getAsJsonArray();
                for (JsonElement jactionElement : jactions) {
                    jobject = jactionElement.getAsJsonObject();
                    SomfyTahomaAction action = new SomfyTahomaAction();
                    action.setDeviceURL(jobject.get("deviceURL").getAsString());
                    JsonArray jcommands = jobject.get("commands").getAsJsonArray();
                    for (JsonElement jcommandElement : jcommands) {
                        JsonObject jcommand = jcommandElement.getAsJsonObject();
                        String name = jcommand.get("name").getAsString();
                        SomfyTahomaCommand cmd = new SomfyTahomaCommand(name);
                        for (JsonElement jparamElement : jcommand.get("parameters").getAsJsonArray()) {
                            cmd.addParam(jparamElement.getAsJsonObject().getAsString());
                        }
                        action.addCommand(cmd);
                    }

                    actions.add(action);
                }
                break;
            }
        }
        return actions;

    }
}