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

package org.openecomp.dcae.analytics.tca.utils;

import co.cask.cdap.api.RuntimeContext;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openecomp.dcae.analytics.common.AnalyticsConstants;
import org.openecomp.dcae.analytics.common.exception.DCAEAnalyticsRuntimeException;
import org.openecomp.dcae.analytics.common.exception.MessageProcessingException;
import org.openecomp.dcae.analytics.common.service.processor.AbstractMessageProcessor;
import org.openecomp.dcae.analytics.common.service.processor.GenericMessageChainProcessor;
import org.openecomp.dcae.analytics.model.domain.cef.CommonEventHeader;
import org.openecomp.dcae.analytics.model.domain.cef.EventListener;
import org.openecomp.dcae.analytics.model.domain.cef.EventSeverity;
import org.openecomp.dcae.analytics.model.domain.policy.tca.Direction;
import org.openecomp.dcae.analytics.model.domain.policy.tca.MetricsPerFunctionalRole;
import org.openecomp.dcae.analytics.model.domain.policy.tca.TCAPolicy;
import org.openecomp.dcae.analytics.model.domain.policy.tca.Threshold;
import org.openecomp.dcae.analytics.model.facade.tca.AAI;
import org.openecomp.dcae.analytics.model.facade.tca.TCAVESResponse;
import org.openecomp.dcae.analytics.model.util.AnalyticsModelJsonUtils;
import org.openecomp.dcae.analytics.tca.persistance.TCAVESAlertEntity;
import org.openecomp.dcae.analytics.tca.processor.TCACEFJsonProcessor;
import org.openecomp.dcae.analytics.tca.processor.TCACEFPolicyDomainFilter;
import org.openecomp.dcae.analytics.tca.processor.TCACEFPolicyFunctionalRoleFilter;
import org.openecomp.dcae.analytics.tca.processor.TCACEFPolicyThresholdsProcessor;
import org.openecomp.dcae.analytics.tca.processor.TCACEFProcessorContext;
import org.openecomp.dcae.analytics.tca.settings.TCAAppPreferences;
import org.openecomp.dcae.analytics.tca.settings.TCAPolicyPreferences;
import org.openecomp.dcae.analytics.tca.validator.TCAPolicyPreferencesValidator;
import org.openecomp.dcae.analytics.tca.validator.TCAPreferencesValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.collect.Lists.newArrayList;
import static org.openecomp.dcae.analytics.common.AnalyticsConstants.TCA_POLICY_METRICS_PER_FUNCTIONAL_ROLE_PATH;
import static org.openecomp.dcae.analytics.common.utils.ValidationUtils.validateSettings;

/**
 * Utility Helper methods for TCA sub module only. Extends {@link AnalyticsModelJsonUtils} to get
 * pre configured Json Object Mapper understand serialization and deserialization of CEF Message
 * and TCA Policy
 *
 * @author Rajiv Singla. Creation Date: 10/24/2016.
 */
public abstract class TCAUtils extends AnalyticsModelJsonUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TCAUtils.class);

    /**
     * Threshold Comparator which is used to order thresholds based on their severity e.g. ( CRITICAL, MAJOR, MINOR,
     * WARNING )
     */
    private static final Comparator<Threshold> THRESHOLD_COMPARATOR = new Comparator<Threshold>() {
        @Override
        public int compare(Threshold threshold1, Threshold threshold2) {
            return threshold1.getSeverity().compareTo(threshold2.getSeverity());
        }
    };


    /**
     * {@link Function} that extracts {@link TCAPolicy#getMetricsPerFunctionalRole()} from {@link TCAPolicy}
     *
     * @return TCA Policy Metrics Per Functional Roles List
     */
    public static Function<TCAPolicy, List<MetricsPerFunctionalRole>> tcaPolicyMetricsExtractorFunction() {
        return new Function<TCAPolicy, List<MetricsPerFunctionalRole>>() {
            @Nullable
            @Override
            public List<MetricsPerFunctionalRole> apply(@Nonnull TCAPolicy tcaPolicy) {
                return tcaPolicy.getMetricsPerFunctionalRole();
            }
        };
    }

    /**
     * {@link Function} that extracts {@link MetricsPerFunctionalRole#getFunctionalRole()} from
     * {@link MetricsPerFunctionalRole}
     *
     * @return Functional role or a Metrics Per Functional Role object
     */
    public static Function<MetricsPerFunctionalRole, String> tcaFunctionalRoleExtractorFunction() {
        return new Function<MetricsPerFunctionalRole, String>() {
            @Override
            public String apply(@Nonnull MetricsPerFunctionalRole metricsPerFunctionalRole) {
                return metricsPerFunctionalRole.getFunctionalRole();
            }
        };
    }


    /**
     * Extracts {@link TCAPolicy} Functional Roles
     *
     * @param tcaPolicy TCA Policy
     * @return List of functional Roles in the tca Policy
     */
    public static List<String> getPolicyFunctionalRoles(@Nonnull final TCAPolicy tcaPolicy) {
        final List<MetricsPerFunctionalRole> metricsPerFunctionalRoles =
                tcaPolicyMetricsExtractorFunction().apply(tcaPolicy);

        return Lists.transform(metricsPerFunctionalRoles, tcaFunctionalRoleExtractorFunction());
    }

    /**
     * A {@link Supplier} which caches {@link TCAPolicy} Functional Roles as they are not expected to
     * change during runtime
     *
     * @param tcaPolicy TCA Policy
     * @return a Supplier that memoize the Functional roles
     */
    public static Supplier<List<String>> getPolicyFunctionalRoleSupplier(@Nonnull final TCAPolicy tcaPolicy) {
        return Suppliers.memoize(new Supplier<List<String>>() {
            @Override
            public List<String> get() {
                return getPolicyFunctionalRoles(tcaPolicy);
            }
        });
    }


    /**
     * Creates a Table to lookup thresholds of a {@link TCAPolicy} by its Functional Role and Threshold Field path
     *
     * @param tcaPolicy TCA Policy
     * @return A table with Keys of functional role and field path containing List of threshold as values
     */
    public static Table<String, String, List<Threshold>> getPolicyFRThresholdsTable(final TCAPolicy tcaPolicy) {
        final Table<String, String, List<Threshold>> domainFRTable = HashBasedTable.create();
        for (MetricsPerFunctionalRole metricsPerFunctionalRole : tcaPolicy.getMetricsPerFunctionalRole()) {
            final String functionalRole = metricsPerFunctionalRole.getFunctionalRole();
            final List<Threshold> thresholds = metricsPerFunctionalRole.getThresholds();
            for (Threshold threshold : thresholds) {
                final List<Threshold> existingThresholds = domainFRTable.get(functionalRole, threshold.getFieldPath());
                if (existingThresholds == null) {
                    final LinkedList<Threshold> newThresholdList = new LinkedList<>();
                    newThresholdList.add(threshold);
                    domainFRTable.put(functionalRole, threshold.getFieldPath(), newThresholdList);
                } else {
                    domainFRTable.get(functionalRole, threshold.getFieldPath()).add(threshold);
                }
            }
        }
        return domainFRTable;
    }


    /**
     * A {@link Supplier} which caches Policy Functional Role and Threshold Field Path Thresholds lookup table
     *
     * @param tcaPolicy TCA Policy
     * @return Cached Supplier for table with Keys of functional role and field path containing thresholds as values
     */
    public static Supplier<Table<String, String, List<Threshold>>> getPolicyFRThresholdsTableSupplier
    (final TCAPolicy tcaPolicy) {
        return Suppliers.memoize(new Supplier<Table<String, String, List<Threshold>>>() {
            @Override
            public Table<String, String, List<Threshold>> get() {
                return getPolicyFRThresholdsTable(tcaPolicy);
            }
        });
    }


    /**
     * Parses and validates Runtime Arguments to {@link TCAAppPreferences} object
     *
     * @param runtimeContext Runtime Context
     *
     * @return validated runtime arguments as {@link TCAAppPreferences} object
     */
    public static TCAAppPreferences getValidatedTCAAppPreferences(final RuntimeContext runtimeContext) {
        // Parse runtime arguments
        final Map<String, String> runtimeArguments = runtimeContext.getRuntimeArguments();
        final TCAAppPreferences tcaAppPreferences =
                ANALYTICS_MODEL_OBJECT_MAPPER.convertValue(runtimeArguments, TCAAppPreferences.class);

        // Update values of app preferences based on controller passed arguments if required
        final TCAAppPreferences updatedTCAAppPreferences =
                updateDMaaPPubSubValues(runtimeArguments, tcaAppPreferences);

        // Validate runtime arguments
        validateSettings(updatedTCAAppPreferences, new TCAPreferencesValidator());

        return tcaAppPreferences;
    }


    /**
     * Updates DMaaP Subscriber and Publisher Urls if present in runtime arguments.
     * Maps runtime arguments property - dmaap.in.event-input.dmaapUrl to Subscriber host, port and topic
     * Maps runtime arguments property - dmaap.out.alert-output.dmaapUrl to Publisher host, port and topic
     *
     * @param runtimeArguments Runtime arguments passed in to TCA App by controller
     * @param tcaAppPreferences TCA App Preferences
     *
     * @return TCA App Preferences which updated Publisher and Subscriber host,port and topic values
     */
    public static TCAAppPreferences updateDMaaPPubSubValues(final Map<String, String> runtimeArguments,
                                                            final TCAAppPreferences tcaAppPreferences) {

        final String subscriberPropertyKey = "dmaap.in.event-input.dmaapUrl";
        final String subscriberPropertyValue = runtimeArguments.get(subscriberPropertyKey);

        if (subscriberPropertyValue != null) {
            LOG.debug("Updating value for DMaaP Subscriber to values provided in property: {} with value: {}",
                    subscriberPropertyKey, subscriberPropertyValue);
            final URL subscriberUrl = parseURL(subscriberPropertyValue);
            tcaAppPreferences.setSubscriberProtocol(subscriberUrl.getProtocol());
            tcaAppPreferences.setSubscriberHostName(subscriberUrl.getHost());
            final int subscriberUrlPort = subscriberUrl.getPort() != -1 ?
                    Integer.valueOf(subscriberUrl.getPort()) : getDefaultDMaaPPort(subscriberUrl.getProtocol());
            tcaAppPreferences.setSubscriberHostPort(subscriberUrlPort);
            tcaAppPreferences.setSubscriberTopicName(subscriberUrl.getPath().substring(1));
        }

        final String subscriberUserNamePropertyKey = "dmaap.in.event-input.dmaapUserName";
        if (runtimeArguments.containsKey(subscriberUserNamePropertyKey)) {
            tcaAppPreferences.setSubscriberUserName(runtimeArguments.get(subscriberUserNamePropertyKey));
        }
        final String subscriberPasswordPropertyKey = "dmaap.in.event-input.dmaapPassword";
        if (runtimeArguments.containsKey(subscriberPasswordPropertyKey)) {
            tcaAppPreferences.setSubscriberUserPassword(runtimeArguments.get(subscriberPasswordPropertyKey));
        }

        final String publisherPropertyKey = "dmaap.out.alert-output.dmaapUrl";
        final String publisherPropertyValue = runtimeArguments.get(publisherPropertyKey);
        if (publisherPropertyValue != null) {
            LOG.debug("Updating value for DMaaP Publisher to values provided in property: {} with value: {}",
                    publisherPropertyKey, publisherPropertyValue);
            final URL publisherUrl = parseURL(publisherPropertyValue);
            tcaAppPreferences.setPublisherProtocol(publisherUrl.getProtocol());
            tcaAppPreferences.setPublisherHostName(publisherUrl.getHost());
            final int publisherUrlPort = publisherUrl.getPort() != -1 ?
                    Integer.valueOf(publisherUrl.getPort()) : getDefaultDMaaPPort(publisherUrl.getProtocol());
            tcaAppPreferences.setPublisherHostPort(publisherUrlPort);
            tcaAppPreferences.setPublisherTopicName(publisherUrl.getPath().substring(1));
        }

        final String publisherUserNamePropertyKey = "dmaap.out.alert-output.dmaapUserName";
        if (runtimeArguments.containsKey(publisherUserNamePropertyKey)) {
            tcaAppPreferences.setPublisherUserName(runtimeArguments.get(publisherUserNamePropertyKey));
        }
        final String publisherPasswordPropertyKey = "dmaap.out.alert-output.dmaapPassword";
        if (runtimeArguments.containsKey(publisherPasswordPropertyKey)) {
            tcaAppPreferences.setPublisherUserPassword(runtimeArguments.get(publisherPasswordPropertyKey));
        }


        return tcaAppPreferences;
    }

    /**
     * Sets up default DMaaP Port if not provided with DMaaP URL
     *
     * @param protocol protocol e.g. http or https
     *
     * @return default DMaaP MR port number
     */
    private static int getDefaultDMaaPPort(final String protocol) {
        if ("http".equals(protocol)) {
            return 3904;
        } else if ("https".equals(protocol)) {
            return 3905;
        } else {
            return 80;
        }
    }

    /**
     * Parses provided DMaaP MR URL string to {@link URL} object
     *
     * @param urlString url string
     *
     * @return url object
     */
    private static URL parseURL(final String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            final String errorMessage = String.format("Invalid URL format: %s", urlString);
            throw new DCAEAnalyticsRuntimeException(errorMessage, LOG, e);
        }
    }


    /**
     * Creates a {@link GenericMessageChainProcessor} of {@link TCACEFJsonProcessor},
     * {@link TCACEFPolicyDomainFilter} and {@link TCACEFPolicyFunctionalRoleFilter}s to
     * filter out messages which does not match policy domain or functional role
     *
     * @param cefMessage CEF Message
     * @param tcaPolicy TCA Policy
     * @return Message Process Context after processing filter chain
     */
    public static TCACEFProcessorContext filterCEFMessage(@Nullable final String cefMessage,
                                                          @Nonnull final TCAPolicy tcaPolicy) {

        final TCACEFJsonProcessor jsonProcessor = new TCACEFJsonProcessor();
        final TCACEFPolicyDomainFilter domainFilter = new TCACEFPolicyDomainFilter();
        final TCACEFPolicyFunctionalRoleFilter functionalRoleFilter = new TCACEFPolicyFunctionalRoleFilter();
        // Create a list of message processors
        final ImmutableList<AbstractMessageProcessor<TCACEFProcessorContext>> messageProcessors =
                ImmutableList.of(jsonProcessor, domainFilter, functionalRoleFilter);
        final TCACEFProcessorContext processorContext = new TCACEFProcessorContext(cefMessage, tcaPolicy);
        // Create a message processors chain
        final GenericMessageChainProcessor<TCACEFProcessorContext> tcaProcessingChain =
                new GenericMessageChainProcessor<>(messageProcessors, processorContext);
        // process chain
        return tcaProcessingChain.processChain();
    }


    /**
     * Extracts json path values for given json Field Paths from using Json path notation. Assumes
     * that values extracted are always long
     *
     * @param message CEF Message
     * @param jsonFieldPaths Json Field Paths
     * @return Map containing key as json path and values as values associated with that json path
     */
    public static Map<String, List<Long>> getJsonPathValue(@Nonnull String message, @Nonnull Set<String>
            jsonFieldPaths) {

        final Map<String, List<Long>> jsonFieldPathMap = new HashMap<>();
        final DocumentContext documentContext = JsonPath.parse(message);

        for (String jsonFieldPath : jsonFieldPaths) {
            final List<Long> jsonFieldValues = documentContext.read(jsonFieldPath, new TypeRef<List<Long>>() {
            });
            // If Json Field Values are not or empty
            if (jsonFieldValues != null && !jsonFieldValues.isEmpty()) {
                // Filter out all null values in the filed values list
                final List<Long> nonNullValues = Lists.newLinkedList(Iterables.filter(jsonFieldValues,
                        Predicates.<Long>notNull()));
                // If there are non null values put them in the map
                if (!nonNullValues.isEmpty()) {
                    jsonFieldPathMap.put(jsonFieldPath, nonNullValues);
                }
            }
        }

        return jsonFieldPathMap;
    }

    /**
     * Computes if any CEF Message Fields have violated any Policy Thresholds. For the same policy field path
     * it applies threshold in order of their severity and record the first threshold per message field path
     *
     * @param messageFieldValues Field Path Values extracted from CEF Message
     * @param fieldThresholds Policy Thresholds for Field Path
     * @return Optional of violated threshold for a field path
     */
    public static Optional<Threshold> thresholdCalculator(final List<Long> messageFieldValues, final List<Threshold>
            fieldThresholds) {
        // order thresholds by severity
        Collections.sort(fieldThresholds, THRESHOLD_COMPARATOR);
        // Now apply each threshold to field values
        for (Threshold fieldThreshold : fieldThresholds) {
            for (Long messageFieldValue : messageFieldValues) {
                final Boolean isThresholdViolated =
                        fieldThreshold.getDirection().operate(messageFieldValue, fieldThreshold.getThresholdValue());
                if (isThresholdViolated) {
                    return Optional.of(fieldThreshold);
                }
            }
        }
        return Optional.absent();
    }

    /**
     * Prioritize Threshold to be reported in case there was multiple TCA violations in a single CEF message.
     * Grabs first highest priority violated threshold
     *
     * @param violatedThresholdsMap Map containing field Path and associated violated Thresholds
     * @return First Highest priority violated threshold
     */
    public static Threshold prioritizeThresholdViolations(final Map<String, Threshold> violatedThresholdsMap) {

        final List<Threshold> violatedThresholds = newArrayList(violatedThresholdsMap.values());

        if (violatedThresholds.size() == 1) {
            return violatedThresholds.get(0);
        }
        Collections.sort(violatedThresholds, THRESHOLD_COMPARATOR);
        // Just grab the first violated threshold with highest priority
        return violatedThresholds.get(0);
    }


    /**
     * Creates {@link MetricsPerFunctionalRole} object which contains violated thresholds
     *
     * @param tcaPolicy TCA Policy
     * @param violatedThreshold Violated thresholds
     * @param functionalRole Functiona Role
     *
     * @return MetricsPerFunctionalRole object containing one highest severity violated threshold
     */
    public static MetricsPerFunctionalRole createViolatedMetrics(@Nonnull final TCAPolicy tcaPolicy,
                                                                 @Nonnull final Threshold violatedThreshold,
                                                                 @Nonnull final String functionalRole) {

        final ArrayList<MetricsPerFunctionalRole> metricsPerFunctionalRoles = newArrayList(
                Iterables.filter(tcaPolicy.getMetricsPerFunctionalRole(), new Predicate<MetricsPerFunctionalRole>() {
                    @Override
                    public boolean apply(@Nonnull MetricsPerFunctionalRole metricsPerFunctionalRole) {
                        return metricsPerFunctionalRole.getFunctionalRole().equals(functionalRole);
                    }
                }));
        // TCA policy must have only one metrics role per functional role
        if (metricsPerFunctionalRoles.size() == 1) {
            final MetricsPerFunctionalRole policyMetrics = metricsPerFunctionalRoles.get(0);
            final MetricsPerFunctionalRole violatedMetrics = new MetricsPerFunctionalRole();
            violatedMetrics.setFunctionalRole(policyMetrics.getFunctionalRole());
            violatedMetrics.setPolicyScope(policyMetrics.getPolicyScope());
            violatedMetrics.setPolicyName(policyMetrics.getPolicyName());
            violatedMetrics.setPolicyVersion(policyMetrics.getPolicyVersion());
            violatedMetrics.setThresholds(ImmutableList.of(violatedThreshold));
            return violatedMetrics;
        } else {
            final String errorMessage = String.format("TCA Policy must contain functional Role: %s", functionalRole);
            throw new MessageProcessingException(errorMessage, LOG, new IllegalStateException(errorMessage));
        }
    }

    /**
     * Computes threshold violations
     *
     * @param processorContext Filtered processor Context
     * @return processor context with any threshold violations
     */
    public static TCACEFProcessorContext computeThresholdViolations(final TCACEFProcessorContext processorContext) {
        final TCACEFPolicyThresholdsProcessor policyThresholdsProcessor = new TCACEFPolicyThresholdsProcessor();
        return policyThresholdsProcessor.apply(processorContext);
    }

    /**
     * Creates {@link TCAVESResponse} object
     *
     * @param processorContext processor Context with violations
     * @param tcaAppName TCA App Name
     *
     * @return TCA VES Response Message
     */
    public static TCAVESResponse createNewTCAVESResponse(final TCACEFProcessorContext processorContext,
                                                         final String tcaAppName) {

        final MetricsPerFunctionalRole metricsPerFunctionalRole = processorContext.getMetricsPerFunctionalRole();
        // confirm violations are indeed present
        if (metricsPerFunctionalRole == null) {
            final String errorMessage = "No violations metrics. Unable to create VES Response";
            throw new MessageProcessingException(errorMessage, LOG, new IllegalArgumentException(errorMessage));
        }

        final String functionalRole = metricsPerFunctionalRole.getFunctionalRole();
        final Threshold violatedThreshold = metricsPerFunctionalRole.getThresholds().get(0);
        final EventListener eventListener = processorContext.getCEFEventListener();
        final CommonEventHeader commonEventHeader = eventListener.getEvent().getCommonEventHeader();

        final TCAVESResponse tcavesResponse = new TCAVESResponse();
        // ClosedLoopControlName included in the DCAE configuration Policy
        tcavesResponse.setClosedLoopControlName(violatedThreshold.getClosedLoopControlName());
        // version included in the DCAE configuration Policy
        tcavesResponse.setVersion(violatedThreshold.getVersion());
        // Generate a UUID for this output message
        tcavesResponse.setRequestID(UUID.randomUUID().toString());
        // commonEventHeader.startEpochMicrosec from the received VES measurementsForVfScaling message
        tcavesResponse.setClosedLoopAlarmStart(commonEventHeader.getStartEpochMicrosec());
        // Concatenate name of this DCAE instance and name for this TCA instance, separated by dot
        // TODO: Find out how to get this field
        tcavesResponse.setClosedLoopEventClient("DCAE_INSTANCE_ID." + tcaAppName);

        final AAI aai = new AAI();
        tcavesResponse.setAai(aai);

        // vLoadBalancer specific settings
        if (isFunctionalRoleVLoadBalancer(functionalRole)) {
            // Hard Coded - "VM"
            tcavesResponse.setTargetType(AnalyticsConstants.LOAD_BALANCER_TCA_VES_RESPONSE_TARGET_TYPE);
            // Hard Coded - "vserver.vserver-name"
            tcavesResponse.setTarget(AnalyticsConstants.LOAD_BALANCER_TCA_VES_RESPONSE_TARGET);
            aai.setGenericServerId(commonEventHeader.getReportingEntityName());
        } else {
            // Hard Coded - "VNF"
            tcavesResponse.setTargetType(AnalyticsConstants.TCA_VES_RESPONSE_TARGET_TYPE);
            // Hard Coded - "generic-vnf.vnf-id"
            tcavesResponse.setTarget(AnalyticsConstants.TCA_VES_RESPONSE_TARGET);
            // commonEventHeader.reportingEntityName from the received VES measurementsForVfScaling message (value for
            // the data element used in A&AI)
            aai.setGenericVNFId(commonEventHeader.getReportingEntityName());
        }

        // Hard Coded - "DCAE"
        tcavesResponse.setFrom(AnalyticsConstants.TCA_VES_RESPONSE_FROM);
        // policyScope included in the DCAE configuration Policy
        tcavesResponse.setPolicyScope(metricsPerFunctionalRole.getPolicyScope());
        // policyName included in the DCAE configuration Policy
        tcavesResponse.setPolicyName(metricsPerFunctionalRole.getPolicyName());
        // policyVersion included in the DCAE configuration Policy
        tcavesResponse.setPolicyVersion(metricsPerFunctionalRole.getPolicyVersion());
        // Hard Coded - "ONSET"
        tcavesResponse.setClosedLoopEventStatus(AnalyticsConstants.TCA_VES_RESPONSE_CLOSED_LOOP_EVENT_STATUS);

        return tcavesResponse;
    }

    /**
     * Determines if Functional Role is vLoadBlanacer
     *
     * @param functionalRole functional Role to check
     *
     * @return return true if functional role is for vLoadBalancer
     */
    private static boolean isFunctionalRoleVLoadBalancer(final String functionalRole) {
        return functionalRole.equals(AnalyticsConstants.LOAD_BALANCER_FUNCTIONAL_ROLE);
    }


    /**
     * Extract Domain and functional Role from processor context if present
     *
     * @param processorContext processor context
     * @return Tuple of domain and functional role
     */
    public static Pair<String, String> getDomainAndFunctionalRole(@Nullable final TCACEFProcessorContext
                                                                          processorContext) {

        String domain = null;
        String functionalRole = null;


        if (processorContext != null &&
                processorContext.getCEFEventListener() != null &&
                processorContext.getCEFEventListener().getEvent() != null &&
                processorContext.getCEFEventListener().getEvent().getCommonEventHeader() != null) {
            final CommonEventHeader commonEventHeader = processorContext.getCEFEventListener().getEvent()
                    .getCommonEventHeader();

            if (commonEventHeader.getDomain() != null) {
                domain = commonEventHeader.getDomain();
            }

            if (commonEventHeader.getFunctionalRole() != null) {
                functionalRole = commonEventHeader.getFunctionalRole();
            }

        }

        return new ImmutablePair<>(domain, functionalRole);

    }

    /**
     * Function that extracts alert message string from {@link TCAVESAlertEntity}
     */
    public static final Function<TCAVESAlertEntity, String> MAP_ALERT_ENTITY_TO_ALERT_STRING_FUNCTION =
            new Function<TCAVESAlertEntity, String>() {
                @Override
                public String apply(TCAVESAlertEntity alertEntity) {
                    return alertEntity == null ? null : alertEntity.getAlertMessage();
                }
            };

    /**
     * Extracts alert message strings from {@link TCAVESAlertEntity}
     * @param alertEntities collection of alert entities
     * @return List of alert message strings
     */
    public static List<String> extractAlertFromAlertEntities(final Collection<TCAVESAlertEntity> alertEntities) {
        return Lists.transform(newArrayList(alertEntities), MAP_ALERT_ENTITY_TO_ALERT_STRING_FUNCTION);
    }


    /**
     * Converts Runtime Arguments to {@link TCAPolicyPreferences} object
     *
     * @param runtimeContext CDAP Runtime Arguments
     *
     * @return TCA Policy Preferences
     */
    public static TCAPolicy getValidatedTCAPolicyPreferences(final RuntimeContext runtimeContext) {

        final Map<String, String> runtimeArguments = runtimeContext.getRuntimeArguments();
        final TreeMap<String, String> sortedRuntimeArguments = new TreeMap<>(runtimeArguments);

        LOG.debug("Printing all Received Runtime Arguments:");
        for (Map.Entry<String, String> runtimeArgsEntry : sortedRuntimeArguments.entrySet()) {
            LOG.debug("{}:{}", runtimeArgsEntry.getKey(), runtimeArgsEntry.getValue());
        }

        // extract TCA Policy Domain from Runtime Arguments
        final String policyDomain = sortedRuntimeArguments.get(AnalyticsConstants.TCA_POLICY_DOMAIN_PATH);

        // create new TCA Policy object
        final TCAPolicyPreferences tcaPolicyPreferences = new TCAPolicyPreferences();
        tcaPolicyPreferences.setDomain(policyDomain);

        // filter out other non relevant fields which are not related to tca policy
        final Map<String, String> tcaPolicyMap = filterMapByKeyNamePrefix(sortedRuntimeArguments,
                TCA_POLICY_METRICS_PER_FUNCTIONAL_ROLE_PATH);

        // determine functional Roles
        final Map<String, Map<String, String>> functionalRolesMap =
                extractSubTree(tcaPolicyMap, 2, 3, AnalyticsConstants.TCA_POLICY_DELIMITER);

        // create metrics per functional role list
        tcaPolicyPreferences.setMetricsPerFunctionalRole(
                createTCAPolicyMetricsPerFunctionalRoleList(functionalRolesMap));

        // validate tca Policy Preferences
        validateSettings(tcaPolicyPreferences, new TCAPolicyPreferencesValidator());

        LOG.info("Printing Effective TCA Policy: {}", tcaPolicyPreferences);

        return tcaPolicyPreferences;
    }

    /**
     * Creates {@link TCAPolicy} Metrics per Functional Role list
     *
     * @param functionalRolesMap Map containing functional Roles as key and corresponding values
     *
     * @return List of {@link MetricsPerFunctionalRole}
     */
    public static List<MetricsPerFunctionalRole> createTCAPolicyMetricsPerFunctionalRoleList(
            final Map<String, Map<String, String>> functionalRolesMap) {

        // create a new metrics per functional role list
        final List<MetricsPerFunctionalRole> metricsPerFunctionalRoles = new LinkedList<>();

        for (Map.Entry<String, Map<String, String>> functionalRolesEntry : functionalRolesMap.entrySet()) {

            // create new metrics per functional role instance
            final MetricsPerFunctionalRole newMetricsPerFunctionalRole =
                    createNewMetricsPerFunctionalRole(functionalRolesEntry);
            metricsPerFunctionalRoles.add(newMetricsPerFunctionalRole);

            // determine all threshold related values
            final Map<String, String> thresholdsValuesMaps =
                    filterMapByKeyNamePrefix(functionalRolesEntry.getValue(),
                            AnalyticsConstants.TCA_POLICY_THRESHOLDS_PATH_POSTFIX);

            // create a map of all threshold values
            final Map<String, Map<String, String>> thresholdsMap =
                    extractSubTree(thresholdsValuesMaps, 1, 2,
                            AnalyticsConstants.TCA_POLICY_DELIMITER);

            // add thresholds to nmetrics per functional roles threshold list
            for (Map<String, String> thresholdMap : thresholdsMap.values()) {
                newMetricsPerFunctionalRole.getThresholds().add(createNewThreshold(thresholdMap));
            }

        }

        return metricsPerFunctionalRoles;
    }

    /**
     * Creates new instance of TCA Policy {@link Threshold} with values extracted from thresholdMap
     *
     * @param thresholdMap threshold map with threshold values
     *
     * @return new instance of TCA Policy Threshold
     */
    public static Threshold createNewThreshold(final Map<String, String> thresholdMap) {
        final Threshold threshold = new Threshold();
        threshold.setClosedLoopControlName(thresholdMap.get("policy.closedLoopControlName"));
        threshold.setVersion(thresholdMap.get("policy.version"));
        threshold.setFieldPath(thresholdMap.get("policy.fieldPath"));
        threshold.setDirection(Direction.valueOf(thresholdMap.get("policy.direction")));
        threshold.setSeverity(EventSeverity.valueOf(thresholdMap.get("policy.severity")));
        threshold.setThresholdValue(Long.valueOf(thresholdMap.get("policy.thresholdValue")));
        return threshold;
    }

    /**
     * Create new {@link MetricsPerFunctionalRole} instance with policy Name, policy Version and policy Scope
     * extracted from given functionalRolesEntry
     *
     * @param functionalRolesEntry Functional Role Entry
     *
     * @return new instance of MetricsPerFunctionalRole
     */
    public static MetricsPerFunctionalRole createNewMetricsPerFunctionalRole(
            final Map.Entry<String, Map<String, String>> functionalRolesEntry) {
        // determine functional Role
        final String functionalRole = functionalRolesEntry.getKey();
        // determine functional Role thresholds
        final Map<String, String> metricsPerFunctionalRoleThresholdsMap = functionalRolesEntry.getValue();
        final MetricsPerFunctionalRole metricsPerFunctionalRole = new MetricsPerFunctionalRole();
        final List<Threshold> thresholds = new LinkedList<>();
        metricsPerFunctionalRole.setThresholds(thresholds);
        metricsPerFunctionalRole.setFunctionalRole(functionalRole);
        // bind policyName, policyVersion and policyScope
        metricsPerFunctionalRole.setPolicyName(metricsPerFunctionalRoleThresholdsMap.get("policyName"));
        metricsPerFunctionalRole.setPolicyVersion(metricsPerFunctionalRoleThresholdsMap.get("policyVersion"));
        metricsPerFunctionalRole.setPolicyScope(metricsPerFunctionalRoleThresholdsMap.get("policyScope"));
        return metricsPerFunctionalRole;
    }

    /**
     * Converts a flattened key/value map which has keys delimited by a given delimiter.
     * The start Index and end index extract the sub-key value and returns a new map containing
     * sub-keys and values.
     *
     * @param actualMap actual Map
     * @param startIndex start index
     * @param endIndex end index
     * @param delimiter delimiter
     *
     * @return Map with new sub tree map
     */
    public static Map<String, Map<String, String>> extractSubTree(
            final Map<String, String> actualMap, int startIndex, int endIndex, String delimiter) {

        final SortedMap<String, Map<String, String>> subTreeMap = new TreeMap<>();

        // iterate over actual map entries
        for (Map.Entry<String, String> actualMapEntry : actualMap.entrySet()) {
            final String actualMapKey = actualMapEntry.getKey();
            final String actualMapValue = actualMapEntry.getValue();

            // determine delimiter start and end index
            final int keyStartIndex = StringUtils.ordinalIndexOf(actualMapKey, delimiter, startIndex);
            final int keyEndIndex = StringUtils.ordinalIndexOf(actualMapKey, delimiter, endIndex);
            final int keyLength = actualMapKey.length();

            // extract sub-tree map
            if (keyStartIndex != -1 && keyEndIndex != -1 && keyEndIndex > keyStartIndex && keyLength > keyEndIndex) {
                final String thresholdKey = actualMapKey.substring(keyStartIndex + 1, keyEndIndex);
                final Map<String, String> existingThresholdMap = subTreeMap.get(thresholdKey);
                final String subMapKey = actualMapKey.substring(keyEndIndex + 1, keyLength);
                if (existingThresholdMap == null) {
                    Map<String, String> newThresholdMap = new LinkedHashMap<>();
                    newThresholdMap.put(subMapKey, actualMapValue);
                    subTreeMap.put(thresholdKey, newThresholdMap);
                } else {
                    existingThresholdMap.put(subMapKey, actualMapValue);
                }

            }
        }

        return subTreeMap;

    }


    /**
     * Provides a view of underlying map that filters out entries with keys starting with give prefix
     *
     * @param actualMap Target map that needs to be filtered
     * @param keyNamePrefix key prefix
     *
     * @return a view of actual map which only show entries which have give prefix
     */
    public static Map<String, String> filterMapByKeyNamePrefix(final Map<String, String> actualMap,
                                                               final String keyNamePrefix) {
        return Maps.filterKeys(actualMap,
                new Predicate<String>() {
                    @Override
                    public boolean apply(@Nullable String key) {
                        return key != null && key.startsWith(keyNamePrefix);
                    }
                });
    }


}
