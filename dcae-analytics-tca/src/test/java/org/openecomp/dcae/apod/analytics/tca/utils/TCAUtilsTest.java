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

package org.openecomp.dcae.apod.analytics.tca.utils;

import co.cask.cdap.api.RuntimeContext;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openecomp.dcae.apod.analytics.common.exception.MessageProcessingException;
import org.openecomp.dcae.apod.analytics.model.domain.cef.CommonEventHeader;
import org.openecomp.dcae.apod.analytics.model.domain.cef.Event;
import org.openecomp.dcae.apod.analytics.model.domain.cef.EventListener;
import org.openecomp.dcae.apod.analytics.model.domain.cef.EventSeverity;
import org.openecomp.dcae.apod.analytics.model.domain.policy.tca.MetricsPerFunctionalRole;
import org.openecomp.dcae.apod.analytics.model.domain.policy.tca.TCAPolicy;
import org.openecomp.dcae.apod.analytics.model.domain.policy.tca.Threshold;
import org.openecomp.dcae.apod.analytics.model.facade.tca.TCAVESResponse;
import org.openecomp.dcae.apod.analytics.model.util.AnalyticsModelIOUtils;
import org.openecomp.dcae.apod.analytics.tca.BaseAnalyticsTCAUnitTest;
import org.openecomp.dcae.apod.analytics.tca.processor.TCACEFProcessorContext;
import org.openecomp.dcae.apod.analytics.tca.settings.TCAAppPreferences;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Rajiv Singla. Creation Date: 11/9/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class TCAUtilsTest extends BaseAnalyticsTCAUnitTest {

    @Test
    public void testGetPolicyFunctionalRoles() throws Exception {

        final TCAPolicy sampleTCAPolicy = getSampleTCAPolicy();
        final List<String> policyFunctionalRoles = TCAUtils.getPolicyFunctionalRoles(sampleTCAPolicy);

        assertThat("Policy Functional Roles must contain vFirewall and vLoadBalancer", policyFunctionalRoles,
                containsInAnyOrder("vFirewall", "vLoadBalancer"));
    }

    @Test
    public void testGetPolicyFunctionalRoleSupplier() throws Exception {
        final TCAPolicy sampleTCAPolicy = getSampleTCAPolicy();
        final Supplier<List<String>> policyFunctionalRoleSupplier = TCAUtils.getPolicyFunctionalRoleSupplier
                (sampleTCAPolicy);
        final List<String> policyFunctionalRoles = policyFunctionalRoleSupplier.get();
        assertThat("Policy Functional Roles must contain vFirewall and vLoadBalancer", policyFunctionalRoles,
                containsInAnyOrder("vFirewall", "vLoadBalancer"));
    }

    @Test
    public void testProcessCEFMessage() throws Exception {
        final String cefMessageString = fromStream(CEF_MESSAGE_JSON_FILE_LOCATION);
        final TCACEFProcessorContext tcacefProcessorContext = TCAUtils.filterCEFMessage(cefMessageString,
                getSampleTCAPolicy());
        assertThat("TCAECEFProcessor Processor Context can continue flag is true", tcacefProcessorContext
                .canProcessingContinue(), is(true));
    }

    @Test
    public void testGetPolicyFRThresholdsTableSupplier() throws Exception {
        final Table<String, String, List<Threshold>> policyFRThresholdPathTable = TCAUtils
                .getPolicyFRThresholdsTableSupplier(getSampleTCAPolicy()).get();

        final Map<String, List<Threshold>> vFirewall = policyFRThresholdPathTable.row("vFirewall");
        final Map<String, List<Threshold>> vLoadBalancer = policyFRThresholdPathTable.row("vLoadBalancer");

        final Set<String> vFirewallThresholdPaths = vFirewall.keySet();
        final Set<String> vLoadBalancerPaths = vLoadBalancer.keySet();

        assertThat("vFirewall threshold field path size must be " +
                        "\"$.event.measurementsForVfScalingFields.vNicUsageArray[*].bytesIn\"",
                vFirewallThresholdPaths.iterator().next(),
                is("$.event.measurementsForVfScalingFields.vNicUsageArray[*].bytesIn"));

        assertThat("vLoadBalancer threshold field path size must be " +
                        "\"\"$.event.measurementsForVfScalingFields.vNicUsageArray[*].packetsIn\"",
                vLoadBalancerPaths.iterator().next(),
                is("$.event.measurementsForVfScalingFields.vNicUsageArray[*].packetsIn"));

        final List<Threshold> firewallThresholds = policyFRThresholdPathTable.get("vFirewall",
                "$.event.measurementsForVfScalingFields.vNicUsageArray[*].bytesIn");
        final List<Threshold> vLoadBalancerThresholds = policyFRThresholdPathTable.get("vLoadBalancer",
                "$.event.measurementsForVfScalingFields.vNicUsageArray[*].packetsIn");

        assertThat("vFirewall Threshold size must be 2", firewallThresholds.size(), is(2));
        assertThat("vLoadBalancer Threshold size must be 2", vLoadBalancerThresholds.size(), is(2));
    }

    @Test
    public void testGetJsonPathValueWithValidMessageAndPolicy() throws Exception {
        final String cefMessageString = fromStream(CEF_MESSAGE_JSON_FILE_LOCATION);
        final String jsonPath = "$.event.measurementsForVfScalingFields.vNicUsageArray[*].bytesIn";
        final ImmutableSet<String> fieldPaths = ImmutableSet.of(jsonPath);
        final Map<String, List<Long>> jsonPathValueMap = TCAUtils.getJsonPathValue(cefMessageString, fieldPaths);
        assertThat("Json Path value must match", jsonPathValueMap.get(jsonPath).get(0), is(6086L));

    }

    @Test
    public void testGetJsonPathValueWithValidPath() throws Exception {
        final String cefMessageString = fromStream(CEF_MESSAGE_JSON_FILE_LOCATION);
        final String jsonPath = "$.event.measurementsForVfScalingFields.vNicUsageArray[*].invalid";
        final ImmutableSet<String> fieldPaths = ImmutableSet.of(jsonPath);
        final Map<String, List<Long>> jsonPathValueMap = TCAUtils.getJsonPathValue(cefMessageString, fieldPaths);
        assertThat("Json path value must be empty", jsonPathValueMap.size(), is(0));

    }

    @Test
    public void testGetValidatedTCAAppPreferences() throws Exception {
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        when(runtimeContext.getRuntimeArguments()).thenReturn(getPreferenceMap());
        TCAAppPreferences validatedTCAAppPreferences = TCAUtils.getValidatedTCAAppPreferences(runtimeContext);
        assertEquals(validatedTCAAppPreferences.getSubscriberHostName(), "mrlocal-mtnjftle01.homer.com");
    }

    @Test
    public void testGetValidatedTCAAppPreferencesWhenDMaaPUrlArePresent() throws Exception {
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        final Map<String, String> preferenceMap = getPreferenceMap();
        preferenceMap.put("dmaap.in.event-input.dmaapUrl",
                "http://zldcmtd1njcoll00.research.com/unauthenticated.SEC_MEASUREMENT_OUTPUT");
        preferenceMap.put("dmaap.out.alert-output.dmaapUrl",
                "http://zldcmtd1njcoll00.research.com/unauthenticated.TCA_EVENT_OUTPUT");

        preferenceMap.put("dmaap.in.event-input.dmaapUserName", null);
        preferenceMap.put("dmaap.in.event-input.dmaapPassword", null);
        preferenceMap.put("dmaap.out.alert-output.dmaapUserName", null);
        preferenceMap.put("dmaap.out.alert-output.dmaapPassword", null);

        when(runtimeContext.getRuntimeArguments()).thenReturn(preferenceMap);
        TCAAppPreferences validatedTCAAppPreferences = TCAUtils.getValidatedTCAAppPreferences(runtimeContext);

        assertEquals(validatedTCAAppPreferences.getSubscriberProtocol(), "http");
        assertEquals(validatedTCAAppPreferences.getPublisherProtocol(), "http");

        assertEquals(validatedTCAAppPreferences.getSubscriberHostName(), "zldcmtd1njcoll00.research.com");
        assertEquals(validatedTCAAppPreferences.getPublisherHostName(), "zldcmtd1njcoll00.research.com");

        assertEquals(validatedTCAAppPreferences.getSubscriberHostPort(), new Integer(3904));
        assertEquals(validatedTCAAppPreferences.getPublisherHostPort(), new Integer(3904));

        assertEquals(validatedTCAAppPreferences.getSubscriberTopicName(), "unauthenticated.SEC_MEASUREMENT_OUTPUT");
        assertEquals(validatedTCAAppPreferences.getPublisherTopicName(), "unauthenticated.TCA_EVENT_OUTPUT");

        assertNull(validatedTCAAppPreferences.getSubscriberUserName());
        assertNull(validatedTCAAppPreferences.getSubscriberUserPassword());
        assertNull(validatedTCAAppPreferences.getPublisherUserName());
        assertNull(validatedTCAAppPreferences.getPublisherUserPassword());

    }

    @Test
    public void testCreateNewTCAVESResponse() throws Exception {
        TCACEFProcessorContext tcacefProcessorContext = mock(TCACEFProcessorContext.class);

        MetricsPerFunctionalRole metricsPerFunctionalRole = mock(MetricsPerFunctionalRole.class);
        when(metricsPerFunctionalRole.getThresholds()).thenReturn(getThresholds());
        when(metricsPerFunctionalRole.getPolicyScope()).thenReturn("Test Policy scope");
        when(tcacefProcessorContext.getMetricsPerFunctionalRole()).thenReturn(metricsPerFunctionalRole);
        when(metricsPerFunctionalRole.getFunctionalRole()).thenReturn("vLoadBalancer");

        when(tcacefProcessorContext.getCEFEventListener()).thenReturn(getCEFEventListener());
        TCAVESResponse tcaVESResponse = TCAUtils.createNewTCAVESResponse(tcacefProcessorContext, "TCA_APP_NAME");

        //TODO :  Add proper assertions, as the usage is not clearly understood
        assertThat(tcaVESResponse.getClosedLoopControlName(),
                is("CL-LBAL-LOW-TRAFFIC-SIG-FB480F95-A453-6F24-B767-FD703241AB1A"));
        assertThat(tcaVESResponse.getVersion(), is("Test Version"));
        assertThat(tcaVESResponse.getPolicyScope(), is("Test Policy scope"));
    }

    @Rule
    public ExpectedException expectedIllegalArgumentException = ExpectedException.none();

    @Test
    public void testCreateNewTCAVESResponseNullFunctionalRole() throws Exception {
        expectedIllegalArgumentException.expect(MessageProcessingException.class);
        expectedIllegalArgumentException.expectCause(isA(IllegalArgumentException.class));
        expectedIllegalArgumentException.expectMessage("No violations metrics. Unable to create VES Response");

        TCACEFProcessorContext tcacefProcessorContext = mock(TCACEFProcessorContext.class);
        TCAVESResponse tcaVESResponse = TCAUtils.createNewTCAVESResponse(tcacefProcessorContext, "TCA_APP_NAME");
        assertNotNull(tcaVESResponse.getClosedLoopControlName());
    }

    @Test
    public void testPrioritizeThresholdViolations() throws Exception {

        Map<String, Threshold> thresholdMap = new HashMap<>();
        Threshold majorThreshold = mock(Threshold.class);
        when(majorThreshold.getSeverity()).thenReturn(EventSeverity.MAJOR);
        thresholdMap.put("MAJOR", majorThreshold);

        Threshold result1 = TCAUtils.prioritizeThresholdViolations(thresholdMap);
        assertEquals(result1.getSeverity(), EventSeverity.MAJOR);

        Threshold criticalThreshold = mock(Threshold.class);
        when(criticalThreshold.getSeverity()).thenReturn(EventSeverity.CRITICAL);
        thresholdMap.put("CRITICAL", criticalThreshold);

        Threshold result2 = TCAUtils.prioritizeThresholdViolations(thresholdMap);
        assertEquals(result2.getSeverity(), EventSeverity.CRITICAL);
    }

    @Test
    public void testCreateViolatedMetrics() throws Exception {
        TCAPolicy tcaPolicy = getSampleTCAPolicy();
        Threshold violatedThreshold = getCriticalThreshold();
        String functionalRole = "vFirewall";
        MetricsPerFunctionalRole result = TCAUtils.createViolatedMetrics(tcaPolicy, violatedThreshold, functionalRole);
        assertThat(result.getPolicyScope(), is("resource=vFirewall;type=configuration"));
        assertThat(result.getPolicyName(), is("configuration.dcae.microservice.tca.xml"));
    }

    @Test
    public void testCreateViolatedMetricsWrongFunctionalRole() throws Exception {
        expectedIllegalArgumentException.expect(MessageProcessingException.class);
        expectedIllegalArgumentException.expectCause(isA(IllegalStateException.class));
        expectedIllegalArgumentException.expectMessage("TCA Policy must contain functional Role: badFunctionRoleName");

        TCAPolicy tcaPolicy = getSampleTCAPolicy();
        Threshold violatedThreshold = getCriticalThreshold();
        String functionalRole = "badFunctionRoleName";
        MetricsPerFunctionalRole result = TCAUtils.createViolatedMetrics(tcaPolicy, violatedThreshold, functionalRole);
    }

    @Test
    public void testGetDomainAndFunctionalRole() {
        TCACEFProcessorContext tcacefProcessorContext = mock(TCACEFProcessorContext.class);
        EventListener eventListener = mock(EventListener.class);
        Event event = mock(Event.class);
        CommonEventHeader commonEventHeader = mock(CommonEventHeader.class);

        Pair<String, String> result = TCAUtils.getDomainAndFunctionalRole(tcacefProcessorContext);
        assertNull(result.getLeft());
        assertNull(result.getRight());

        when(tcacefProcessorContext.getCEFEventListener()).thenReturn(eventListener);
        result = TCAUtils.getDomainAndFunctionalRole(tcacefProcessorContext);
        assertNull(result.getLeft());
        assertNull(result.getRight());

        when(eventListener.getEvent()).thenReturn(event);
        result = TCAUtils.getDomainAndFunctionalRole(tcacefProcessorContext);
        assertNull(result.getLeft());
        assertNull(result.getRight());

        when(event.getCommonEventHeader()).thenReturn(commonEventHeader);
        result = TCAUtils.getDomainAndFunctionalRole(tcacefProcessorContext);
        assertNull(result.getLeft());
        assertNull(result.getRight());

        when(commonEventHeader.getDomain()).thenReturn("testDomain");
        when(commonEventHeader.getFunctionalRole()).thenReturn("functionalRole");

        result = TCAUtils.getDomainAndFunctionalRole(tcacefProcessorContext);
        assertEquals(result.getLeft(), "testDomain");
        assertEquals(result.getRight(), "functionalRole");

    }

    @Test
    public void testComputeThresholdViolationsNotPresent() throws Exception {
        TCACEFProcessorContext tcacefProcessorContext = mock(TCACEFProcessorContext.class);
        when(tcacefProcessorContext.canProcessingContinue()).thenReturn(true);
        when(tcacefProcessorContext.getMessage()).thenReturn(getValidCEFMessage());

        when(tcacefProcessorContext.getTCAPolicy()).thenReturn(getSampleTCAPolicy());
        when(tcacefProcessorContext.getCEFEventListener()).thenReturn(getCEFEventListener());

        TCACEFProcessorContext result = TCAUtils.computeThresholdViolations(tcacefProcessorContext);
        assertNotNull(result);
        verify(result, times(0)).setMetricsPerFunctionalRole(Mockito.any(MetricsPerFunctionalRole.class));
    }

    @Test
    public void testComputeThresholdViolationsPresent() throws Exception {
        TCACEFProcessorContext tcacefProcessorContext = mock(TCACEFProcessorContext.class);
        when(tcacefProcessorContext.canProcessingContinue()).thenReturn(true);
        final String cefMessageString = fromStream(CEF_MESSAGE_WITH_THRESHOLD_VIOLATION_JSON_FILE_LOCATION);
        when(tcacefProcessorContext.getMessage()).thenReturn(cefMessageString);

        when(tcacefProcessorContext.getTCAPolicy()).thenReturn(getSampleTCAPolicy());
        when(tcacefProcessorContext.getCEFEventListener()).thenReturn(getCEFEventListener());

        TCACEFProcessorContext result = TCAUtils.computeThresholdViolations(tcacefProcessorContext);
        verify(result, times(1)).setMetricsPerFunctionalRole(Mockito.any(MetricsPerFunctionalRole.class));
    }


    @Test
    public void testConvertRuntimeContextToTCAPolicy() throws Exception {

        final Properties controllerProperties =
                AnalyticsModelIOUtils.loadPropertiesFile(TCA_CONTROLLER_POLICY_FILE_LOCATION);

        Map<String, String> runtimeArgs = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> property : controllerProperties.entrySet()) {
            runtimeArgs.put(property.getKey().toString(), property.getValue().toString());
        }

        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        when(runtimeContext.getRuntimeArguments()).thenReturn(runtimeArgs);
        final TCAPolicy tcaPolicy = TCAUtils.getValidatedTCAPolicyPreferences(runtimeContext);

        assertThat("Policy Domain must be measurementsForVfScaling",
                tcaPolicy.getDomain(), is("measurementsForVfScaling"));

        assertThat("Policy must have 2 metrics per functional roles",
                tcaPolicy.getMetricsPerFunctionalRole().size(), is(2));

    }
}
