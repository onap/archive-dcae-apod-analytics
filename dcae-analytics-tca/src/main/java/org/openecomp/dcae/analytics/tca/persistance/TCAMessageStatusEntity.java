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

package org.openecomp.dcae.analytics.tca.persistance;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * TCA Message Status is an Entity which is used to persist TCA VES Message status information in Message Status Table
 *
 * @author Rajiv Singla. Creation Date: 11/16/2016.
 */
public class TCAMessageStatusEntity implements Writable, Serializable {

    private static final long serialVersionUID = -6186607668704516962L;

    private long creationTS;
    private int flowletInstance;
    private String messageType;
    private String vesMessage;
    private String domain;
    private String functionalRole;
    private String thresholdPath;
    private String thresholdSeverity;
    private String thresholdDirection;
    private Long thresholdValue;
    private String jsonProcessorStatus;
    private String jsonProcessorMessage;
    private String domainFilterStatus;
    private String domainFilterMessage;
    private String functionalRoleFilterStatus;
    private String functionalRoleFilterMessage;
    private String thresholdCalculatorStatus;
    private String thresholdCalculatorMessage;
    private String alertMessage;

    public TCAMessageStatusEntity() {
    }

    public TCAMessageStatusEntity(long creationTS, int flowletInstance, String messageType, String vesMessage, String
            domain, String functionalRole) {
        this(creationTS, flowletInstance, messageType, vesMessage, domain, functionalRole, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    public TCAMessageStatusEntity(long creationTS, int flowletInstance, String messageType, String vesMessage,
                                  String domain, String functionalRole,
                                  String thresholdPath, String thresholdSeverity, String thresholdDirection,
                                  Long thresholdValue,
                                  String jsonProcessorStatus, String jsonProcessorMessage,
                                  String domainFilterStatus, String domainFilterMessage,
                                  String functionalRoleFilterStatus, String functionalRoleFilterMessage,
                                  String thresholdCalculatorStatus, String thresholdCalculatorMessage,
                                  String alertMessage) {
        this.creationTS = creationTS;
        this.flowletInstance = flowletInstance;
        this.messageType = messageType;
        this.vesMessage = vesMessage;
        this.domain = domain;
        this.functionalRole = functionalRole;
        this.thresholdPath = thresholdPath;
        this.thresholdSeverity = thresholdSeverity;
        this.thresholdDirection = thresholdDirection;
        this.thresholdValue = thresholdValue;
        this.jsonProcessorStatus = jsonProcessorStatus;
        this.jsonProcessorMessage = jsonProcessorMessage;
        this.domainFilterStatus = domainFilterStatus;
        this.domainFilterMessage = domainFilterMessage;
        this.functionalRoleFilterStatus = functionalRoleFilterStatus;
        this.functionalRoleFilterMessage = functionalRoleFilterMessage;
        this.thresholdCalculatorStatus = thresholdCalculatorStatus;
        this.thresholdCalculatorMessage = thresholdCalculatorMessage;
        this.alertMessage = alertMessage;
    }

    public long getCreationTS() {
        return creationTS;
    }

    public void setCreationTS(long creationTS) {
        this.creationTS = creationTS;
    }

    public int getFlowletInstance() {
        return flowletInstance;
    }

    public void setFlowletInstance(int flowletInstance) {
        this.flowletInstance = flowletInstance;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getVesMessage() {
        return vesMessage;
    }

    public void setVesMessage(String vesMessage) {
        this.vesMessage = vesMessage;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getFunctionalRole() {
        return functionalRole;
    }

    public void setFunctionalRole(String functionalRole) {
        this.functionalRole = functionalRole;
    }

    public String getThresholdPath() {
        return thresholdPath;
    }

    public void setThresholdPath(String thresholdPath) {
        this.thresholdPath = thresholdPath;
    }

    public String getThresholdSeverity() {
        return thresholdSeverity;
    }

    public void setThresholdSeverity(String thresholdSeverity) {
        this.thresholdSeverity = thresholdSeverity;
    }

    public String getThresholdDirection() {
        return thresholdDirection;
    }

    public void setThresholdDirection(String thresholdDirection) {
        this.thresholdDirection = thresholdDirection;
    }

    public Long getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Long thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getJsonProcessorStatus() {
        return jsonProcessorStatus;
    }

    public void setJsonProcessorStatus(String jsonProcessorStatus) {
        this.jsonProcessorStatus = jsonProcessorStatus;
    }

    public String getJsonProcessorMessage() {
        return jsonProcessorMessage;
    }

    public void setJsonProcessorMessage(String jsonProcessorMessage) {
        this.jsonProcessorMessage = jsonProcessorMessage;
    }

    public String getDomainFilterStatus() {
        return domainFilterStatus;
    }

    public void setDomainFilterStatus(String domainFilterStatus) {
        this.domainFilterStatus = domainFilterStatus;
    }

    public String getDomainFilterMessage() {
        return domainFilterMessage;
    }

    public void setDomainFilterMessage(String domainFilterMessage) {
        this.domainFilterMessage = domainFilterMessage;
    }

    public String getFunctionalRoleFilterStatus() {
        return functionalRoleFilterStatus;
    }

    public void setFunctionalRoleFilterStatus(String functionalRoleFilterStatus) {
        this.functionalRoleFilterStatus = functionalRoleFilterStatus;
    }

    public String getFunctionalRoleFilterMessage() {
        return functionalRoleFilterMessage;
    }

    public void setFunctionalRoleFilterMessage(String functionalRoleFilterMessage) {
        this.functionalRoleFilterMessage = functionalRoleFilterMessage;
    }

    public String getThresholdCalculatorStatus() {
        return thresholdCalculatorStatus;
    }

    public void setThresholdCalculatorStatus(String thresholdCalculatorStatus) {
        this.thresholdCalculatorStatus = thresholdCalculatorStatus;
    }

    public String getThresholdCalculatorMessage() {
        return thresholdCalculatorMessage;
    }

    public void setThresholdCalculatorMessage(String thresholdCalculatorMessage) {
        this.thresholdCalculatorMessage = thresholdCalculatorMessage;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        WritableUtils.writeVLong(dataOutput, creationTS);
        WritableUtils.writeVInt(dataOutput, flowletInstance);
        WritableUtils.writeString(dataOutput, messageType);
        WritableUtils.writeString(dataOutput, vesMessage);

        WritableUtils.writeString(dataOutput, domain);
        WritableUtils.writeString(dataOutput, functionalRole);

        WritableUtils.writeString(dataOutput, thresholdPath);
        WritableUtils.writeString(dataOutput, thresholdSeverity);
        WritableUtils.writeString(dataOutput, thresholdDirection);
        WritableUtils.writeVLong(dataOutput, thresholdValue);

        WritableUtils.writeString(dataOutput, jsonProcessorStatus);
        WritableUtils.writeString(dataOutput, jsonProcessorMessage);
        WritableUtils.writeString(dataOutput, domainFilterStatus);
        WritableUtils.writeString(dataOutput, domainFilterMessage);
        WritableUtils.writeString(dataOutput, functionalRoleFilterStatus);
        WritableUtils.writeString(dataOutput, functionalRoleFilterMessage);
        WritableUtils.writeString(dataOutput, thresholdCalculatorStatus);
        WritableUtils.writeString(dataOutput, thresholdCalculatorMessage);

        WritableUtils.writeString(dataOutput, alertMessage);

    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        creationTS = WritableUtils.readVLong(dataInput);
        flowletInstance = WritableUtils.readVInt(dataInput);
        messageType = WritableUtils.readString(dataInput);
        vesMessage = WritableUtils.readString(dataInput);

        domain = WritableUtils.readString(dataInput);
        functionalRole = WritableUtils.readString(dataInput);

        thresholdPath = WritableUtils.readString(dataInput);
        thresholdSeverity = WritableUtils.readString(dataInput);
        thresholdDirection = WritableUtils.readString(dataInput);
        thresholdValue = WritableUtils.readVLong(dataInput);

        jsonProcessorStatus = WritableUtils.readString(dataInput);
        jsonProcessorMessage = WritableUtils.readString(dataInput);
        domainFilterStatus = WritableUtils.readString(dataInput);
        domainFilterMessage = WritableUtils.readString(dataInput);
        functionalRoleFilterStatus = WritableUtils.readString(dataInput);
        functionalRoleFilterMessage = WritableUtils.readString(dataInput);
        thresholdCalculatorStatus = WritableUtils.readString(dataInput);
        thresholdCalculatorMessage = WritableUtils.readString(dataInput);

        alertMessage = WritableUtils.readString(dataInput);

    }
}
