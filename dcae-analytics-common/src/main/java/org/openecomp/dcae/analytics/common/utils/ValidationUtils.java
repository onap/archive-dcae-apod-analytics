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

package org.openecomp.dcae.analytics.common.utils;

import org.openecomp.dcae.analytics.common.cdap.settings.CDAPAppSettings;
import org.openecomp.dcae.analytics.common.cdap.validation.CDAPAppSettingsValidator;
import org.openecomp.dcae.analytics.common.exception.CDAPSettingsException;
import org.openecomp.dcae.analytics.common.validation.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contains utility methods to validate null checks, empty string etc
 *
 * @author Rajiv Singla. Creation Date: 10/24/2016.
 */
public abstract class ValidationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationUtils.class);

    /**
     * Checks if String is empty. For null string true is returned
     *
     * @param stringValue string value
     * @return returns true is string is empty or null
     */
    public static boolean isEmpty(final @Nullable String stringValue) {
        return stringValue == null || stringValue.isEmpty() || stringValue.trim().isEmpty();
    }


    /**
     * Provides common functionality to Validates CDAP App Settings. Throws Runtime exception if validation fails
     *
     * @param appSettings app Settings e.g. App Config, App Preferences etc
     * @param appSettingsValidator app Settings validator
     *
     * @param <T> Settings type e.g. AppConfig or AppPreferences
     * @param <R> Validation Response type
     * @param <V> Validator Type
     */
    public static <T extends CDAPAppSettings, R extends ValidationResponse<T>,
            V extends CDAPAppSettingsValidator<T, R>> void validateSettings(final @Nonnull T appSettings,
                                                                            final @Nonnull V appSettingsValidator) {
        checkNotNull(appSettings, "App Settings must not be null");
        checkNotNull(appSettingsValidator, "App Settings validator must not be null");

        final String appSettingsClassName = appSettings.getClass().getSimpleName();
        final String appSettingsClassValidator = appSettingsValidator.getClass().getSimpleName();

        LOG.debug("Validating App Settings for: {}, with App Settings Validator: {} ",
                appSettingsClassName, appSettingsClassValidator);

        final R validationResponse = appSettingsValidator.validateAppSettings(appSettings);

        // If setting validation fails throw an exception
        if (validationResponse.hasErrors()) {
            throw new CDAPSettingsException(
                    validationResponse.getAllErrorMessage(), LOG, new IllegalArgumentException());
        }

        LOG.debug("App Settings Validation Successful for app Settings: {} with validator: {}", appSettingsClassName,
                appSettingsClassValidator);
    }

}
