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
package org.openehealth.ipf.commons.flow.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;
import org.openehealth.ipf.commons.flow.transfer.FlowPartInfo;
import org.openehealth.ipf.commons.flow.util.Flows;

/**
 * @author Mitko Kolev
 * @author Martin Krasser 
 */
public class FlowPartTest {
    
    private static final String PACKET = "packet";
    
	@Test
	public void testFlowPartGetLatestUpdate() throws Exception {
		Flow flow = Flows.createFlow(PACKET, 1);
		FlowPart part = flow.getPart("0.0");
        part.setContributionTime(null);
        part.setFilterTime(null);
        assertNull(part.getLatestUpdate());
        part.setContributionTime(dateHigh());
        part.setFilterTime(null);
		assertEquals(dateHigh(), part.getLatestUpdate());
        part.setContributionTime(null);
        part.setFilterTime(dateHigh());
        assertEquals(dateHigh(), part.getLatestUpdate());
        part.setContributionTime(dateHigh());
        part.setFilterTime(dateLow());
        assertEquals(dateHigh(), part.getLatestUpdate());
        part.setContributionTime(dateLow());
        part.setFilterTime(dateHigh());
        assertEquals(dateHigh(), part.getLatestUpdate());
	}
	
	@Test
	public void testFlowPartInfoCreation() throws Exception {
		Flow flow = Flows.createFlow(PACKET, 1);
		FlowPart part = flow.getPart("0.0");
        FlowPartInfo info = part.getInfo(flow);
        assertEquals("CLEAN", info.getStatus());
        assertEquals(0, info.getContributionCount());
        flow.acknowledge("0.0", false);
        info = part.getInfo(flow);
        assertEquals(1, info.getContributionCount());
	}
	
    private static Date dateHigh() {
        return new Date(2000L);
    }

    private static Date dateLow() {
        return new Date(1000L);
    }

}
