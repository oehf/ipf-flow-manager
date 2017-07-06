/*
 * Copyright 2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openehealth.ipf.commons.flow.purge;

import static org.openehealth.ipf.commons.flow.config.ApplicationConfig.DO_NOT_PURGE_ERROR_FLOWS_DEFAULT;
import static org.openehealth.ipf.commons.flow.config.ApplicationConfig.FLOW_PURGE_SCHEDULE_DEFAULT;
import static org.openehealth.ipf.commons.flow.config.ApplicationConfig.PURGE_FLOWS_OLDER_THAN_DEFAULT;
import static org.openehealth.ipf.commons.flow.repository.FlowPurgeCriteria.PurgeMode.ALL;
import static org.openehealth.ipf.commons.flow.repository.FlowPurgeCriteria.PurgeMode.CLEAN;

import java.util.Date;

import org.openehealth.ipf.commons.flow.core.Duration;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openehealth.ipf.commons.flow.FlowManager;
import org.openehealth.ipf.commons.flow.config.ApplicationConfig;
import org.openehealth.ipf.commons.flow.jmx.FlowPurgerMBean;
import org.openehealth.ipf.commons.flow.repository.FlowPurgeCriteria;
import org.openehealth.ipf.commons.flow.repository.FlowPurgeCriteria.PurgeMode;

import static org.quartz.TriggerBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

/**
 * A flow purge job implementation used by both, Quartz scheduler and
 * {@link FlowPurgerMBean}. The interface to the Quartz scheduler is
 * {@link Job} whereas the {@link FlowPurgerMBean} uses this class directly for
 * managing jobs and and tracking their status.
 * 
 * @author Martin Krasser
 */
public class FlowPurgeJob implements Job {
    
    private static final String FLOW_MANAGER_KEY = "flowManager";
    private static final String APP_CONFIG_KEY = "appConfig";
    private static final Logger LOG = LoggerFactory.getLogger(FlowPurgeJob.class);
    
    private final FlowManager flowManager;
    private final Scheduler scheduler;
    private final String application;

    private String flowPurgeSchedule;
    private String purgeFlowsOlderThan;
    private boolean doNotPurgeErrorFlows;
    
    /**
     * Constructor used by Quartz.
     */
    public FlowPurgeJob() {
        this(null, null, null);
    }

    /**
     * Constructor used by {@link FlowPurgerMBean}.
     * 
     * @param flowManager
     *            flow manager.
     * @param scheduler
     *            Quartz scheduler.
     * @param application
     *            application name.
     */
    public FlowPurgeJob(FlowManager flowManager, Scheduler scheduler, String application) {
        this.flowManager = flowManager;
        this.scheduler = scheduler;
        this.application = application;
        flowPurgeSchedule = FLOW_PURGE_SCHEDULE_DEFAULT;
        purgeFlowsOlderThan = PURGE_FLOWS_OLDER_THAN_DEFAULT;
        doNotPurgeErrorFlows = DO_NOT_PURGE_ERROR_FLOWS_DEFAULT;
    }
    
    public String getFlowPurgeSchedule() {
        return flowPurgeSchedule;
    }

    /**
     * @see ApplicationConfig#setFlowPurgeSchedule(String)
     */
    public void setFlowPurgeSchedule(String flowPurgeSchedule) {
        this.flowPurgeSchedule = flowPurgeSchedule;
    }

    public String getPurgeFlowsOlderThan() {
        return purgeFlowsOlderThan;
    }

    /**
     * @see ApplicationConfig#setPurgeFlowsOlderThan(String)
     */
    public void setPurgeFlowsOlderThan(String purgeFlowsOlderThan) {
        this.purgeFlowsOlderThan = purgeFlowsOlderThan;
    }

    public boolean isDoNotPurgeErrorFlows() {
        return doNotPurgeErrorFlows;
    }

    /**
     * @see ApplicationConfig#setDoNotPurgeErrorFlows(boolean) 
     */
    public void setDoNotPurgeErrorFlows(boolean doNotPurgeErrorFlows) {
        this.doNotPurgeErrorFlows = doNotPurgeErrorFlows;
    }

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        FlowManager manager = (FlowManager)context.getJobDetail().getJobDataMap().get(FLOW_MANAGER_KEY);
        ApplicationConfig config = (ApplicationConfig)context.getJobDetail().getJobDataMap().get(APP_CONFIG_KEY);
        synchronized (manager) {
            execute(manager, config);
        }
    }

    /**
     * Runs repeated {@link FlowManager#purgeFlows(FlowPurgeCriteria)}
     * operations based on criteria given in <code>config</code> until all flows
     * matching the criteria have been purged from the database. .
     * 
     * @param manager
     *            flow manager
     * @param config
     *            application-specific purge configuration
     */
    private void execute(FlowManager manager, ApplicationConfig config) {
        PurgeMode mode = config.isDoNotPurgeErrorFlows() ? CLEAN : ALL;
        Date olderThan = Duration.parse(config.getPurgeFlowsOlderThan()).since();
        FlowPurgeCriteria criteria = new FlowPurgeCriteria(mode, olderThan, config.getApplication(), 100);
    
        int purgeCountTotal = 0;
        int purgeCount;
        LOG.info("Start purging flows for application {}", config.getApplication());
        do {
            purgeCount = manager.purgeFlows(criteria);
            purgeCountTotal += purgeCount;
            LOG.info("Purged {} flows (application={})", purgeCountTotal, config.getApplication());
        } while (purgeCount >= criteria.getMaxPurgeCount()); 
        LOG.info("Finished purging flows for application {}", config.getApplication());
    }

    /**
     * Schedules this job for an single, immediate execution in a separate
     * thread.
     * 
     * @param config
     *            application-specific purge configuration
     */
    public void execute(ApplicationConfig config) {
        String name = "once";

        schedule(config, newTrigger().withIdentity(name, null).build(), name);
        LOG.info("Execute purge job once for application {}", application);
    }
    
    /**
     * Schedules this job for a repeated execution based on a cron expression.
     * 
     * @param config
     *            application-specific purge configuration
     * @see ApplicationConfig#setFlowPurgeSchedule(String)
     */
    public void schedule(ApplicationConfig config) {
        CronTrigger cronTrigger = newTrigger().withIdentity(application, null)
                                              .withSchedule(cronSchedule(flowPurgeSchedule))
                                              .build();
        schedule(config, cronTrigger, application);
        setFlowPurgeScheduled(true);
        LOG.info("Scheduled purge job for application {}", application);
    }
    
    private void schedule(ApplicationConfig config, Trigger trigger, String jobName) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(FlowPurgeJob.FLOW_MANAGER_KEY, flowManager);
        jobDataMap.put(FlowPurgeJob.APP_CONFIG_KEY, config);

        JobDetail jobDetail = newJob()
                                  .withIdentity(jobName, null)
                                  .ofType(FlowPurgeJob.class)
                                  .usingJobData(jobDataMap)
                                  .build();
        
        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new FlowPurgeJobException("Cannot schedule job: " + e.getMessage());
        }
    }
    
    /**
     * Unschedules this job from repeated execution.
     */
    public void unschedule() {
        try {
            scheduler.deleteJob(JobKey.jobKey(application, null));
            setFlowPurgeScheduled(false);
            LOG.info("Unscheduled purge job for application {}", application);
        } catch (SchedulerException e) {
            throw new FlowPurgeJobException("Cannot unschedule job: " + e.getMessage());
        }
    }

    /**
     * Returns <code>true</code> is this job is currently scheduled for repeated
     * execution. This method does <b>not</b> return
     * <code> if the job currently as a result of a {@link #execute(ApplicationConfig)} invocation.
     * 
     * @return <code>true</code> if the job is scheduled for repeated execution,
     *         <code>false</code> otherwise.
     */
    public boolean isScheduled() {
        try {
            return scheduler.getJobDetail(JobKey.jobKey(application, null)) != null;
        } catch (SchedulerException e) {
            throw new FlowPurgeJobException("Cannot get job details: " + e.getMessage());
        }
    }
    
    private void setFlowPurgeScheduled(boolean purgeScheduled) {
        ApplicationConfig applicationConfig = flowManager.getApplicationConfig(application);
        applicationConfig.setFlowPurgeScheduled(purgeScheduled);
        flowManager.mergeApplicationConfig(applicationConfig);
    }

}
