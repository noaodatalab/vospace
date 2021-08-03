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
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.internal.CaseInsensitiveMultivaluedMap;

/**
 * This class is used to represent a single part in an inbound MultiPart
 * messages,
 * 
 * @see InMultiPart
 */
public class InPart {
    private MultivaluedMap<String, String> headers = new CaseInsensitiveMultivaluedMap<String>();
    private InputStream                    inputStream;
    private Providers                      providers;

    /**
     * get the providers that are used by the @see InPart#getBody(Class, Type)
     * method for deserialization of the part
     * 
     * @return
     */
    public Providers getProviders() {
        return providers;
    }

    /**
     * set the providers to be used by the @see InPart#getBody(Class, Type)
     * method for deserialization of the part
     * 
     * @return
     */
    public void setProviders(Providers providers) {
        this.providers = providers;
    }

    public InPart() {

    }

    public InPart(MultivaluedMap<String, String> headers, Providers providers) {
        super();
        this.headers = headers;
        this.providers = providers;
    }

    protected void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    protected void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Get the part's headers
     * 
     * @return
     */
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }


    public String getContentType() {
        String res = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if(res == null)
        	res = MediaType.TEXT_PLAIN;
        return res;
        
    }

    public Set<String> getHeadersName() {
        return getHeaders().keySet();
    }

    /**
     * This method is used for deserialization of the part stream to a <T>
     * object using the given providers.<br>
     * 
     * @param <T>
     * @param type
     * @param genericType
     * @param providers
     * @return
     * @throws IOException
     */
    public <T> T getBody(Class<T> type, Type genericType, Providers providers) throws IOException {
        MediaType mt = MediaType.valueOf(getContentType());
        MessageBodyReader<T> reader = providers.getMessageBodyReader(type, genericType, null, mt);
        if (reader == null)
            throw new WebApplicationException(Response.Status.UNSUPPORTED_MEDIA_TYPE);
        return reader.readFrom(type, genericType, null, mt, getHeaders(), getInputStream());
    }

    /**
     * This method is used for deserialization of the part stream. It make a
     * usage of its providers data member for the deserialization <br>
     */
    public <T> T getBody(Class<T> type, Type genericType) throws IOException {
        return getBody(type, genericType, this.providers);
    }

}
