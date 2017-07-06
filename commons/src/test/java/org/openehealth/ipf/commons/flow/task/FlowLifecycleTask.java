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
package org.openehealth.ipf.commons.flow.task;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openehealth.ipf.commons.flow.domain.Flow;
import org.openehealth.ipf.commons.flow.repository.FlowRepository;
import org.openehealth.ipf.commons.flow.tx.TestTransactionManager;

/**
 * @author Martin Krasser
 */
public class FlowLifecycleTask extends FlowTaskSupport {
    
    private static final Logger LOG = LoggerFactory.getLogger(FlowLifecycleTask.class);
    
    private FlowRepository flowRepository;

    private TestTransactionManager transactionManager;
    
    public FlowRepository getFlowRepository() {
        return flowRepository;
    }

    public void setFlowRepository(FlowRepository flowRepository) {
        this.flowRepository = flowRepository;
    }

    public TestTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TestTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void doRun() {
        // start a new flow
        long id = getFlowManager().beginFlow(getMessage(), "test");
        
        // on first pass an exchange must not be filtered
        if(getFlowManager().filterFlow(getMessage())) {
            throw new FlowTaskException("flow filtered");
        }
        
        // flow is acknowledged (normal outcome) 
        getFlowManager().acknowledgeFlow(getMessage());

        // simulate replay (no JBI environment here)
        transactionManager.beginTransaction();
        Flow flow = getFlowRepository().find(id);
        flow.incrementReplayCount();
        flow.setReplayTime(new Date());
        transactionManager.endTransaction();
        
        // start a flow (should recognize replay)
        if (!getFlowManager().beginFlow(getMessage(), "test").equals(id)) {
            throw new FlowTaskException("unexpected identifier");
        }
        // on second pass an exchange must be filtered
        if(!getFlowManager().filterFlow(getMessage())) {
            throw new FlowTaskException("flow not filtered");
        }
        
        // final acknowledgement
        getFlowManager().acknowledgeFlow(getMessage());
        
        // clear flow id from exchange
        getMessage().setFlowId(null);
        
        LOG.info("generated id = {} ({})", id, Thread.currentThread().getName());
    }

}
