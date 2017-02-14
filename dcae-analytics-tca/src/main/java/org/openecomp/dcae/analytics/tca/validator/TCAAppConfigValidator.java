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

package org.openecomp.dcae.analytics.tca.validator;

import org.openecomp.dcae.analytics.common.cdap.validation.CDAPAppSettingsValidator;
import org.openecomp.dcae.analytics.common.validation.GenericValidationResponse;
import org.openecomp.dcae.analytics.tca.settings.TCAAppConfig;

import static org.openecomp.dcae.analytics.common.utils.ValidationUtils.isEmpty;

/**
 *  <p>
 *      TCA App Config Validator validates any TCA App Config parameter values
 *  </p>
 *
 * @author Rajiv Singla. Creation Date: 10/24/2016.
 */
public class TCAAppConfigValidator implements CDAPAppSettingsValidator<TCAAppConfig,
        GenericValidationResponse<TCAAppConfig>> {

    @Override
    public GenericValidationResponse<TCAAppConfig> validateAppSettings(TCAAppConfig tcaAppConfig) {

        final GenericValidationResponse<TCAAppConfig> validationResponse = new GenericValidationResponse<>();

        if (isEmpty(tcaAppConfig.getTcaSubscriberOutputStreamName())) {
            validationResponse.addErrorMessage("tcaSubscriberOutputStreamName",
                    "tcaSubscriberOutputStreamName must be present");
        }

        if (isEmpty(tcaAppConfig.getTcaVESMessageStatusTableName())) {
            validationResponse.addErrorMessage("tcaVESMessageStatusTableName",
                    "tcaVESMessageStatusTableName must be present");
        }
        if (isEmpty(tcaAppConfig.getTcaVESAlertsTableName())) {
            validationResponse.addErrorMessage("tcaVESAlertsTableName",
                    "tcaVESAlertsTableName must be present");
        }

        return validationResponse;
    }
}
