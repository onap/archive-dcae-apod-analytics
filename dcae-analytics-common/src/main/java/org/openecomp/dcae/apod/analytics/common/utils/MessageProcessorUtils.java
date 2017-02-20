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

package org.openecomp.dcae.apod.analytics.common.utils;

import org.openecomp.dcae.apod.analytics.common.exception.MessageProcessingException;
import org.openecomp.dcae.apod.analytics.common.service.processor.MessageProcessor;
import org.openecomp.dcae.apod.analytics.common.service.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;


/**
 * Contains utility methods for {@link MessageProcessor}
 *
 * @author Rajiv Singla. Creation Date: 11/8/2016.
 */
public abstract class MessageProcessorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorUtils.class);

    /**
     * Provides an abstraction how to apply {@link ProcessorContext} to next {@link MessageProcessor}
     * in the message processor chain
     *
     * @param <P> Sub classes of Processor Context
     */
    public interface MessageProcessorFunction<P extends ProcessorContext> {

        /**
         * Method which provides accumulated {@link ProcessorContext} from previous processors and a reference
         * to next processor in the chain
         *
         * @param p accumulated {@link ProcessorContext} from previous processors
         * @param m current {@link MessageProcessor} in the chain
         * @param <M> Message processor sub classes
         *
         * @return processing context after computing the current Message Processor
         */
        <M extends MessageProcessor<P>> P apply(P p, M m);
    }


    /**
     * Provides an abstraction to compute a chain of {@link MessageProcessor}
     *
     * @param messageProcessors An iterable containing one or more {@link MessageProcessor}s
     * @param initialProcessorContext An initial processing Context
     * @param messageProcessorFunction messageProcessor Function
     * @param <P> Sub classes for Processor Context
     *
     * @return processing context which results after computing the whole chain
     */
    public static <P extends ProcessorContext> P computeMessageProcessorChain(
            final Iterable<? extends MessageProcessor<P>> messageProcessors,
            final P initialProcessorContext,
            final MessageProcessorFunction<P> messageProcessorFunction) {

        // Get message processor iterator
        final Iterator<? extends MessageProcessor<P>> processorIterator = messageProcessors.iterator();

        // If no next message processor - return initial processor context
        if (!processorIterator.hasNext()) {
            return initialProcessorContext;
        }

        // An accumulator for processor Context
        P processorContextAccumulator = initialProcessorContext;

        while (processorIterator.hasNext()) {

            final MessageProcessor<P> nextProcessor = processorIterator.next();

            // If Initial Processor Context is null
            if (processorContextAccumulator == null) {
                final String errorMessage =
                        String.format("Processor Context must not be null for Message Process: %s",
                                nextProcessor.getProcessorInfo().getProcessorName());
                throw new MessageProcessingException(errorMessage, LOG, new IllegalStateException(errorMessage));
            }


            if (!processorContextAccumulator.canProcessingContinue()) {
                LOG.debug("Triggering Early Termination, before Message Processor: {}, Incoming Message: {}",
                        nextProcessor.getProcessorInfo().getProcessorName(), processorContextAccumulator.getMessage());
                break;
            }
            processorContextAccumulator = messageProcessorFunction.apply(processorContextAccumulator, nextProcessor);
        }

        return processorContextAccumulator;
    }


}
