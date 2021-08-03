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
package org.apache.wink.server.internal.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import javax.activation.CommandMap;
import javax.activation.DataContentHandler;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.wink.common.http.HttpStatus;
import org.apache.wink.common.internal.MultivaluedMapImpl;
import org.apache.wink.common.internal.i18n.Messages;
import org.apache.wink.common.internal.runtime.RuntimeContextTLS;
import org.apache.wink.common.utils.ProviderUtils;
import org.apache.wink.common.utils.ProviderUtils.PROVIDER_EXCEPTION_ORIGINATOR;
import org.apache.wink.server.handlers.AbstractHandler;
import org.apache.wink.server.handlers.MessageContext;
import org.apache.wink.server.internal.contexts.RequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlushResultHandler extends AbstractHandler {

    private static final Logger          logger          = LoggerFactory.getLogger(FlushResultHandler.class);
    private static final RuntimeDelegate runtimeDelegate = RuntimeDelegate.getInstance();

    @SuppressWarnings("unchecked")
    public void handleResponse(MessageContext context) throws Throwable {

        // assert status code is valid
        int statusCode = context.getResponseStatusCode();
        if (statusCode < 0) {
            logger.trace("Status code was not set. Nothing to do."); //$NON-NLS-1$
            return;
        }

        // assert response is not committed
        final HttpServletResponse httpResponse = context.getAttribute(HttpServletResponse.class);
        if (httpResponse.isCommitted()) {
            logger.trace("The response is already committed. Nothing to do."); //$NON-NLS-1$
            return;
        }

        // set the status code
        logger.trace("Response status code set to: {}", statusCode); //$NON-NLS-1$
        httpResponse.setStatus(statusCode);

        // get the entity
        Object entity = context.getResponseEntity();
        boolean isOriginalEntityResponseObj = false;

        // extract the entity and headers from the response (if it is a
        // response)
        MultivaluedMap<String, Object> httpHeaders = null;
        if (entity instanceof Response) {
            Response response = (Response)entity;
            entity = response.getEntity();
            httpHeaders = response.getMetadata();
            isOriginalEntityResponseObj = true;
        }

        // prepare the entity to write, its class and generic type
        Type genericType = null;
        Annotation[] declaredAnnotations = null;
        SearchResult searchResult = context.getAttribute(SearchResult.class);
        if (searchResult != null && searchResult.isFound()) {
            Method reflectionMethod = searchResult.getMethod().getMetadata().getReflectionMethod();
            genericType = reflectionMethod.getGenericReturnType();
            declaredAnnotations = reflectionMethod.getDeclaredAnnotations();
        } else {
            declaredAnnotations = new Annotation[0];
        }

        Class<?> rawType = null;

        if (entity instanceof GenericEntity) {
            GenericEntity<?> genericEntity = (GenericEntity<?>)entity;
            entity = genericEntity.getEntity();
            rawType = genericEntity.getRawType();
            genericType = genericEntity.getType();
        } else {
            rawType = (entity != null ? entity.getClass() : null);
            if (isOriginalEntityResponseObj) {
                genericType = rawType;
            } else {
                genericType = (genericType != null ? genericType : rawType);
            }
        }

        if (httpHeaders == null) {
            httpHeaders = new MultivaluedMapImpl<String, Object>();
        }

        // we're done if the actual entity is null
        if (entity == null) {
            logger.trace("No entity so writing only the headers"); //$NON-NLS-1$
            flushHeaders(httpResponse, httpHeaders);
            return;
        }

        // we have an entity, get the response media type
        // the actual writing will take places in the flush
        MediaType responseMediaType = context.getResponseMediaType();

        // get the provider to write the entity
        Providers providers = context.getProviders();
        MessageBodyWriter<Object> messageBodyWriter =
            (MessageBodyWriter<Object>)providers.getMessageBodyWriter(rawType,
                                                                      genericType,
                                                                      declaredAnnotations,
                                                                      responseMediaType);

        // use the provider to write the entity
        if (messageBodyWriter != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Serialization using provider {}", messageBodyWriter.getClass().getName()); //$NON-NLS-1$
            }

            final MultivaluedMap<String, Object> headers = httpHeaders;

            long size;
            try {
                size =
                    messageBodyWriter.getSize(entity,
                                              rawType,
                                              genericType,
                                              declaredAnnotations,
                                              responseMediaType);
            } catch (RuntimeException e) {
                ProviderUtils.logUserProviderException(e,
                                                       messageBodyWriter,
                                                       PROVIDER_EXCEPTION_ORIGINATOR.getSize,
                                                       new Object[] {entity, rawType, genericType,
                                                           declaredAnnotations, responseMediaType},
                                                       context);
                throw e;
            }
            if (logger.isTraceEnabled()) {
                logger.trace("{}@{}.getSize({}, {}, {}, {}, {}) returned {}", new Object[] { //$NON-NLS-1$
                             messageBodyWriter.getClass().getName(),
                                 Integer.toHexString(System.identityHashCode(messageBodyWriter)),
                                 Integer.toHexString(System.identityHashCode(entity)), rawType,
                                 genericType, declaredAnnotations, responseMediaType, size});
            }
            if (size >= 0) {
                headers.putSingle(HttpHeaders.CONTENT_LENGTH, String.valueOf(size));
            }

            FlushHeadersOutputStream outputStream =
                new FlushHeadersOutputStream(httpResponse, headers, responseMediaType);
            if (logger.isTraceEnabled()) {
                logger.trace("{}@{}.writeTo({}, {}, {}, {}, {}, {}, {}) being called", new Object[] { //$NON-NLS-1$
                           messageBodyWriter.getClass().getName(),
                               Integer.toHexString(System.identityHashCode(messageBodyWriter)),
                               Integer.toHexString(System.identityHashCode(entity)), rawType,
                               genericType, declaredAnnotations, responseMediaType, httpHeaders,
                               outputStream});
            }
            try {
                messageBodyWriter.writeTo(entity,
                                          rawType,
                                          genericType,
                                          declaredAnnotations,
                                          responseMediaType,
                                          httpHeaders,
                                          outputStream);
            } catch (RuntimeException e) {
                ProviderUtils.logUserProviderException(e,
                                              messageBodyWriter,
                                              ProviderUtils.PROVIDER_EXCEPTION_ORIGINATOR.writeTo,
                                              new Object[] {entity, rawType, genericType,
                                                  declaredAnnotations, responseMediaType,
                                                  httpHeaders, outputStream},
                                              context);
                throw e;
            }
            logger.trace("Flushing headers if not written"); //$NON-NLS-1$
            outputStream.flushHeaders();
            return;

        } else {
            logger.trace("Could not find a writer for {} and {}. Try to find JAF DataSourceProvider", //$NON-NLS-1$
                       entity.getClass().getName(),
                       responseMediaType);
        }

        DataContentHandler dataContentHandler = null;
        // Write Entity with ASF DataContentHandler

        // try to find a data handler using JavaBeans Activation Framework, if
        // found use DataSourceProvider
        dataContentHandler = CommandMap
                .getDefaultCommandMap()
                .createDataContentHandler(responseMediaType.getType() + "/" + responseMediaType.getSubtype()); //$NON-NLS-1$
        if (dataContentHandler == null) {
            if (logger.isErrorEnabled()) {
                logger.error(Messages.getMessage("noWriterOrDataSourceProvider", entity.getClass() //$NON-NLS-1$
                    .getName(), responseMediaType));
            }
            // WINK-379 - From the spec :
            // If no methods support one of the acceptable response entity body media types
            // an implementation MUST generate a WebApplicationException with a not acceptable
            // response (HTTP 406 status) and no entity
            throw new WebApplicationException(HttpStatus.NOT_ACCEPTABLE.getCode());
        }

        if (logger.isTraceEnabled()) {
            logger
                .trace("Serialization using data content handler {}", dataContentHandler.getClass() //$NON-NLS-1$
                    .getName());
        }

        FlushHeadersOutputStream outputStream = new FlushHeadersOutputStream(httpResponse, httpHeaders, responseMediaType);
        if (logger.isTraceEnabled()) {
            logger.trace("{}@{}.writeTo({}, {}, {}) being called", new Object[] { //$NON-NLS-1$
                         dataContentHandler.getClass().getName(),
                             Integer.toHexString(System.identityHashCode(dataContentHandler)),
                             entity, responseMediaType.toString(), outputStream});
        }
        dataContentHandler
            .writeTo(entity,
                     responseMediaType.getType() + "/" + responseMediaType.getSubtype(), outputStream); //$NON-NLS-1$
        logger.trace("Flushing headers if not written"); //$NON-NLS-1$
        outputStream.flushHeaders();
    }

    @SuppressWarnings("unchecked")
    private static void flushHeaders(final HttpServletResponse httpResponse,
                                     final MultivaluedMap<String, Object> headers) {
        if (headers.get(HttpHeaders.VARY) == null) {
            RequestImpl.VaryHeader varyHeader =
                RuntimeContextTLS.getRuntimeContext().getAttribute(RequestImpl.VaryHeader.class);
            if (varyHeader != null) {
                logger.trace("Vary header automatically set by a call to RequestImpl"); //$NON-NLS-1$
                headers.putSingle(HttpHeaders.VARY, varyHeader.getVaryHeaderValue());
            }
        }

        logger.trace("Flushing headers: {}", headers); //$NON-NLS-1$

        for (Entry<String, List<Object>> entry : headers.entrySet()) {
            String key = entry.getKey();
            List<Object> values = entry.getValue();
            for (Object val : values) {
                if (val != null) {
                    HeaderDelegate<Object> headerDelegate =
                        (HeaderDelegate<Object>)runtimeDelegate
                            .createHeaderDelegate(val.getClass());
                    String header =
                        headerDelegate != null ? headerDelegate.toString(val) : val.toString();
                    httpResponse.addHeader(key, header);
                }
            }
        }
    }

    private static class FlushHeadersOutputStream extends OutputStream {

        // The Proxy over OutputStream is provided here in order to write the
        // headers that a provider MAY have added to the response, before it
        // actually started writing to stream.

        private boolean                              writeStarted;
        final private HttpServletResponse            httpResponse;
        final private ServletOutputStream            outputStream;
        final private MultivaluedMap<String, Object> headers;
        final private MediaType                      responseMediaType;

        public FlushHeadersOutputStream(HttpServletResponse httpResponse,
                                        MultivaluedMap<String, Object> headers,
                                        MediaType responseMediaType) throws IOException {
            this.writeStarted = false;
            this.httpResponse = httpResponse;
            this.outputStream = httpResponse.getOutputStream();
            this.headers = headers;
            this.responseMediaType = responseMediaType;
        }

        @Override
        public void write(int b) throws IOException {
            flushHeaders();
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            flushHeaders();
            outputStream.write(b, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException {
            flushHeaders();
            outputStream.write(b);
        }

        @Override
        public void flush() throws IOException {
            flushHeaders();
            outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            flushHeaders();
            outputStream.close();
        }

        private void flushHeaders() {
            if (!writeStarted) {
                if (httpResponse.getContentType() == null) {
                    logger.trace("Set response Content-Type to: {} ", responseMediaType); //$NON-NLS-1$
                    httpResponse.setContentType(responseMediaType.toString());
                }

                FlushResultHandler.flushHeaders(httpResponse, headers);
                writeStarted = true;
            }
        }
    }

    public void init(Properties props) {
    }
}
