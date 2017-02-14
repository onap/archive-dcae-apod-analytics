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

package org.openecomp.dcae.analytics.tca;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Suppliers;
import org.junit.BeforeClass;
import org.openecomp.dcae.analytics.common.AnalyticsConstants;
import org.openecomp.dcae.analytics.model.util.AnalyticsModelIOUtils;
import org.openecomp.dcae.analytics.model.util.json.AnalyticsModelObjectMapperSupplier;
import org.openecomp.dcae.analytics.tca.settings.TCATestAppConfig;
import org.openecomp.dcae.analytics.tca.settings.TCATestAppPreferences;
import org.openecomp.dcae.analytics.test.BaseDCAEAnalyticsIT;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Rajiv Singla. Creation Date: 10/25/2016.
 */
public class BaseAnalyticsTCAIT extends BaseDCAEAnalyticsIT {

    protected static ObjectMapper objectMapper;

    @BeforeClass
    public static void beforeClass() {
        final AnalyticsModelObjectMapperSupplier analyticsModelObjectMapperSupplier =
                new AnalyticsModelObjectMapperSupplier();
        objectMapper = Suppliers.memoize(analyticsModelObjectMapperSupplier).get();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    protected static final String TCA_CONTROLLER_POLICY_FILE_LOCATION =
            "data/properties/tca_controller_policy.properties";

    // App Settings
    protected static final String DCAE_ANALYTICS_TCA_TEST_APP_NAME = "dcae-tca";
    protected static final String DCAE_ANALYTICS_TCA_TEST_APP_DESC =
            "DCAE Analytics Threshold Crossing Alert Application";
    
    // Subscriber App Preferences
    protected static final String SUBSCRIBER_HOST_NAME = "mrlocal-mtnjftle01.homer.com";
    protected static final Integer SUBSCRIBER_PORT_NUMBER = 3905;
    protected static final String SUBSCRIBER_TOPIC_NAME = "com.dcae.dmaap.mtnje2.DcaeTestVESSub";
    protected static final String SUBSCRIBER_USERNAME = "m00502@tca.af.dcae.com";
    protected static final String SUBSCRIBER_PASSWORD = "Te5021abc";
    protected static final String SUBSCRIBER_HTTP_PROTOCOL = "https";
    protected static final String SUBSCRIBER_CONTENT_TYPE = "application/json";
    protected static final Integer SUBSCRIBER_POLLING_INTERVAL = 20000;

    protected static final String SUBSCRIBER_CONSUMER_ID = "c12";
    protected static final String SUBSCRIBER_CONSUMER_GROUP_NAME = AnalyticsConstants.DMAAP_GROUP_PREFIX + 
            SUBSCRIBER_CONSUMER_ID;
    protected static final int SUBSCRIBER_TIMOUT_MS = -1;
    protected static final int SUBSCRIBER_MESSAGE_LIMIT = -1;

    // Publisher App Preferences
    protected static final String PUBLISHER_HOST_NAME = "mrlocal-mtnjftle01.homer.com";
    protected static final Integer PUBLISHER_PORT_NUMBER = 3905;
    protected static final String PUBLISHER_TOPIC_NAME = "com.dcae.dmaap.mtnje2.DcaeTestVESPub";
    protected static final String PUBLISHER_USERNAME = "m00502@tca.af.dcae.com";
    protected static final String PUBLISHER_PASSWORD = "Te5021abc";
    protected static final String PUBLISHER_HTTP_PROTOCOL = "https";
    protected static final String PUBLISHER_CONTENT_TYPE = "application/json";
    protected static final Integer PUBLISHER_BATCH_QUEUE_SIZE = 10;
    protected static final Integer PUBLISHER_RECOVERY_QUEUE_SIZE = 100000;
    protected static final Integer PUBLISHER_POLLING_INTERVAL = 20000;

    protected static TCATestAppConfig getTCATestAppConfig() {
        final TCATestAppConfig tcaTestAppConfig = new TCATestAppConfig();
        tcaTestAppConfig.setAppName(DCAE_ANALYTICS_TCA_TEST_APP_NAME);
        tcaTestAppConfig.setAppDescription(DCAE_ANALYTICS_TCA_TEST_APP_DESC);
        return tcaTestAppConfig;
    }

    protected static TCATestAppPreferences getTCATestAppPreferences() {
        final TCATestAppPreferences tcaTestAppPreferences = new TCATestAppPreferences(getTCAPolicyPreferences());
        tcaTestAppPreferences.setSubscriberHostName(SUBSCRIBER_HOST_NAME);
        tcaTestAppPreferences.setSubscriberHostPortNumber(SUBSCRIBER_PORT_NUMBER);
        tcaTestAppPreferences.setSubscriberTopicName(SUBSCRIBER_TOPIC_NAME);
        tcaTestAppPreferences.setSubscriberUserName(SUBSCRIBER_USERNAME);
        tcaTestAppPreferences.setSubscriberUserPassword(SUBSCRIBER_PASSWORD);
        tcaTestAppPreferences.setSubscriberProtocol(SUBSCRIBER_HTTP_PROTOCOL);
        tcaTestAppPreferences.setSubscriberContentType(SUBSCRIBER_CONTENT_TYPE);
        tcaTestAppPreferences.setSubscriberConsumerId(SUBSCRIBER_CONSUMER_ID);
        tcaTestAppPreferences.setSubscriberConsumerGroup(SUBSCRIBER_CONSUMER_GROUP_NAME);
        tcaTestAppPreferences.setSubscriberTimeoutMS(SUBSCRIBER_TIMOUT_MS);
        tcaTestAppPreferences.setSubscriberMessageLimit(SUBSCRIBER_MESSAGE_LIMIT);
        tcaTestAppPreferences.setSubscriberPollingInterval(SUBSCRIBER_POLLING_INTERVAL);

        tcaTestAppPreferences.setPublisherHostName(PUBLISHER_HOST_NAME);
        tcaTestAppPreferences.setPublisherHostPort(PUBLISHER_PORT_NUMBER);
        tcaTestAppPreferences.setPublisherTopicName(PUBLISHER_TOPIC_NAME);
        tcaTestAppPreferences.setPublisherUserName(PUBLISHER_USERNAME);
        tcaTestAppPreferences.setPublisherUserPassword(PUBLISHER_PASSWORD);
        tcaTestAppPreferences.setPublisherProtocol(PUBLISHER_HTTP_PROTOCOL);
        tcaTestAppPreferences.setPublisherContentType(PUBLISHER_CONTENT_TYPE);
        tcaTestAppPreferences.setPublisherMaxBatchSize(PUBLISHER_BATCH_QUEUE_SIZE);
        tcaTestAppPreferences.setPublisherMaxRecoveryQueueSize(PUBLISHER_RECOVERY_QUEUE_SIZE);
        tcaTestAppPreferences.setPublisherPollingInterval(PUBLISHER_POLLING_INTERVAL);
        return tcaTestAppPreferences;
    }


    protected static Map<String, String> getTCAPolicyPreferences() {
        final Map<String, String> policyPreferences = new LinkedHashMap<>();
        final Properties policyPreferencesProps =
                AnalyticsModelIOUtils.loadPropertiesFile(TCA_CONTROLLER_POLICY_FILE_LOCATION);
        for (Map.Entry<Object, Object> propEntry : policyPreferencesProps.entrySet()) {
            policyPreferences.put(propEntry.getKey().toString(), propEntry.getValue().toString());
        }

        return policyPreferences;
    }

    protected static String serializeModelToJson(Object model) throws JsonProcessingException {
        return objectMapper.writeValueAsString(model);
    }
}
