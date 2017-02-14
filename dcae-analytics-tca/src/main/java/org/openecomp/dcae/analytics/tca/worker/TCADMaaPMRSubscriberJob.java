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

import co.cask.cdap.api.metrics.Metrics;
import co.cask.cdap.api.worker.WorkerContext;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import org.openecomp.dcae.analytics.common.AnalyticsConstants;
import org.openecomp.dcae.analytics.common.CDAPMetricsConstants;
import org.openecomp.dcae.analytics.common.exception.DCAEAnalyticsRuntimeException;
import org.openecomp.dcae.analytics.common.utils.HTTPUtils;
import org.openecomp.dcae.analytics.dmaap.domain.response.DMaaPMRSubscriberResponse;
import org.openecomp.dcae.analytics.dmaap.service.subscriber.DMaaPMRSubscriber;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

/**
 * Quartz Job which polls DMaaP MR VES Collector Topic for messages and writes them to
 * a given CDAP Stream
 *
 * @author Rajiv Singla. Creation Date: 10/24/2016.
 */
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class TCADMaaPMRSubscriberJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(TCADMaaPMRSubscriberJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        LOG.debug("Starting DMaaP MR Topic Subscriber fetch Job. Next firing time will be: {}",
                jobExecutionContext.getNextFireTime());

        // Get Job Data Map
        final JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();

        // Fetch all Job Params from Job Data Map
        final String cdapStreamName = jobDataMap.getString(AnalyticsConstants.CDAP_STREAM_VARIABLE_NAME);
        final WorkerContext workerContext =
                (WorkerContext) jobDataMap.get(AnalyticsConstants.WORKER_CONTEXT_VARIABLE_NAME);
        final DMaaPMRSubscriber subscriber =
                (DMaaPMRSubscriber) jobDataMap.get(AnalyticsConstants.DMAAP_SUBSCRIBER_VARIABLE_NAME);
        final Metrics metrics = (Metrics) jobDataMap.get(AnalyticsConstants.DMAAP_METRICS_VARIABLE_NAME);

        final Optional<DMaaPMRSubscriberResponse> subscriberResponseOptional =
                getSubscriberResponse(subscriber, metrics);

        // If response is not present, unable to proceed
        if (!subscriberResponseOptional.isPresent()) {
            return;
        }

        final DMaaPMRSubscriberResponse subscriberResponse = subscriberResponseOptional.get();

        // If response code return by the subscriber call is not successful, unable to do proceed
        if (!HTTPUtils.isSuccessfulResponseCode(subscriberResponse.getResponseCode())) {
            LOG.error("Subscriber was unable to fetch messages properly. Subscriber Response Code: {} " +
                    "Unable to proceed further....", subscriberResponse.getResponseCode());
            metrics.count(CDAPMetricsConstants.TCA_SUBSCRIBER_UNSUCCESSFUL_RESPONSES_METRIC, 1);
            return;
        }

        LOG.debug("Subscriber HTTP Response Status Code match successful:  {}", subscriberResponse,
                HTTPUtils.HTTP_SUCCESS_STATUS_CODE);

        final List<String> actualMessages = subscriberResponse.getFetchedMessages();

        // If there are no message returned during from Subscriber, nothing to write to CDAP Stream
        if (actualMessages.isEmpty()) {
            LOG.debug("Subscriber Response has no messages. Nothing to write to CDAP stream....");
            metrics.count(CDAPMetricsConstants.TCA_SUBSCRIBER_RESPONSES_WITH_NO_MESSAGES_METRIC, 1);
            return;
        }

        LOG.debug("DMaaP MR Subscriber found new messages in DMaaP Topic. Message count: {}", actualMessages.size());
        metrics.count(CDAPMetricsConstants.TCA_SUBSCRIBER_TOTAL_MESSAGES_PROCESSED_METRIC, actualMessages.size());

        // Write message to CDAP Stream using Stream Batch Writer
        LOG.debug("Writing message to CDAP Stream: {}, Message Count: {}", cdapStreamName, actualMessages.size());
        try {

            for (String message : actualMessages) {
                workerContext.write(cdapStreamName, message);
            }

        } catch (IOException e) {
            metrics.count(CDAPMetricsConstants.TCA_SUBSCRIBER_FAILURE_TO_WRITE_TO_STREAM_METRIC, 1);
            final String errorMessage =
                    format("Error while DMaaP message router subscriber attempting to write to CDAP Stream: %s, " +
                            "Exception: %s", cdapStreamName, e);
            throw new DCAEAnalyticsRuntimeException(errorMessage, LOG, e);
        }

        LOG.debug("DMaaP MR Subscriber successfully finished writing messages to CDAP Stream: {}, Message count: {}",
                cdapStreamName, actualMessages.size());
    }


    /**
     * Get Subscriber response and records time taken to fetch messages. Returns Optional.None if Subscriber response
     * is null or response status code is not present
     *
     * @param subscriber - DMaaP Subscriber
     * @param metrics - CDAP Metrics collector
     *
     * @return - Optional of Subscriber Response
     */
    private static Optional<DMaaPMRSubscriberResponse> getSubscriberResponse(final DMaaPMRSubscriber subscriber,
                                                                             final Metrics metrics) {

        // Check how long it took for subscriber to respond
        final Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        DMaaPMRSubscriberResponse subscriberResponse = null;
        // Fetch messages from DMaaP MR Topic
        try {
            subscriberResponse = subscriber.fetchMessages();
        } catch (DCAEAnalyticsRuntimeException e) {
            LOG.error("Error while fetching messages for DMaaP MR Topic: {}", e);
        }

        stopwatch.stop();
        final long subscriberResponseTimeMS = stopwatch.elapsedMillis();

        // If response is null is null or response code is null, unable to proceed nothing to do
        if (subscriberResponse == null || subscriberResponse.getResponseCode() == null) {
            LOG.error("Subscriber Response is null or subscriber Response code is null. Unable to proceed further...");
            return Optional.absent();
        }

        LOG.debug("Subscriber Response:{}, Subscriber HTTP Response Status Code {}, Subscriber Response Time(ms): {}",
                subscriberResponse, subscriberResponse.getResponseCode(), subscriberResponseTimeMS);

        // Record subscriber response time
        metrics.gauge(CDAPMetricsConstants.TCA_SUBSCRIBER_RESPONSE_TIME_MS_METRIC, subscriberResponseTimeMS);

        // Record all response count from subscriber
        metrics.count(CDAPMetricsConstants.TCA_SUBSCRIBER_ALL_RESPONSES_COUNT_METRIC, 1);

        return Optional.of(subscriberResponse);
    }

}
