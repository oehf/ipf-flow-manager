/*
 * Copyright 2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openehealth.ipf.platform.camel.flow.extend

import org.apache.camel.builder.DefaultErrorHandlerBuilder
import org.apache.camel.builder.ErrorHandlerBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.http.common.HttpOperationFailedException
import org.apache.camel.impl.SerializationDataFormat
import org.apache.camel.impl.StringDataFormat
import org.apache.camel.processor.ErrorHandler
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy
import org.apache.camel.support.LoggingExceptionHandler

/**
 * @author Martin Krasser
 */
class GroovyFlowRouteBuilder extends RouteBuilder {
    
    void configure() {
        
        def serialization = new SerializationDataFormat()

        // --------------------------------------------------------------
        //  Recipient lists
        // --------------------------------------------------------------

//        errorHandler(new DefaultErrorHandlerBuilder()
//                .logExhaustedMessageHistory(false)
//                .logExhaustedMessageBody(false)
//                .logExhausted(false)
//                .logRetryStackTrace(false)
//                .logStackTrace(false))

        onException(HttpOperationFailedException.class)
            .handled(true)
            .nakFlow()
            .to('mock:mock')

        from('direct:flow-test-recipient-list')
            .split().body().aggregationStrategy(new UseLatestAggregationStrategy())
            .initFlow('test-recipient-list')
            .application('test')
            .inOnly().to('seda:recipient')

        from('seda:recipient')
            .recipientList().header('recipient')
            .ackFlow()
            .to('mock:wait') // avoid race conditions

        from('jetty:http://0.0.0.0:7799/recipient').to('mock:mock')

        // --------------------------------------------------------------
        //  Linear Flows
        // --------------------------------------------------------------

        from("direct:flow-test-1")
            .initFlow("test-1")
                .application("test")
                .outType(String.class)
            .setHeader("foo", constant("test-1"))
            .to("mock:mock")
            .ackFlow()

        from("direct:flow-test-2")
            .initFlow("test-2")
                .application("test")
                .inFormat(serialization)
                .outFormat(serialization)
            .setHeader("foo", constant("test-2"))
            .to("mock:mock")
            .ackFlow()

        from("direct:flow-test-3")
            .initFlow("test-3")
                .application("test")
                .inFormat(serialization)
                .outConversion(false)
            .setHeader("foo", constant("test-3"))
            .to("mock:mock")
            .ackFlow()

        from("direct:flow-test-4")
            .initFlow("test-4")
                .application("test")
                .outType(String.class)
            .setHeader("foo", constant("test-4"))
            .dedupeFlow()
            .to("mock:mock")
            .ackFlow()

        from("direct:flow-test-5")
            .initFlow("test-5")
                .application("test")
                .outType(String.class)
            .throwException(new Exception('unhandled'))
            .to("mock:mock")

        from("direct:flow-test-6")
            .initFlow("test-6")
                .replayErrorHandler("mock:error")
                .application("test")
                .outType(String.class)
            .throwException(new Exception('handled'))
            .to("mock:mock")
                     
        from("direct:flow-test-7")
            .initFlow("test-7")
                .application("test")
                .inFormat(serialization)
                .outFormat(new StringDataFormat("UTF-8"))
                .outConversion(false)
             .setHeader("foo", constant("test-4"))
             .to("mock:mock")
             .ackFlow()
        
        from("direct:flow-test-8")
            .initFlow("test-8")
                .application("test")
                .inFormat(serialization)
                .outFormat(new StringDataFormat("UTF-8"))
            .setHeader("foo", constant("test-8"))
            .to("mock:mock")
            .ackFlow()
            
        from('direct:flow-test-9')
            .transacted()
            .initFlow('test-9')
                .application('test')
            .to('mock:mock')
            .ackFlow()

        // --------------------------------------------------------------
        //  Split Flows (original Camel splitter)
        // --------------------------------------------------------------

        from("direct:flow-test-split")
            .initFlow("test-split")
                .application("test")
                .outType(String.class)
            .multicast()
            .to("direct:out-1")
            .to("direct:out-2")
    
        from("direct:out-1")
            .to("mock:mock-1")
            .ackFlow()
    
        from("direct:out-2")
            .to("mock:mock-2")
            .ackFlow()
            
        from("direct:init-flow-after-split-with-no-explicit-aggregation-strategy")
            .split(body())
                .initFlow("test-split-no-explicit-aggregation-strategy")
                    .application("test")
                    .end() //end the flow config
                 .log('Received part ${in.body}')
                 .end() //end the split
            .to("direct:ack")
        
        from("direct:ack")
            .to("mock:mock")
            .ackFlow()
        
        // --------------------------------------------------------------
        //  Pipe Flows
        // --------------------------------------------------------------
        
        from("direct:flow-test-pipe")
            .initFlow("test-pipe")
                .application("test")
                .outType(String.class)
            .to("direct:out-1")
            .to("direct:out-2")

    }
    
}