/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.temporaryResource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.infiniteautomation.mango.rest.v2.util.CrudNotificationType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.exception.NotFoundException;

/**
 * @author Jared Wiltshire
 * @param <T> result type
 * @param <E> error type
 */
public abstract class TemporaryResourceManager<T, E> {
    /**
     * Default time before the resource is removed after completion
     */
    public static final long DEFAULT_EXPIRATION_MILLISECONDS = 300000; // 5 minutes
    
    /**
     * Default time that the task is allowed to run for before it is cancelled
     */
    public static final long DEFAULT_TIMEOUT_MILLISECONDS = 600000; // 10 minutes
    
    @FunctionalInterface
    public static interface ResourceTask<T, E> {
        void run(TemporaryResource<T, E> resource) throws Exception;
    }

    private final ConcurrentMap<String, TemporaryResource<T, E>> resources;
    private final TemporaryResourceWebSocketHandler websocketHandler;

    public TemporaryResourceManager() {
        this(null);
    }
    
    public TemporaryResourceManager(TemporaryResourceWebSocketHandler websocketHandler) {
        this.websocketHandler = websocketHandler;
        this.resources = new ConcurrentHashMap<>();
    }
    
    public abstract E exceptionToError(Exception e);

    /**
     * Creates a new temporary resource which is run in a Mango high priority task.
     * 
     * @param resourceType unique type string assigned to each resource type e.g. BULK_DATA_POINT
     * @param id if null will be assigned a UUID
     * @param user the user that started the temporary resource
     * @param expiration time after the resource completes that it will be removed (milliseconds). If null or less than zero, it will be set to the default DEFAULT_EXPIRATION_MILLISECONDS 
     * @param timeout time after the resource starts that it will be timeout if not complete (milliseconds). If null or less than zero, it will be set to the default DEFAULT_TIMEOUT_MILLISECONDS
     * @param resourceTask the task to be run
     * @return
     */
    public final TemporaryResource<T, E> newTemporaryResource(String resourceType, String id, User user, Long expiration, Long timeout, ResourceTask<T, E> resourceTask) {
        if (expiration == null || expiration < 0) {
            expiration = DEFAULT_EXPIRATION_MILLISECONDS;
        }
        if (timeout == null || timeout < 0) {
            timeout = DEFAULT_TIMEOUT_MILLISECONDS;
        }
        
        TemporaryResource<T, E> resource = new MangoTaskTemporaryResource<T, E>(resourceType, id, user.getId(), expiration, timeout, resourceTask, this);
        this.add(resource);

        try {
            resource.start();
        } catch (Exception e) {
            this.error(resource, e);
        }

        return resource;
    }

    /**
     * @return list of all resources in the resources map
     */
    public final List<TemporaryResource<T, E>> list() {
        return new ArrayList<>(this.resources.values());
    }
    
    /**
     * Get a resource from the map of resources using its id
     * @param id
     * @return
     */
    public final TemporaryResource<T, E> get(String id) {
        TemporaryResource<T, E> resource = this.resources.get(id);
        if (resource == null) {
            throw new NotFoundException();
        }
        return resource;
    }
    
    /**
     * Removes the resource from the map of resources.
     * Will not succeed if the resource has not completed.
     * 
     * @param resource
     * @return true if resource was successfully removed
     */
    public final boolean remove(TemporaryResource<T, E> resource) {
        if (resource.isComplete()) {
            TemporaryResource<T, E> existing = this.resources.remove(resource.getId());
            if (existing == null) {
                return false;
            }
            
            resource.removed();
            
            if (this.websocketHandler != null) {
                this.websocketHandler.notify(CrudNotificationType.DELETE, resource);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Sets the status to CANCELLED.
     * Will not succeed if the resource has already completed.
     * 
     * @param resource
     * @return true if status was successfully updated
     */
    public final boolean cancel(TemporaryResource<T, E> resource) {
        boolean cancelled = false;
        
        try {
            cancelled = resource.cancel();
        } catch (Exception e) {
            this.error(resource, e);
        }

        if (cancelled && this.websocketHandler != null) {
            this.websocketHandler.notify(CrudNotificationType.UPDATE, resource);
        }
        
        return cancelled;
    }

    /**
     * Adds the resource to the map of resources.
     * @param resource
     */
    final void add(TemporaryResource<T, E> resource) {
        TemporaryResource<T, E> existing = this.resources.putIfAbsent(resource.getId(), resource);
        if (existing != null) {
            throw new RuntimeException("UUID collision");
        }

        if (this.websocketHandler != null) {
            this.websocketHandler.notify(CrudNotificationType.CREATE, resource);
        }
    }

    /**
     * Sets the status to TIMED_OUT.
     * Will not succeed if the resource has already completed.
     * 
     * @param resource
     * @return true if status was successfully updated
     */
    final boolean timeOut(TemporaryResource<T, E> resource) {
        boolean timedOut = false;
        
        try {
            timedOut = resource.timeOut();
        } catch (Exception e) {
            this.error(resource, e);
        }
        
        if (timedOut && this.websocketHandler != null) {
            this.websocketHandler.notify(CrudNotificationType.UPDATE, resource);
        }
        
        return timedOut;
    }

    /**
     * Sets the status to SUCCESS.
     * Will not succeed if the resource has already completed.
     * 
     * @param resource
     * @param result
     * @return true if status was successfully updated
     */
    public final boolean success(TemporaryResource<T, E> resource, T result) {
        boolean succeeded = false;
        
        try {
            succeeded = resource.success(result);
        } catch (Exception e) {
            this.error(resource, e);
        }
        
        if (succeeded && this.websocketHandler != null) {
            this.websocketHandler.notify(CrudNotificationType.UPDATE, resource);
        }
        
        return succeeded;
    }
    
    /**
     * Sets the status to ERROR.
     * Will not succeed if the resource has already completed.
     * 
     * @param resource
     * @param exception
     * @return true if status was successfully updated
     */
    public final boolean error(TemporaryResource<T, E> resource, Exception exception) {
        E error = this.exceptionToError(exception);
        return this.error(resource, error);
    }
    
    /**
     * Sets the status to ERROR.
     * Will not succeed if the resource has already completed.
     * 
     * @param resource
     * @param error
     * @return true if status was successfully updated
     */
    public final boolean error(TemporaryResource<T, E> resource, E error) {
        if (resource.error(error)) {
            if (this.websocketHandler != null) {
                this.websocketHandler.notify(CrudNotificationType.UPDATE, resource);
            }
            return true;
        }
        return false;
    }

    /**
     * Sets the status to RUNNING and updates the progress.
     * Will not succeed if the resource has already completed.
     * 
     * @param resource
     * @param result
     * @param position
     * @param maximum
     * @return true if progress was successfully updated
     */
    public final boolean progress(TemporaryResource<T, E> resource, T result, Integer position, Integer maximum) {
        boolean progressUpdated = resource.progress(result, position, maximum);
        if (progressUpdated && this.websocketHandler != null) {
            this.websocketHandler.notify(CrudNotificationType.UPDATE, resource);
        }
        return progressUpdated;
    }
    
    /**
     * If position == maximum will set the status to SUCCESS, otherwise set the status to RUNNING and update progress.
     * Will not succeed if the resource has already completed.
     * 
     * @param resource
     * @param result
     * @param position
     * @param maximum
     * @return true if progress/status was successfully updated
     */
    public final boolean progressOrSuccess(TemporaryResource<T, E> resource, T result, Integer position, Integer maximum) {
        boolean callSuccess = position != null && position.equals(maximum);

        boolean progressUpdated = resource.progress(result, position, maximum);
        if (progressUpdated && callSuccess) {
            progressUpdated = resource.success(result);
        }
        
        if (progressUpdated && this.websocketHandler != null) {
            this.websocketHandler.notify(CrudNotificationType.UPDATE, resource);
        }

        return progressUpdated;
    }
    
    /**
     * Check if the resource is complete, i.e. the status is TIMED_OUT, CANCELLED, SUCCESS, or ERROR.
     * If a resource is complete its status can no longer be updated and all work should cease.
     * 
     * @param resource
     * @return true if the resource is complete
     */
    public final boolean isComplete(TemporaryResource<T, E> resource) {
        return resource.isComplete();
    }
}
