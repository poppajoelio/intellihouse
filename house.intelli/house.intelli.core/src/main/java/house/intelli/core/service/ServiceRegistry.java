package house.intelli.core.service;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceRegistry<S> {

	private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

	private final Class<S> serviceClass;

	private static final Map<Class<?>, ServiceRegistry<?>> serviceClass2ServiceProvider = new HashMap<>();
	private final CopyOnWriteArrayList<ServiceRegistryDelegate<S>> delegates = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<ServiceRegistryListener<S>> listeners = new CopyOnWriteArrayList<>();

	protected ServiceRegistry(final Class<S> serviceClass) {
		this.serviceClass = requireNonNull(serviceClass, "serviceClass");
		addDelegate(new ServiceLoaderServiceRegistryDelegate<>(serviceClass));
	}

	public static synchronized <S> ServiceRegistry<S> getInstance(final Class<S> serviceClass) {
		requireNonNull(serviceClass, "serviceClass");

		@SuppressWarnings("unchecked")
		ServiceRegistry<S> serviceProvider = (ServiceRegistry<S>) serviceClass2ServiceProvider.get(serviceClass);
		if (serviceProvider == null) {
			serviceProvider = new ServiceRegistry<>(serviceClass);
			serviceClass2ServiceProvider.put(serviceClass, serviceProvider);
		}
		return serviceProvider;
	}

	public Class<S> getServiceClass() {
		return serviceClass;
	}

	public void addDelegate(final ServiceRegistryDelegate<S> delegate) {
		requireNonNull(delegate, "delegate");
		logger.info("addDelegate: serviceClass={}, delegate={}", serviceClass.getName(), delegate);
		delegate.setServiceRegistry(this);
		delegates.add(delegate);
		fireServiceRegistryChanged();
	}

	public void removeDelegate(final ServiceRegistryDelegate<S> delegate) {
		requireNonNull(delegate, "delegate");
		logger.info("removeDelegate: serviceClass={}, delegate={}", serviceClass.getName(), delegate);
		delegates.remove(delegate);
		fireServiceRegistryChanged();
		delegate.setServiceRegistry(null);
	}

	public void addListener(final ServiceRegistryListener<S> listener) {
		requireNonNull(listener, "listener");
		listeners.add(listener);
	}

	public void removeListener(final ServiceRegistryListener<S> listener) {
		requireNonNull(listener, "listener");
		listeners.remove(listener);
	}

	protected void fireServiceRegistryEvent(ServiceRegistryEvent<S> event) {
		requireNonNull(event, "event");
		for (ServiceRegistryListener<S> listener : listeners)
			listener.onServiceRegistryChanged(event);
	}

	public void fireServiceRegistryChanged() {
		fireServiceRegistryEvent(new ServiceRegistryEvent<>(this));
	}

	public List<S> getServices() {
		final ArrayList<S> result = new ArrayList<>();
		for (final ServiceRegistryDelegate<S> delegate : delegates) {
			List<S> services = delegate.getServices();
			if (logger.isInfoEnabled()) {
				List<String> serviceClassNames = new ArrayList<>();
				for (S service : services)
					serviceClassNames.add(service.getClass().getName());

				logger.info("getServices: delegateClass={}, serviceClasses={}", delegate.getClass().getName(), serviceClassNames);
			}
			result.addAll(services);
		}

		result.trimToSize();
		return Collections.unmodifiableList(result);
	}

}
