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
package org.openehealth.ipf.platform.camel.flow.process;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.openehealth.ipf.commons.flow.FlowManager;
import org.openehealth.ipf.commons.flow.ManagedMessage;
import org.openehealth.ipf.commons.flow.transfer.FlowInfo;
import org.openehealth.ipf.platform.camel.flow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A processor that triggers a
 * {@link FlowManager#beginFlow(ManagedMessage, String, int)} operation.
 * 
 * @author Martin Krasser
 */
public class FlowBeginProcessor extends FlowProcessor implements ReplayStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBeginProcessor.class);

    @Autowired
    private ReplayStrategyRegistry registry;
    
    private ReplayStrategyRegistration registration;
    
    private Processor replayErrorProcessor;
    
    private String identifier;
    
    private String application;
    
    private int expectedAckCount = FlowInfo.ACK_COUNT_EXPECTED_UNDEFINED;

    /**
     * Creates a new {@link FlowBeginProcessor}.
     */
    public FlowBeginProcessor() {
        this(null);
    }
    
    /**
     * Creates a new {@link FlowBeginProcessor} with unique
     * <code>identifier</code>.
     * 
     * @param identifier
     *            unique identifier.
     */
    public FlowBeginProcessor(String identifier) {
        this.identifier = identifier;
        replayErrorProcessor = exchange -> {};
    }
    
    public void setRegistry(ReplayStrategyRegistry registry) {
        this.registry = registry;
    }
    
    @Override
    public String toString() {
        return "FlowBeginProcessor[" + identifier + " -> " + getProcessor() + "]";
    }

    /**
     * Returns this processors's unique identifier.
     * 
     * @return this processor's unique identifier.
     */
    @Override
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets this processors's unique identifier.
     * 
     * @param identifier
     *            this processor's unique identifier.
     */
    public FlowBeginProcessor identifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Set the application name for the flow.
     * 
     * @param application
     *            application name.
     * @return this processor.
     */
    public FlowBeginProcessor application(String application) {
        this.application = application;
        return this;
    }
    
    /**
     * Sets URI of the endpoint that processes replay errors.
     * 
     * @param replayErrorUri
     *            URI of error endpoint.
     * @return this processor.
     */
    public FlowBeginProcessor replayErrorHandler(String replayErrorUri) throws Exception {
        if (replayErrorUri != null) {
            replayErrorProcessor = resolveReplayErrorEndpoint(replayErrorUri).createProducer();
        }
        return this;
    }
    
    /**
     * Set the expected acknowledgement count for the flow.
     * 
     * @param expectedAckCount
     *            expected acknowledgement count.
     * @return this processor.
     */
    public FlowBeginProcessor expectedAckCount(int expectedAckCount) {
        this.expectedAckCount = expectedAckCount;
        return this;
    }
    
    /* (non-Javadoc)
     * @see org.openehealth.ipf.platform.camel.flow.ReplayStrategy#register()
     */
    @Override
    public void register() {
        if (registry != null) {
            registration = registry.register(this);
        } else {
            LOG.warn("no replay strategy registry defined in application context");
        }
    }

    public void unregister() {
        if (registration != null) {
            registration.terminate();
        }
    }

    /**
     * Delegates to {@link FlowProcessor#createPacket(Exchange)} and
     * additionally sets the {@link ReplayStrategy#getIdentifier()}.
     * 
     * @param exchange
     *            message exchange.
     * @return packet additionally containing replay strategy id.
     */
    @Override
    public PlatformPacket createPacket(Exchange exchange) {
        PlatformPacket packet = super.createPacket(exchange);
        packet.setReplayStrategyId(identifier);
        return packet;
    }

    /**
     * Replays the exchange represented by <code>packet</code>. If the
     * replayed exchange has failed it is forwarded to the error endpoint
     * configured via {@link #replayErrorProcessor}.
     * 
     * @throws Exception
     *             if replay fails.
     * @return the packet passed as argument.
     */
    @Override
    public PlatformPacket replay(PlatformPacket packet) throws Exception {
        Exchange exchange = createExchange(packet);
        try {
			getProcessor().process(exchange);
		} catch (Exception e) {
            // ok
		}
        if (exchange.isFailed()) {
            setFailureHandled(exchange);
            replayErrorProcessor.process(exchange);
        }
        return packet;
    }

    /**
     * Delegates to {@link FlowManager#beginFlow(ManagedMessage, String, int)}
     * 
     * @param message
     *            managed message.
     */
    @Override
    protected void processMessage(PlatformMessage message) {
        try {
            flowManager.beginFlow(message, application, expectedAckCount);
        } catch (Exception e) {
            // apply conversions defined in route 
            message.createPacket();
            // keep processing exchange (only log error)
            LOG.error(e.getMessage(), e);
        }
        
    }

    @Override
    protected void doStop() throws Exception {
        unregister();
        super.doStop();
    }

    private Endpoint resolveReplayErrorEndpoint(String replayErrorUri) {
        Endpoint endpoint = getCamelContext().getEndpoint(replayErrorUri);
        if (endpoint == null) {
            throw new IllegalArgumentException("Unknown endpoint with URI " + replayErrorUri);
        }
        return endpoint;
    }

    private static void setFailureHandled(Exchange exchange) {
    	exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exchange.getException());
    	exchange.setProperty(Exchange.ERRORHANDLER_HANDLED, Boolean.TRUE);
    	exchange.setException(null);
    }
    
}
