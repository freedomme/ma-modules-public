/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.temporaryResource;

import java.util.Date;

import com.infiniteautomation.mango.rest.v2.exception.ServerErrorException;
import com.infiniteautomation.mango.rest.v2.temporaryResource.TemporaryResourceManager.ResourceTask;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimerTask;

/**
 * @author Jared Wiltshire
 */
public class MangoTaskTemporaryResource<T, E> extends TemporaryResource<T, E> {
    private HighPriorityTask mainTask;
    private TimerTask timeoutTask;
    private TimerTask expirationTask;
    private TemporaryResourceManager<T, E> manager;
    private ResourceTask<T, E> resourceTask;

    /**
     * @param resourceType unique type string assigned to each resource type e.g. BULK_DATA_POINT
     * @param id if null will be assigned a UUID
     * @param userId user id of the user that started the temporary resource
     * @param expiration time after the resource completes that it will be removed (milliseconds)
     * @param timeout time after the resource starts that it will be timeout if not complete (milliseconds)
     * @param resourceTask the task to run
     * @param manager
     */
    protected MangoTaskTemporaryResource(String resourceType, String id, int userId, Long expiration, Long timeout, ResourceTask<T, E> resourceTask, TemporaryResourceManager<T, E> manager) {
        super(resourceType, id, userId, expiration, timeout);
        this.manager = manager;
        this.resourceTask = resourceTask;
    }
    
    @Override
    void startTask() {
        this.mainTask = new HighPriorityTask(this.getId()) {
            @Override
            public void run(long runtime) {
                try {
                    MangoTaskTemporaryResource.this.resourceTask.run(MangoTaskTemporaryResource.this);
                } catch (Exception e) {
                    E error = MangoTaskTemporaryResource.this.manager.exceptionToError(e);
                    MangoTaskTemporaryResource.this.manager.error(MangoTaskTemporaryResource.this, error);
                }
            }

            @Override
            public void rejected(RejectedTaskReason reason) {
                super.rejected(reason);

                // TODO translation for rejection reason
                E error = MangoTaskTemporaryResource.this.manager.exceptionToError(new ServerErrorException());
                MangoTaskTemporaryResource.this.manager.error(MangoTaskTemporaryResource.this, error);
            }
        };
        
        Common.backgroundProcessing.execute(this.mainTask);
    }

    @Override
    void scheduleTimeout(Date timeout) {
        // TimeoutTask schedules itself to be executed
        this.timeoutTask = new TimeoutTask(timeout, new TimeoutClient() {
            @Override
            public void scheduleTimeout(long fireTime) {
                MangoTaskTemporaryResource.this.manager.timeOut(MangoTaskTemporaryResource.this);
            }

            @Override
            public String getThreadName() {
                return "Temporary resource timeout " + MangoTaskTemporaryResource.this.getId();
            }

            @Override
            public void rejected(RejectedTaskReason reason) {
                super.rejected(reason);
                MangoTaskTemporaryResource.this.manager.timeOut(MangoTaskTemporaryResource.this);
            }
        });
    }

    @Override
    void scheduleRemoval(Date expiration) {
        // TimeoutTask schedules itself to be executed
        this.expirationTask = new TimeoutTask(expiration, new TimeoutClient() {
            @Override
            public void scheduleTimeout(long fireTime) {
                MangoTaskTemporaryResource.this.manager.remove(MangoTaskTemporaryResource.this);
            }

            @Override
            public String getThreadName() {
                return "Temporary resource expiration " + MangoTaskTemporaryResource.this.getId();
            }

            @Override
            public void rejected(RejectedTaskReason reason) {
                super.rejected(reason);
                MangoTaskTemporaryResource.this.manager.remove(MangoTaskTemporaryResource.this);
            }
        });
    }

    @Override
    void removeNow() {
        MangoTaskTemporaryResource.this.manager.remove(MangoTaskTemporaryResource.this);
    }

    @Override
    void cancelMainAndTimeout() {
        if (this.mainTask != null) {
            this.mainTask.cancel();
        }
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }
    }

    @Override
    void cancelRemoval() {
        if (this.expirationTask != null) {
            this.expirationTask.cancel();
        }
    }
}
