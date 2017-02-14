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

package org.openecomp.dcae.analytics.tca.flowlet;

import co.cask.cdap.api.annotation.Output;
import co.cask.cdap.api.annotation.ProcessInput;
import co.cask.cdap.api.annotation.Property;
import co.cask.cdap.api.annotation.RoundRobin;
import co.cask.cdap.api.dataset.lib.ObjectMappedTable;
import co.cask.cdap.api.flow.flowlet.AbstractFlowlet;
import co.cask.cdap.api.flow.flowlet.FlowletContext;
import co.cask.cdap.api.flow.flowlet.OutputEmitter;
import co.cask.cdap.api.metrics.Metrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.openecomp.dcae.analytics.common.CDAPComponentsConstants;
import org.openecomp.dcae.analytics.common.CDAPMetricsConstants;
import org.openecomp.dcae.analytics.model.domain.policy.tca.TCAPolicy;
import org.openecomp.dcae.analytics.model.facade.tca.TCAVESResponse;
import org.openecomp.dcae.analytics.tca.persistance.TCACalculatorMessageType;
import org.openecomp.dcae.analytics.tca.persistance.TCAMessageStatusEntity;
import org.openecomp.dcae.analytics.tca.persistance.TCAMessageStatusPersister;
import org.openecomp.dcae.analytics.tca.processor.TCACEFProcessorContext;
import org.openecomp.dcae.analytics.tca.utils.TCAUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openecomp.dcae.analytics.common.CDAPComponentsConstants.TCA_FIXED_VES_THRESHOLD_VIOLATION_CALCULATOR_DESCRIPTION_FLOWLET;
import static org.openecomp.dcae.analytics.common.CDAPComponentsConstants.TCA_FIXED_VES_THRESHOLD_VIOLATION_CALCULATOR_NAME_FLOWLET;

/**
 * TCA VES Message Filter filters out messages which are not applicable for TCA as per TCA Policy
 *
 * @author Rajiv Singla. Creation Date: 11/3/2016.
 */
public class TCAVESThresholdViolationCalculatorFlowlet extends AbstractFlowlet {

    private static final Logger LOG = LoggerFactory.getLogger(TCAVESThresholdViolationCalculatorFlowlet.class);

    @Output(CDAPComponentsConstants.TCA_FIXED_VES_TCA_CALCULATOR_NAME_OUTPUT)
    private OutputEmitter<String> tcaAlertOutputEmitter;

    @Property
    private final String messageStatusTableName;

    private TCAPolicy tcaPolicy;
    private Metrics metrics;
    private ObjectMappedTable<TCAMessageStatusEntity> vesMessageStatusTable;

    public TCAVESThresholdViolationCalculatorFlowlet(String messageStatusTableName) {
        this.messageStatusTableName = messageStatusTableName;
    }

    @Override
    public void configure() {
        setName(TCA_FIXED_VES_THRESHOLD_VIOLATION_CALCULATOR_NAME_FLOWLET);
        setDescription(TCA_FIXED_VES_THRESHOLD_VIOLATION_CALCULATOR_DESCRIPTION_FLOWLET);
    }


    @Override
    public void initialize(FlowletContext flowletContext) throws Exception {
        super.initialize(flowletContext);

        // parse Runtime Arguments to tca policy preferences
        tcaPolicy = TCAUtils.getValidatedTCAPolicyPreferences(flowletContext);

        vesMessageStatusTable = getContext().getDataset(messageStatusTableName);

    }

    @ProcessInput(CDAPComponentsConstants.TCA_FIXED_VES_MESSAGE_ROUTER_OUTPUT)
    @RoundRobin
    public void filterVESMessages(String vesMessage) throws JsonProcessingException {

        TCACalculatorMessageType calculatorMessageType = TCACalculatorMessageType.INAPPLICABLE;
        String alertMessage = null;

        // Step 1: Filter incoming messages
        final TCACEFProcessorContext processorContext = TCAUtils.filterCEFMessage(vesMessage, tcaPolicy);

        if (processorContext.canProcessingContinue()) {

            // Step 2: Check if CEF Message violate any thresholds
            final TCACEFProcessorContext processorContextWithViolations =
                    TCAUtils.computeThresholdViolations(processorContext);

            if (processorContextWithViolations.canProcessingContinue()) {

                // Step 3: Create Alert Message
                final String tcaAppName = getContext().getApplicationSpecification().getName();
                final TCAVESResponse newTCAVESResponse =
                        TCAUtils.createNewTCAVESResponse(processorContextWithViolations, tcaAppName);
                alertMessage = TCAUtils.writeValueAsString(newTCAVESResponse);
                calculatorMessageType = TCACalculatorMessageType.NON_COMPLIANT;
                LOG.debug("VES Threshold Violation Detected. An alert message is be generated. {}", alertMessage);

                metrics.count(CDAPMetricsConstants.TCA_VES_NON_COMPLIANT_MESSAGES_METRIC, 1);

                // Step 4: Emit message to Alert Sink Flowlet
                tcaAlertOutputEmitter.emit(alertMessage);

            } else {

                calculatorMessageType = TCACalculatorMessageType.COMPLIANT;
                metrics.count(CDAPMetricsConstants.TCA_VES_COMPLIANT_MESSAGES_METRIC, 1);
            }

        } else {

            metrics.count(CDAPMetricsConstants.TCA_VES_INAPPLICABLE_MESSAGES_METRIC, 1);
        }

        // save message to message status table
        TCAMessageStatusPersister.persist(
                processorContext, getContext(), calculatorMessageType, vesMessageStatusTable, alertMessage);
    }


}
