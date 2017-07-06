/*
 * Copyright 2008-2009 the original author or authors.
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
package org.openehealth.ipf.platform.camel.flow.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.camel.Message;
import org.apache.commons.io.IOUtils;
import org.openehealth.ipf.platform.camel.flow.PlatformMessage;
import org.openehealth.ipf.platform.camel.flow.PlatformMessageRenderer;

/**
 * @author Martin Krasser
 */
public class SimpleMessageRenderer implements PlatformMessageRenderer {

    @Override
    public String render(PlatformMessage message) {
        Message input = message.getExchange().getIn();
        if (input.getBody() instanceof InputStream) {
            return render((InputStream)input.getBody());
        } else {
            return input.getBody(String.class);
        }
    }
    
    private String render(InputStream stream) {
        try {
            String result = IOUtils.toString(stream, Charset.defaultCharset());
            stream.reset();
            return result;
        } catch (IOException e) {
            return null;
        }
    }
    
}
