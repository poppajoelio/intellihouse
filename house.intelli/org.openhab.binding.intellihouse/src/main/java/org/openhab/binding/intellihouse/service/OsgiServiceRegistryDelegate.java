package org.openhab.binding.intellihouse.service;

import static house.intelli.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import house.intelli.core.service.AbstractServiceRegistryDelegate;
import house.intelli.core.service.ServiceRegistry;

public class OsgiServiceRegistryDelegate<S> extends AbstractServiceRegistryDelegate<S> {

    private final Class<S> serviceClass;
    private final BundleContext bundleContext;
    private ServiceListener serviceListener;

    public OsgiServiceRegistryDelegate(Class<S> serviceClass, BundleContext bundleContext) {
        this.serviceClass = assertNotNull(serviceClass, "serviceClass");
        this.bundleContext = assertNotNull(bundleContext, "bundleContext");
    }

    @Override
    public void setServiceRegistry(ServiceRegistry<S> serviceRegistry) {
        super.setServiceRegistry(serviceRegistry);
        hookListener();
    }

    @Override
    public List<S> getServices() {
        try {
            List<S> rpcServices = new ArrayList<>();
            Collection<ServiceReference<S>> serviceReferences = bundleContext.getServiceReferences(serviceClass, null);
            for (ServiceReference<S> serviceReference : serviceReferences) {
                S service = bundleContext.getService(serviceReference);
                if (service != null) {
                    rpcServices.add(service);
                }
            }
            return rpcServices;
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void serviceChanged(ServiceEvent event) {
        // sub-classes may override to get notified about this.
        // but they MUST call the super-method!
        final ServiceRegistry<S> serviceRegistry = getServiceRegistry();
        if (serviceRegistry != null) {
            serviceRegistry.fireServiceRegistryChanged();
        }
    }

    protected void hookListener() {
        synchronized (this) {
            if (serviceListener != null) {
                return; // already hooked! hook only once!
            }
            serviceListener = event -> OsgiServiceRegistryDelegate.this.serviceChanged(event);
        }
        boolean unhook = true;
        try {
            final String filter = String.format("(objectclass=%s)", serviceClass.getName());
            bundleContext.addServiceListener(serviceListener, filter);
            unhook = false;
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        } finally {
            if (unhook) {
                unhookListener();
            }
        }
    }

    protected void unhookListener() {
        ServiceListener sl;
        synchronized (this) {
            sl = serviceListener;
            serviceListener = null;
        }
        if (sl != null) { // maybe not hooked! unhook only once.
            bundleContext.removeServiceListener(sl);
        }
    }

    @Override
    public void close() {
        unhookListener();
        super.close();
    }
}