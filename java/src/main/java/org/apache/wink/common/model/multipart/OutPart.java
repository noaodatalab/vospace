/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *  
 *******************************************************************************/
package org.apache.wink.common.model.multipart;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.internal.CaseInsensitiveMultivaluedMap;
import org.apache.wink.common.internal.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to represent a part in an {@link OutMultiPart} message,
 * basically it holds the part headers and body
 * 
 * @author elib
 */
public class OutPart {
    private MultivaluedMap<String, String> headers = new CaseInsensitiveMultivaluedMap<String>();
    private Object                         body;
    private static final Logger            logger  = LoggerFactory.getLogger(OutPart.class);

    /**
     * add header to the part
     * 
     * @param name
     * @param value
     */
    public void addHeader(String name, String value) {
        getHeaders().add(name, value);
    }

    /**
     * Add the content type header
     * 
     * @param contentType
     */
    public void setContentType(String contentType) {
        getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, contentType);
    }

    public String getContentType() {
    	String res = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if(res == null)
        	res = MediaType.TEXT_PLAIN;
        return res;
        //return getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
    }

    /**
     * set the location header
     * 
     * @param location
     */
    public void setLocationHeader(String location) {
        getHeaders().putSingle("location", location); //$NON-NLS-1$
    }

    /**
     * write the part body to the os stream, the providers might be used to
     * serialize the body
     * 
     * @param os
     * @param providers
     * @throws IOException
     */
    protected void writeBody(OutputStream os, Providers providers) throws IOException {

        if (getBody() != null) {
            MessageBodyWriter writer =
                providers.getMessageBodyWriter(getBody().getClass(), null, null, MediaType
                    .valueOf(getContentType()));
            if (writer == null) {
                logger
                    .error(Messages.getMessage("couldNotFindWriter", getBody().getClass(), //$NON-NLS-1$
                           getContentType()));
                throw new WebApplicationException(500);
            }
            writer.writeTo(getBody(), getBody().getClass(), null, null, MediaType
                .valueOf(getContentType()), getHeaders(), os);
        }
    }

    /**
     * write the entire part to the os stream, the providers might be used to serialize the body
     * 
     * @param os
     * @param providers
     * @throws IOException
     */
    public void writePart(OutputStream os, Providers providers) throws IOException {
        // writeHeaders
        for (String name : getHeaders().keySet()) {
            List<String> values = getHeaders().get(name);
            for (String value : values) {
                String header = new StringBuilder().append(name).append(": ").append(value) //$NON-NLS-1$
                    .append(OutMultiPart.SEP).toString();
                os.write(header.getBytes());
            }
        }
        os.write(OutMultiPart.SEP.getBytes());
        // write Body
        writeBody(os, providers);
    }

    
    public void setBody(Object body) {
        this.body = body;
    }

    public Object getBody() {
        return body;
    }

    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

}
