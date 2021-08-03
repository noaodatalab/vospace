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

package org.apache.wink.common.internal.providers.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.wink.common.RuntimeContext;
import org.apache.wink.common.internal.WinkConfiguration;
import org.apache.wink.common.internal.i18n.Messages;
import org.apache.wink.common.internal.runtime.RuntimeContextTLS;
import org.apache.wink.common.internal.utils.MediaTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class SourceProvider implements MessageBodyWriter<Source> {

    private static TransformerFactory     transformerFactory;
    private static DocumentBuilderFactory documentBuilderFactory;

    private static final Logger logger =
        LoggerFactory
            .getLogger(SourceProvider.class);
    
    static {
        transformerFactory = TransformerFactory.newInstance();
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
    }

    @Provider
    @Consumes( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    public static class StreamSourceProvider extends SourceProvider implements
        MessageBodyReader<Source> {

        public boolean isReadable(Class<?> type,
                                  Type genericType,
                                  Annotation[] annotations,
                                  MediaType mediaType) {
            return (type.isAssignableFrom(StreamSource.class) && super.isReadable(mediaType));
        }

        public StreamSource readFrom(Class<Source> type,
                                     Type genericType,
                                     Annotation[] annotations,
                                     MediaType mediaType,
                                     MultivaluedMap<String, String> httpHeaders,
                                     InputStream entityStream) throws IOException,
            WebApplicationException {
            return new StreamSource(entityStream);
        }
    }

    @Provider
    @Consumes( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    public static class SAXSourceProvider extends SourceProvider implements
        MessageBodyReader<SAXSource> {

        public boolean isReadable(Class<?> type,
                                  Type genericType,
                                  Annotation[] annotations,
                                  MediaType mediaType) {
            return (SAXSource.class == type && super.isReadable(mediaType));
        }

        public SAXSource readFrom(Class<SAXSource> type,
                                  Type genericType,
                                  Annotation[] annotations,
                                  MediaType mediaType,
                                  MultivaluedMap<String, String> httpHeaders,
                                  InputStream entityStream) throws IOException,
            WebApplicationException {
            return new SAXSource(new InputSource(entityStream));
        }
    }

    @Provider
    @Consumes( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    public static class DOMSourceProvider extends SourceProvider implements
        MessageBodyReader<DOMSource> {
        
        public boolean isReadable(Class<?> type,
                                  Type genericType,
                                  Annotation[] annotations,
                                  MediaType mediaType) {
            return (DOMSource.class == type && super.isReadable(mediaType));
        }
        
        private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
            RuntimeContext runtimeContext = RuntimeContextTLS.getRuntimeContext();
            WinkConfiguration winkConfig = runtimeContext.getAttribute(WinkConfiguration.class);
            if (winkConfig != null) {
                Properties props = winkConfig.getProperties();
                if (props != null) {
                    // use valueOf method to require the word "true"
                    if (Boolean.valueOf(props.getProperty("wink.supportDTDEntityExpansion"))) { //$NON-NLS-1$
                        return documentBuilderFactory.newDocumentBuilder();
                    }
                }
            }
            try {
                // important: keep this order
                documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            } catch (ParserConfigurationException e) {
                // this should never happen if you run the SourceProviderTest unittests
                logger.error(e.getMessage());
            }
            try {
                // workaround for JDK5 bug that causes NPE in checking done due to above FEATURE_SECURE_PROCESSING
                // For Apache Xerces-J:  https://issues.apache.org/jira/browse/XERCESJ-977
                documentBuilderFactory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", Boolean.FALSE); //$NON-NLS-1$
            } catch (ParserConfigurationException e) {
                // possible if not on apache parser?  ignore...
            }
            DocumentBuilder dbuilder = documentBuilderFactory.newDocumentBuilder();
            /*
             * You might think you could just do this to prevent entity expansion:
             *    documentBuilderFactory.setExpandEntityReferences(false);
             * In fact, you should not do that, because it will just increase the size
             * of your DOMSource.  We want to actively reject XML when a DTD is present, so...
             */
            dbuilder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String name, String baseURI)
                throws SAXException, IOException {
                    // we don't support entity resolution here
                    throw new SAXParseException(Messages.getMessage("entityRefsNotSupported"), null);  //$NON-NLS-1$
                }
            });
            return dbuilder;
        }

        public DOMSource readFrom(Class<DOMSource> type,
                                  Type genericType,
                                  Annotation[] annotations,
                                  MediaType mediaType,
                                  MultivaluedMap<String, String> httpHeaders,
                                  InputStream entityStream) throws IOException,
            WebApplicationException {
            try {
                DocumentBuilder dbuilder = getDocumentBuilder();  //documentBuilderFactory.newDocumentBuilder();
                return new DOMSource(dbuilder.parse(entityStream));
            } catch (NullPointerException npe) {
                // For Sun JDK5, they will never fix this problem.  See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6181020
                // Let's be as safe as possible, and check that all the conditions that indicate we're avoiding DTD expansion attack
                // are present.  We'll need the catch the NPE when we do the parse, inspect the stack, and fail gracefully.  Ugly
                // hack, but it works.  (Ideally, we'd also inspect the entityStream to ensure we're definitely doing DTD expansion
                // when we get this NPE, but we cannot reliably reset the stream and re-read it due to possibly getting a stream
                // that does not support .reset().)
                StackTraceElement[] stackTraceElement = npe.getStackTrace();
                for(int i = 0; i < stackTraceElement.length; i++) {
                    if(stackTraceElement[i].getClassName().equals("com.sun.org.apache.xerces.internal.dom.DeferredDocumentImpl") //$NON-NLS-1$
                            && (stackTraceElement[i].getMethodName().equals("setChunkIndex"))) { //$NON-NLS-1$
                        // then it's really Sun JDK5, and as far as we can tell, it's related to DTD expansion attack, and we should fail gracefully
                        logger.error(Messages.getMessage("entityRefsNotSupportedSunJDK5"), npe); //$NON-NLS-1$
                        throw new WebApplicationException(Response.Status.BAD_REQUEST);
                    }
                }
                throw npe;
            } catch (SAXException e) {
                logger.error(Messages.getMessage("saxParseException", type.getName()), e); //$NON-NLS-1$
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            } catch (ParserConfigurationException e) {
                logger.error(Messages.getMessage("saxParserConfigurationException"), e); //$NON-NLS-1$
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
        }
    }

    protected boolean isReadable(MediaType mediaType) {
        return MediaTypeUtils.isXmlType(mediaType);
    }

    public long getSize(Source t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType) {
        return (Source.class.isAssignableFrom(type) && MediaTypeUtils.isXmlType(mediaType));
    }

    public void writeTo(Source t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        StreamResult sr = new StreamResult(entityStream);
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.transform(t, sr);
        } catch (TransformerException e) {
            throw asIOException(e);
        }
    }
    
    private static IOException asIOException(Exception e) throws IOException {
        IOException exception = new IOException();
        exception.initCause(e);
        return exception;
    }

}
