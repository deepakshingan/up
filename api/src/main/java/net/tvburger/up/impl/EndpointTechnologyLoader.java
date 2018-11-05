package net.tvburger.up.impl;

import net.tvburger.up.spi.ProtocolLifecycleManager;
import net.tvburger.up.spi.ProtocolManager;

import java.io.IOException;
import java.util.*;

public class EndpointTechnologyLoader {

    private final Map<Class<? extends ProtocolManager>, ProtocolLifecycleManager<? extends ProtocolManager>> lifecycleManagers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void init() {
        ServiceLoader<ProtocolLifecycleManager<?>> serviceLoader = ServiceLoader.load((Class) ProtocolLifecycleManager.class);
        for (ProtocolLifecycleManager<?> lifecycleManager : serviceLoader) {
            initLifecycleManager(lifecycleManager);
            lifecycleManagers.put(lifecycleManager.getProtocolType(), lifecycleManager);
        }
    }

    private void initLifecycleManager(ProtocolLifecycleManager<?> lifecycleManager) {
        try {
            lifecycleManager.init();
            lifecycleManager.start();
        } catch (IOException cause) {
            throw new IllegalStateException(cause);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends ProtocolManager> ProtocolLifecycleManager<P> get(Class<P> protocolType) {
        return (ProtocolLifecycleManager<P>) lifecycleManagers.computeIfAbsent(protocolType, this::loadLifecycleManager);
    }

    public Set<Class<? extends ProtocolManager>> getProtocols() {
        return lifecycleManagers.keySet();
    }

    @SuppressWarnings("unchecked")
    private ProtocolLifecycleManager<?> loadLifecycleManager(Class<?> protocolType) {
        ServiceLoader<ProtocolLifecycleManager<?>> serviceLoader = ServiceLoader.load((Class) ProtocolLifecycleManager.class);
        Iterator<ProtocolLifecycleManager<?>> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            ProtocolLifecycleManager<?> lifecycleManager = iterator.next();
            if (lifecycleManager.getProtocolType().equals(protocolType)) {
                initLifecycleManager(lifecycleManager);
                return lifecycleManager;
            }
        }
        throw new IllegalArgumentException();
    }

    public void destroy() {
        for (Class<? extends ProtocolManager> protocolType : getProtocols()) {
            try {
                get(protocolType).destroy();
            } catch (IOException cause) {
            }
        }
        lifecycleManagers.clear();
    }

}
