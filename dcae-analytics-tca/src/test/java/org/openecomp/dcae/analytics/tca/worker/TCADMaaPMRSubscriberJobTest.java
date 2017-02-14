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
import co.cask.cdap.api.worker.WorkerConfigurer;
import co.cask.cdap.api.worker.WorkerContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openecomp.dcae.analytics.common.AnalyticsConstants;
import org.openecomp.dcae.analytics.dmaap.service.subscriber.DMaaPMRSubscriber;
import org.openecomp.dcae.analytics.tca.BaseAnalyticsTCAUnitTest;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Manjesh Gowda. Creation Date: 11/18/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class TCADMaaPMRSubscriberJobTest extends BaseAnalyticsTCAUnitTest {

    @Test
    public void testExecute() throws Exception {

        DMaaPMRSubscriber mockDMaaPMRSubscriber = mock(DMaaPMRSubscriber.class);
        Metrics mockMetrics = mock(Metrics.class);

        WorkerContext workerContext = mock(WorkerContext.class);
        WorkerConfigurer workerConfigurer = mock(WorkerConfigurer.class);
        //when(workerContext.getRuntimeArguments()).thenReturn(getPreferenceMap());

        JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
        JobDataMap mockJobDataMap = mock(JobDataMap.class);
        when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(mockJobDataMap);

        /*when(mockJobDataMap.getString(AnalyticsConstants.CDAP_STREAM_VARIABLE_NAME))
                .thenReturn(CDAPComponentsConstants.TCA_FIXED_SUBSCRIBER_OUTPUT_NAME_STREAM);*/

        when(mockJobDataMap.get(AnalyticsConstants.WORKER_CONTEXT_VARIABLE_NAME))
                .thenReturn(workerContext);

        when(mockJobDataMap.get(AnalyticsConstants.DMAAP_SUBSCRIBER_VARIABLE_NAME))
                .thenReturn(mockDMaaPMRSubscriber);

        /*when(mockJobDataMap.get(AnalyticsConstants.DMAAP_SUBSCRIBER_METRICS_VARIABLE_NAME))
                .thenReturn(mockMetrics);*/

        TCADMaaPMRSubscriberJob tcaDMaaPMRSubscriberJob = new TCADMaaPMRSubscriberJob();
        tcaDMaaPMRSubscriberJob.execute(mockJobExecutionContext);

        verify(mockJobDataMap, times(1)).getString(Mockito.anyString());
        verify(mockJobDataMap, times(3)).get(any());
    }
}
