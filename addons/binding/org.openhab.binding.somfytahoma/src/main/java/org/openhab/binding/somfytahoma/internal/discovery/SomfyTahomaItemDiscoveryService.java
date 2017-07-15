package org.openhab.binding.somfytahoma.internal.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.somfytahoma.handler.SomfyTahomaBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.openhab.binding.somfytahoma.SomfyTahomaBindingConstants.*;

/**
 * Created by Ondrej Pecta on 14.7.2017.
 */
public class SomfyTahomaItemDiscoveryService extends AbstractDiscoveryService implements ExtendedDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(SomfyTahomaItemDiscoveryService.class);
    private SomfyTahomaBridgeHandler bridge = null;
    private DiscoveryServiceCallback discoveryServiceCallback;


    private static final int DISCOVERY_TIMEOUT_SEC = 10;

    public SomfyTahomaItemDiscoveryService(SomfyTahomaBridgeHandler bridgeHandler) {
        super(DISCOVERY_TIMEOUT_SEC);
        logger.info("Creating discovery service");
        this.bridge = bridgeHandler;
        bridgeHandler.setDiscoveryService(this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return new HashSet<>(Arrays.asList(THING_TYPE_ROLLERSHUTTER, THING_TYPE_ACTIONGROUP));
    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {
        this.discoveryServiceCallback = discoveryServiceCallback;
    }

    @Override
    protected void startScan() {
        logger.debug("Starting scanning for items...");
        bridge.setDiscoveryService(this);
        bridge.startDiscovery();
    }

    public void rollershutterDiscovered(String label, String deviceURL, String oid) {
        Map<String, Object> properties = new HashMap<>(1);
        properties.put("url", deviceURL);

        ThingUID thingUID = new ThingUID(BINDING_ID, "rollershutter", oid);

        if (discoveryServiceCallback.getExistingThing(thingUID) == null) {
            logger.info("Detected a rollershutter - label: {} oid: {}", label, oid);
            thingDiscovered(
                    DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_ROLLERSHUTTER).withProperties(properties)
                            .withRepresentationProperty("url").withLabel(label)
                            .withBridge(bridge.getThing().getUID()).build());
        }
    }

    public void actionGroupDiscovered(String label, String deviceURL, String oid) {
        Map<String, Object> properties = new HashMap<>(1);
        properties.put("url", deviceURL);

        ThingUID thingUID = new ThingUID(BINDING_ID, "actiongroup", oid);

        if (discoveryServiceCallback.getExistingThing(thingUID) == null) {
            logger.info("Detected an action group - label: {} oid: {}", label, oid);
            thingDiscovered(
                    DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_ACTIONGROUP).withProperties(properties)
                            .withRepresentationProperty("url").withLabel(label)
                            .withBridge(bridge.getThing().getUID()).build());
        }
    }

}
