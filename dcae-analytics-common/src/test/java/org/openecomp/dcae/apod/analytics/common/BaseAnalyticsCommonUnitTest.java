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

package org.openecomp.dcae.apod.analytics.common;

import org.openecomp.dcae.apod.analytics.common.service.processor.TestEarlyTerminatingProcessor;
import org.openecomp.dcae.apod.analytics.common.service.processor.TestMessageProcessor1;
import org.openecomp.dcae.apod.analytics.common.service.processor.TestMessageProcessor2;
import org.openecomp.dcae.apod.analytics.common.service.processor.TestProcessorContext;
import org.openecomp.dcae.apod.analytics.test.BaseDCAEAnalyticsUnitTest;

/**
 * Base class from all DCEA Analytics Common Module Unit Tests
 * <p>
 * @author Rajiv Singla. Creation Date: 10/6/2016.
 */
public abstract class BaseAnalyticsCommonUnitTest extends BaseDCAEAnalyticsUnitTest {


    protected TestProcessorContext testProcessorContext = new TestProcessorContext("", true);

    protected TestMessageProcessor1 getTestMessageProcessor1() {
         return new TestMessageProcessor1();
    }
    protected TestMessageProcessor2 getTestMessageProcessor2() {
        return new TestMessageProcessor2();
    }
    protected TestEarlyTerminatingProcessor getTestEarlyTerminationProcessor() {
        return new TestEarlyTerminatingProcessor();
    }
}
