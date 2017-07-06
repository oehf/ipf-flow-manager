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
package org.openehealth.ipf.platform.camel.flow.model;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.RouteContext;
import org.openehealth.ipf.commons.flow.FlowManager;
import org.openehealth.ipf.platform.camel.flow.process.FlowEndProcessor;
import org.openehealth.ipf.platform.camel.flow.process.FlowProcessor;
import org.openehealth.ipf.platform.camel.flow.util.ContextUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import static org.openehealth.ipf.platform.camel.flow.util.ContextUtils.*;

/**
 * @author Martin Krasser
 */
@Metadata(label = "ipf,eip,flow")
@XmlRootElement(name = "ackFlow")
@XmlAccessorType(XmlAccessType.FIELD)
public class FlowEndProcessorDefinition extends FlowProcessorDefinition {

    @Override
    public String toString() {
        return "FlowEndProcessor[" + getOutputs() + "]";
    }

    @Override
    public String getShortName() {
        return "flowEndProcessor";
    }

    @Override
    public FlowProcessor doCreateProcessor(RouteContext routeContext) throws Exception {
        return createFlowEndProcessor(routeContext);
    }

    private static FlowEndProcessor createFlowEndProcessor(RouteContext routeContext) {
        CamelContext camelContext = routeContext.getCamelContext();
        FlowEndProcessor processor = beanOrNull(FlowEndProcessor.class, camelContext);
        
        if (processor != null) {
            return processor;
        }
        
        processor = new FlowEndProcessor();
        processor.setCamelContext(camelContext);
        processor.setFlowManager(ContextUtils.bean(FlowManager.class, camelContext));
        return processor;
    }
    
}
