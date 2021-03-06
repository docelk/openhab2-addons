/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.somfytahoma.internal;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.somfytahoma.handler.SomfyTahomaActionGroupHandler;
import org.openhab.binding.somfytahoma.handler.SomfyTahomaBridgeHandler;
import org.openhab.binding.somfytahoma.handler.SomfyTahomaRollerShutterHandler;
import org.openhab.binding.somfytahoma.internal.discovery.SomfyTahomaItemDiscoveryService;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.openhab.binding.somfytahoma.SomfyTahomaBindingConstants.*;

/**
 * The {@link SomfyTahomaHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class SomfyTahomaHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(SomfyTahomaHandlerFactory.class);
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(Arrays.asList(THING_TYPE_BRIDGE, THING_TYPE_ROLLERSHUTTER, THING_TYPE_ACTIONGROUP));

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();


    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        logger.debug("Creating handler for {}", thing.getThingTypeUID().getId());
        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            SomfyTahomaBridgeHandler handler = new SomfyTahomaBridgeHandler((Bridge) thing);
            registerItemDiscoveryService(handler);
            return handler;
        }
        if (thingTypeUID.equals(THING_TYPE_ROLLERSHUTTER)) {
            return new SomfyTahomaRollerShutterHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_ACTIONGROUP)) {
            return new SomfyTahomaActionGroupHandler(thing);
        }
        return null;
    }


    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof SomfyTahomaBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                SomfyTahomaItemDiscoveryService service = (SomfyTahomaItemDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    private synchronized void registerItemDiscoveryService(SomfyTahomaBridgeHandler bridgeHandler) {
        SomfyTahomaItemDiscoveryService discoveryService = new SomfyTahomaItemDiscoveryService(bridgeHandler);
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));

    }
}
