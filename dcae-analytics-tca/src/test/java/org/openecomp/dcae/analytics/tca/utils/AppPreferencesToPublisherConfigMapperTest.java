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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openecomp.dcae.analytics.dmaap.domain.config.DMaaPMRPublisherConfig;
import org.openecomp.dcae.analytics.tca.BaseAnalyticsTCAUnitTest;
import org.openecomp.dcae.analytics.tca.settings.TCATestAppPreferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Manjesh Gowda. Creation Date: 11/21/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class AppPreferencesToPublisherConfigMapperTest extends BaseAnalyticsTCAUnitTest {
    @Test
    public void testMapTCAConfigToSubscriberConfigFunctionGood() {
        DMaaPMRPublisherConfig dMaaPMRPublisherConfig =
                (new AppPreferencesToPublisherConfigMapper()).apply(getTCATestAppPreferences());
        assertEquals(dMaaPMRPublisherConfig.getHostName(), "PUBLISHER_HOST_NAME");
    }

    @Test
    public void testMapTCAConfigToSubscriberConfigFunctionMap() {
        DMaaPMRPublisherConfig dMaaPMRPublisherConfig = AppPreferencesToPublisherConfigMapper.map(
                getTCATestAppPreferences());
        assertEquals(dMaaPMRPublisherConfig.getHostName(), "PUBLISHER_HOST_NAME");
    }

    @Test
    public void testMapTCAConfigToSubscriberConfigFunctionAllNull() {
        DMaaPMRPublisherConfig dMaaPMRPublisherConfig =
                (new AppPreferencesToPublisherConfigMapper()).apply(new TCATestAppPreferences());
        assertNull(dMaaPMRPublisherConfig.getHostName());
    }
}
