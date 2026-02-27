package com.kntrel.mc.territotem.test.mock;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.UnknownDependencyException;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.kntrel.mc.territotem.test.mock.Mock.unimplemented;

public class MockPluginManager implements PluginManager {

    private final Map<String, Permission> permissionsByName_ = new HashMap<>();
    private final Set<Permission> permissions_ = new HashSet<>();

    @Override
    public void registerInterface(Class<? extends PluginLoader> loader) throws IllegalArgumentException {
        unimplemented();
    }

    @Override
    public Plugin getPlugin(String name) {
        return null;
    }

    @Override
    public Plugin[] getPlugins() {
        return new Plugin[0];
    }

    @Override
    public boolean isPluginEnabled(String name) {
        return false;
    }

    @Override
    public boolean isPluginEnabled(Plugin plugin) {
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        unimplemented();
        return null;
    }

    @Override
    public Plugin[] loadPlugins(File directory) {
        return new Plugin[0];
    }

    @Override
    public void disablePlugins() {
    }

    @Override
    public void clearPlugins() {
        HandlerList.unregisterAll();
    }

    @Override
    public void callEvent(Event event) throws IllegalStateException {
        for (RegisteredListener registeredListener : event.getHandlers().getRegisteredListeners()) {
            try {
                registeredListener.callEvent(event);
            } catch (EventException ex) {
                throw new RuntimeException("Error while dispatching event " + event.getEventName(), ex);
            }
        }
    }

    @Override
    public void registerEvents(Listener listener, Plugin plugin) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 || !Event.class.isAssignableFrom(parameterTypes[0])) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) parameterTypes[0];
            method.setAccessible(true);

            EventExecutor executor = (ignored, event) -> {
                if (!eventType.isInstance(event)) {
                    return;
                }
                try {
                    method.invoke(listener, event);
                } catch (ReflectiveOperationException ex) {
                    throw new EventException(ex);
                }
            };

            this.registerEvent(eventType, listener, annotation.priority(), executor, plugin, annotation.ignoreCancelled());
        }
    }

    @Override
    public void registerEvent(Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin) {
        this.registerEvent(event, listener, priority, executor, plugin, false);
    }

    @Override
    public void registerEvent(Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin, boolean ignoreCancelled) {
        HandlerList handlerList = this.getHandlerList(event);
        handlerList.register(new RegisteredListener(listener, executor, priority, plugin, ignoreCancelled));
    }

    private HandlerList getHandlerList(Class<? extends Event> eventClass) {
        try {
            Method getHandlerList = eventClass.getMethod("getHandlerList");
            return (HandlerList) getHandlerList.invoke(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Event class " + eventClass.getName() + " does not expose static getHandlerList()", ex);
        }
    }

    @Override
    public void enablePlugin(Plugin plugin) {
    }

    @Override
    public void disablePlugin(Plugin plugin) {
    }

    @Override
    public Permission getPermission(String name) {
        return this.permissionsByName_.get(name);
    }

    @Override
    public void addPermission(Permission perm) {
        this.permissions_.add(perm);
        this.permissionsByName_.put(perm.getName(), perm);
    }

    @Override
    public void removePermission(Permission perm) {
        this.permissions_.remove(perm);
        this.permissionsByName_.remove(perm.getName());
    }

    @Override
    public void removePermission(String name) {
        Permission removed = this.permissionsByName_.remove(name);
        if (removed != null) {
            this.permissions_.remove(removed);
        }
    }

    @Override
    public Set<Permission> getDefaultPermissions(boolean op) {
        Set<Permission> defaults = new HashSet<>();
        for (Permission permission : this.permissions_) {
            if (permission.getDefault().getValue(op)) {
                defaults.add(permission);
            }
        }
        return defaults;
    }

    @Override
    public void recalculatePermissionDefaults(Permission perm) {
    }

    @Override
    public void subscribeToPermission(String permission, Permissible permissible) {
    }

    @Override
    public void unsubscribeFromPermission(String permission, Permissible permissible) {
    }

    @Override
    public Set<Permissible> getPermissionSubscriptions(String permission) {
        return Collections.emptySet();
    }

    @Override
    public void subscribeToDefaultPerms(boolean op, Permissible permissible) {
    }

    @Override
    public void unsubscribeFromDefaultPerms(boolean op, Permissible permissible) {
    }

    @Override
    public Set<Permissible> getDefaultPermSubscriptions(boolean op) {
        return Collections.emptySet();
    }

    @Override
    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(this.permissions_);
    }

    @Override
    public boolean useTimings() {
        return false;
    }
}
