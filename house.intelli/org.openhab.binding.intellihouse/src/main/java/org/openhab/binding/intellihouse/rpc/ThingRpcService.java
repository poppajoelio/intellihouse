package org.openhab.binding.intellihouse.rpc;

import static house.intelli.core.util.AssertUtil.assertNotNull;
import static org.openhab.binding.intellihouse.IntelliHouseBindingConstants.THING_CONFIG_KEY_HOST_ID;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.intellihouse.IntelliHouseActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;

public abstract class ThingRpcService<REQ extends Request<RES>, RES extends Response>
        extends AbstractRpcService<REQ, RES> {

    private ThingRegistry thingRegistry;
    private EventPublisher eventPublisher;

    protected ThingRegistry getThingRegistry() {
        if (thingRegistry == null) {
            thingRegistry = getServiceOrFail(ThingRegistry.class);
        }
        return thingRegistry;
    }

    protected EventPublisher getEventPublisher() {
        if (eventPublisher == null) {
            eventPublisher = getServiceOrFail(EventPublisher.class);
        }
        return eventPublisher;
    }

    protected Set<Thing> getThings(final ThingTypeUID thingTypeUID, final REQ request) {
        assertNotNull(request, "request");
        return getThings(thingTypeUID, assertNotNull(request.getClientHostId(), "request.clientHostId"));
    }

    protected Set<Thing> getThings(final ThingTypeUID thingTypeUID, final HostId hostId) {
        assertNotNull(hostId, "hostId");
        final ThingRegistry thingRegistry = getThingRegistry();
        final Set<Thing> result = new LinkedHashSet<>();
        final String hostIdStr = hostId.toString();
        for (final Thing thing : thingRegistry.getAll()) {
            if (thingTypeUID != null && !thingTypeUID.equals(thing.getThingTypeUID())) {
                continue;
            }
            final String hid = (String) thing.getConfiguration().get(THING_CONFIG_KEY_HOST_ID);
            if (hostIdStr.equals(hid)) {
                result.add(thing);
            }
        }
        return result;
    }

    public BundleContext getBundleContext() {
        return IntelliHouseActivator.getInstance().getBundleContext();
    }

    protected <S> S getServiceOrFail(final Class<S> serviceClass) {
        assertNotNull(serviceClass, "serviceClass");
        final BundleContext bundleContext = assertNotNull(getBundleContext(), "bundleContext");
        ServiceReference<S> serviceReference = bundleContext.getServiceReference(serviceClass);
        if (serviceReference == null) {
            throw new IllegalStateException("No ServiceReference found for: " + serviceClass.getName());
        }
        S service = bundleContext.getService(serviceReference);
        if (service == null) {
            throw new IllegalStateException("ServiceReference did not point to existing service: " + serviceReference);
        }
        return service;
    }
}