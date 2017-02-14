/*
 * ============LICENSE_START=========================================================
 * dcae-analytics
 * ================================================================================
 *  Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.dcae.analytics.tca.worker;

import co.cask.cdap.api.annotation.Property;
import co.cask.cdap.api.metrics.Metrics;
import co.cask.cdap.api.worker.AbstractWorker;
import co.cask.cdap.api.worker.WorkerContext;
import org.openecomp.dcae.analytics.common.AnalyticsConstants;
import org.openecomp.dcae.analytics.common.CDAPComponentsConstants;
import org.openecomp.dcae.analytics.common.exception.DCAEAnalyticsRuntimeException;
import org.openecomp.dcae.analytics.dmaap.DMaaPMRFactory;
import org.openecomp.dcae.analytics.dmaap.domain.config.DMaaPMRPublisherConfig;
import org.openecomp.dcae.analytics.dmaap.service.publisher.DMaaPMRPublisher;
import org.openecomp.dcae.analytics.model.util.AnalyticsModelIOUtils;
import org.openecomp.dcae.analytics.tca.settings.TCAAppPreferences;
import org.openecomp.dcae.analytics.tca.utils.AppPreferencesToPublisherConfigMapper;
import org.openecomp.dcae.analytics.tca.utils.TCAUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

/**
 * TCA DMaaP Publisher will monitor alerts table at regular intervals and publish any alerts to DMaaP MR Publishing
 * Topic
 * <p>
 * @author Rajiv Singla. Creation Date: 11/16/2016.
 */
public class TCADMaaPPublisherWorker extends AbstractWorker {

    private static final Logger LOG = LoggerFactory.getLogger(TCADMaaPPublisherWorker.class);

    /**
     * DMaaP MR Publisher
     */
    private DMaaPMRPublisher publisher;
    /**
     * Quartz Scheduler
     */
    private Scheduler scheduler;
    /**
     * Determines if scheduler is shutdown
     */
    private AtomicBoolean isSchedulerShutdown;
    /**
     * Store runtime metrics
     */
    private Metrics metrics;

    @Property
    private final String tcaVESAlertsTableName;

    public TCADMaaPPublisherWorker(final String tcaVESAlertsTableName) {
        this.tcaVESAlertsTableName = tcaVESAlertsTableName;
    }

    @Override
    public void configure() {
        // configure
        setName(CDAPComponentsConstants.TCA_FIXED_DMAAP_PUBLISHER_WORKER);
        setDescription(CDAPComponentsConstants.TCA_FIXED_DMAAP_PUBLISHER_DESCRIPTION_WORKER);
        LOG.debug("Configuring TCA MR DMaaP Publisher worker with name: {}",
                CDAPComponentsConstants.TCA_FIXED_DMAAP_PUBLISHER_WORKER);
    }


    @Override
    public void initialize(WorkerContext context) throws Exception {
        super.initialize(context);

        // Parse runtime arguments
        final TCAAppPreferences tcaAppPreferences = TCAUtils.getValidatedTCAAppPreferences(context);

        LOG.info("Initializing TCA MR DMaaP Publisher worker with preferences: {}", tcaAppPreferences);

        //  Map TCA App Preferences to DMaaP MR Publisher Config
        final DMaaPMRPublisherConfig publisherConfig = AppPreferencesToPublisherConfigMapper.map(tcaAppPreferences);

        LOG.info("TCA DMaaP MR Publisher worker will be polling TCA Alerts Table Name: {}", tcaVESAlertsTableName);

        // Create an instance of DMaaP MR Publisher
        LOG.debug("Creating an instance of DMaaP Publisher");
        publisher = DMaaPMRFactory.create().createPublisher(publisherConfig);

        // initialize a new Quartz scheduler
        initializeScheduler(tcaAppPreferences);
        // initialize scheduler state
        isSchedulerShutdown = new AtomicBoolean(true);
    }


    @Override
    public void run() {
        // Start Publisher scheduler
        try {
            scheduler.start();
            isSchedulerShutdown.getAndSet(false);

        } catch (SchedulerException e) {
            final String errorMessage =
                    format("Error while starting TCA DMaaP MR Publisher scheduler: %s", e.toString());
            throw new DCAEAnalyticsRuntimeException(errorMessage, LOG, e);
        }

        LOG.info("TCA DMaaP MR Publisher Scheduler started successfully");

        // indefinite loop which wakes up and confirms scheduler is indeed running
        while (!isSchedulerShutdown.get()) {
            try {

                Thread.sleep(AnalyticsConstants.TCA_DEFAULT_WORKER_SHUTDOWN_CHECK_INTERVAL_MS);

            } catch (InterruptedException e) {

                final String errorMessage =
                        format("Error while checking TCA DMaaP MR Publisher worker status: %s", e);
                throw new DCAEAnalyticsRuntimeException(errorMessage, LOG, e);
            }
        }

        LOG.info("Finished execution of TCA DMaaP MR Publisher worker thread");

    }

    @Override
    public void stop() {
        // Stop Publisher - which will flush any batch messages if present
        try {
            publisher.close();
        } catch (Exception e) {

            final String errorMessage = format("Error while shutting down DMaaP MR Publisher: %s", e);
            throw new DCAEAnalyticsRuntimeException(errorMessage, LOG, e);

        }
        // Stop Publisher scheduler
        try {

            LOG.info("Shutting TCA DMaaP MR Publisher Scheduler");

            scheduler.shutdown();
            isSchedulerShutdown.getAndSet(true);

        } catch (SchedulerException e) {

            final String errorMessage =
                    format("Error while shutting down TCA DMaaP MR Publisher scheduler: %s", e);
            throw new DCAEAnalyticsRuntimeException(errorMessage, LOG, e);
        }
    }


    /**
     * Initializes a scheduler instance for DMaaP MR Publisher Job
     *
     * @throws SchedulerException SchedulerException
     */
    private void initializeScheduler(TCAAppPreferences tcaAnalyticsAppConfig) throws SchedulerException {

        // Initialize a new Quartz Standard scheduler - settings settings are in quartz-publisher.properties file
        final StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory();
        final String quartzPublisherPropertiesFileName = AnalyticsConstants.TCA_QUARTZ_PUBLISHER_PROPERTIES_FILE_NAME;
        LOG.debug("Configuring quartz scheduler for TCA DMaaP MR Publisher with properties file: {}",
                quartzPublisherPropertiesFileName);
        final Properties publisherProperties =
                AnalyticsModelIOUtils.loadPropertiesFile(quartzPublisherPropertiesFileName);
        stdSchedulerFactory.initialize(publisherProperties);
        scheduler = stdSchedulerFactory.getScheduler();

        // Create a new JobDataMap containing information required by TCA DMaaP Publisher Job
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(AnalyticsConstants.CDAP_ALERTS_TABLE_VARIABLE_NAME, tcaVESAlertsTableName);
        jobDataMap.put(AnalyticsConstants.WORKER_CONTEXT_VARIABLE_NAME, getContext());
        jobDataMap.put(AnalyticsConstants.DMAAP_PUBLISHER_VARIABLE_NAME, publisher);
        jobDataMap.put(AnalyticsConstants.DMAAP_METRICS_VARIABLE_NAME, metrics);

        // Create a new job detail
        final JobDetail jobDetail = JobBuilder.newJob(TCADMaaPMRPublisherJob.class)
                .withIdentity(AnalyticsConstants.TCA_DMAAP_PUBLISHER_QUARTZ_JOB_NAME,
                        AnalyticsConstants.TCA_QUARTZ_GROUP_NAME)
                .usingJobData(jobDataMap).build();

        // Create a new scheduling builder
        final Integer publisherPollingInterval = tcaAnalyticsAppConfig.getPublisherPollingInterval();
        final SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds(publisherPollingInterval) // job will use custom polling schedule
                .repeatForever(); // repeats while worker is running

        // Create a trigger for the TCA Publisher Job
        final SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger()
                .withIdentity(AnalyticsConstants.TCA_DMAAP_PUBLISHER_QUARTZ_TRIGGER_NAME,
                        AnalyticsConstants.TCA_QUARTZ_GROUP_NAME)
                .startNow() // job starts right away
                .withSchedule(simpleScheduleBuilder).build();

        scheduler.scheduleJob(jobDetail, simpleTrigger);
        LOG.info("Initialized TCA DMaaP MR Publisher Scheduler");
    }
}
