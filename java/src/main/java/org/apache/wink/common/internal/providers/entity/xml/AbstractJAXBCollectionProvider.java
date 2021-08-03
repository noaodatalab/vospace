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

package org.apache.wink.common.internal.providers.entity.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.wink.common.internal.i18n.Messages;
import org.apache.wink.common.internal.utils.GenericsUtils;
import org.apache.wink.common.internal.utils.MediaTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJAXBCollectionProvider extends AbstractJAXBProvider {
    private static final String JAXB_DEFAULT_NAMESPACE = "##default"; //$NON-NLS-1$
    private static final String JAXB_DEFAULT_NAME      = "##default"; //$NON-NLS-1$
    private static final Logger logger                 =
                                                           LoggerFactory
                                                               .getLogger(AbstractJAXBCollectionProvider.class);

    public Object read(Class<?> type,
                       Type genericType,
                       Annotation[] annotations,
                       MediaType mediaType,
                       MultivaluedMap<String, String> httpHeaders,
                       InputStream entityStream) throws IOException, WebApplicationException {
        XMLStreamReader xsr = null;
        try {
            xsr = getXMLStreamReader(entityStream);
            Class<?> theType = getParameterizedTypeClassForRead(type, genericType, true);
            theType = getConcreteTypeFromTypeMap(theType, annotations);
            JAXBContext context = getContext(theType, mediaType);
            Unmarshaller unmarshaller = getJAXBUnmarshaller(type, context, mediaType);

            int nextEvent = xsr.getEventType();
            while (nextEvent != XMLStreamReader.START_ELEMENT)
                nextEvent = xsr.next();

            List<Object> elementList = new ArrayList<Object>();
            // skip the plural tag
            nextEvent = xsr.next();
            while (nextEvent != XMLStreamReader.END_DOCUMENT) {
                switch (nextEvent) {
                    case XMLStreamReader.START_ELEMENT:
                        Class<?> parameterizedTypeClass = getParameterizedTypeClassForRead(type, genericType, false);
                        if (parameterizedTypeClass == JAXBElement.class) {
                            elementList.add(unmarshaller.unmarshal(xsr, theType));
                        } else if (theType.isAnnotationPresent(XmlRootElement.class)) {
                            Object o = unmarshaller.unmarshal(xsr);
                            o = unmarshalWithXmlAdapter(o, type, getParameterizedTypeClassForRead(type, genericType, false), annotations);
                            if (o instanceof JAXBElement) {
                                o = ((JAXBElement)o).getValue();
                            }
                            elementList.add(o);
                        } else {
                            elementList.add(unmarshaller.unmarshal(xsr, theType).getValue());
                        }
                        nextEvent = xsr.getEventType();
                        break;
                    default:
                        nextEvent = xsr.next();
                }
            }
            closeXMLStreamReader(xsr);

            Object ret = null;
            if (type.isArray())
                ret = convertListToArray(theType, elementList);
            else if (type == Set.class)
                ret = new HashSet<Object>(elementList);
            else
                ret = elementList;

            releaseJAXBUnmarshaller(context, unmarshaller);
            return ret;
        } catch (XMLStreamException e) {
            closeXMLStreamReader(xsr);
            logger.error(Messages.getMessage("jaxbFailToUnmarshal", type.getName()), e); // TODO //$NON-NLS-1$
            // change
            // message
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        } catch (JAXBException e) {
            closeXMLStreamReader(xsr);
            logger.error(Messages.getMessage("jaxbFailToUnmarshal", type.getName()), e); //$NON-NLS-1$
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        } catch (RuntimeException e) {
            closeXMLStreamReader(xsr);
            throw e;
        }
    }

    public void write(Object t,
                      Class<?> type,
                      Type genericType,
                      Annotation[] annotations,
                      MediaType mediaType,
                      MultivaluedMap<String, Object> httpHeaders,
                      OutputStream entityStream) throws IOException, WebApplicationException {

        mediaType = MediaTypeUtils.setDefaultCharsetOnMediaTypeHeader(httpHeaders, mediaType);

        try {
            Class<?> adapterClass = getParameterizedTypeClassForWrite(type, genericType, true);
            Class<?> theType = getConcreteTypeFromTypeMap(adapterClass, annotations);
            Object[] elementArray = type.isArray() ? (Object[])t : ((Collection<?>)t).toArray();
            QName qname = null;
            boolean isJAXBElement = false;
            if (elementArray.length > 0 && elementArray[0] instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)elementArray[0];
                qname = jaxbElement.getName();
                isJAXBElement = true;
            } else {
                qname = getJaxbQName(theType);
            }

            if (qname != null) {
                writeStartTag(qname, entityStream, mediaType);
            }

            Marshaller marshaller = null;
            JAXBContext context = null;
            for (Object o : elementArray) {
                o = marshalWithXmlAdapter(o, type, getParameterizedTypeClassForWrite(type, genericType, false), annotations);
                if(marshaller == null) {
                    Class<?> oType =
                        isJAXBElement ? ((JAXBElement<?>)o).getDeclaredType() : o.getClass();
                        context = getContext(oType, mediaType);
                        marshaller = getJAXBMarshaller(oType, context, mediaType);
                        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
                        Charset charSet = getCharSet(mediaType);
                        marshaller.setProperty(Marshaller.JAXB_ENCODING, charSet.name());
                }
                Object entityToMarshal = getEntityToMarshal(o, getParameterizedTypeClassForWrite(type, genericType, false));
                if (qname == null) {
                    if (entityToMarshal instanceof JAXBElement<?>)
                        qname = ((JAXBElement<?>)entityToMarshal).getName();
                    else
                        qname =
                            new QName(entityToMarshal.getClass().getPackage().getName(),
                                      entityToMarshal.getClass().getSimpleName());
                    writeStartTag(qname, entityStream, mediaType);
                }
                marshaller.marshal(entityToMarshal, entityStream);
                releaseJAXBMarshaller(context, marshaller);
                marshaller = null;
            }

            writeEndTag(qname, entityStream);
        } catch (JAXBException e) {
            logger.error(Messages.getMessage("jaxbFailToMarshal", type.getName()), e); //$NON-NLS-1$
            throw new WebApplicationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> Object convertListToArray(Class<T> type, List<Object> elementList) {
        T[] ret = (T[])Array.newInstance(type, elementList.size());
        for (int i = 0; i < elementList.size(); ++i)
            ret[i] = (T)elementList.get(i);
        return ret;
    }

    protected static void writeStartTag(QName qname, OutputStream entityStream, MediaType m)
        throws IOException {
        String startTag = null;
        Charset charSet = getCharSet(m);
        startTag = "<?xml version=\"1.0\" encoding=\"" + charSet.name() + "\" standalone=\"yes\"?>"; //$NON-NLS-1$ //$NON-NLS-2$
        entityStream.write(startTag.getBytes());
        // if (qname.getNamespaceURI().length() > 0)
        // startTag = "<" + qname.getLocalPart() + "s xmlns=\"" +
        // qname.getNamespaceURI() + "\">";
        // else
        startTag = "<" + qname.getLocalPart() + "s>"; //$NON-NLS-1$ //$NON-NLS-2$
        entityStream.write(startTag.getBytes());
    }

    protected static Charset getCharSet(MediaType m) {
        String charSetString = m.getParameters().get("charset"); //$NON-NLS-1$
        Charset charSet =
            charSetString == null ? Charset.forName("UTF-8") : Charset.forName(charSetString); //$NON-NLS-1$
        return charSet;
    }

    protected static void writeEndTag(QName qname, OutputStream entityStream) throws IOException {
        String endTag = null;
        if (qname.getNamespaceURI().length() > 0)
            endTag = "</" + qname.getLocalPart() + "s>"; //$NON-NLS-1$ //$NON-NLS-2$
        else
            endTag = "</" + qname.getLocalPart() + "s>"; //$NON-NLS-1$ //$NON-NLS-2$
        entityStream.write(endTag.getBytes());
    }

    public static Class<?> getParameterizedTypeClassForRead(Class<?> type,
                                                        Type genericType,
                                                        boolean recurse) {
        if (Collection.class.isAssignableFrom(type)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)genericType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (!(actualTypeArguments[0] instanceof ParameterizedType)) {
                    return (Class<?>)actualTypeArguments[0];
                } else {
                    parameterizedType = (ParameterizedType)actualTypeArguments[0];
                    if (recurse)
                        return getParameterizedTypeClassForRead(type, parameterizedType, recurse);
                    else
                        return (Class<?>)parameterizedType.getRawType();
                }
            } else {
                return GenericsUtils.getGenericParamType(genericType);
            }
        } else if (type.isArray()) {
            return type.getComponentType();
        }
        return null;
    }
    
    public static Class<?> getParameterizedTypeClassForWrite(Class<?> type,
            Type genericType,
            boolean recurse) {
        if (Collection.class.isAssignableFrom(type)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)genericType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (!(actualTypeArguments[0] instanceof ParameterizedType)) {
                    return (Class<?>)actualTypeArguments[0];
                } else {
                    parameterizedType = (ParameterizedType)actualTypeArguments[0];
                    if (recurse)
                        return getParameterizedTypeClassForRead(type, parameterizedType, recurse);
                    else
                        return (Class<?>)parameterizedType.getRawType();
                }
            } else {
                return GenericsUtils.getGenericParamType(genericType);
            }
        } else if (genericType != null) {
            Class genericTypeClass = null;
            try {
                genericTypeClass = (Class<?>)genericType;
            } catch (ClassCastException cce) {
                genericTypeClass = genericType.getClass();
            }
            if (genericTypeClass.isArray()) {
                return genericTypeClass.getComponentType();
            }
        }
        if (type.isArray()) {
            return type.getComponentType();
        }
        return null;
    }

    protected static QName getJaxbQName(Class<?> cls) {
        XmlRootElement root = cls.getAnnotation(XmlRootElement.class);
        if (root != null) {
            String namespace = getNamespace(root.namespace());
            String name = getLocalName(root.name(), cls.getSimpleName());
            return new QName(namespace, name);
        }
        return null;
    }

    protected static String getLocalName(String name, String clsName) {
        if (JAXB_DEFAULT_NAME.equals(name)) {
            name = clsName;
            if (name.length() > 1) {
                name = name.substring(0, 1).toLowerCase() + name.substring(1);
            } else {
                name = name.toLowerCase();
            }
        }
        return name;
    }

    protected static String getNamespace(String namespace) {
        if (JAXB_DEFAULT_NAMESPACE.equals(namespace)) {
            return ""; //$NON-NLS-1$
        }
        return namespace;
    }
}
