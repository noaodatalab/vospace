/*
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
 */

package org.apache.wink.common.model.wadl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import org.apache.wink.common.DynamicResource;
import org.apache.wink.common.internal.registry.Injectable;
import org.apache.wink.common.internal.registry.Injectable.ParamType;
import org.apache.wink.common.internal.registry.metadata.ClassMetadata;
import org.apache.wink.common.internal.registry.metadata.MethodMetadata;
import org.apache.wink.common.internal.registry.metadata.ResourceMetadataCollector;
import org.apache.wink.common.internal.uritemplate.JaxRsUriTemplateProcessor;
import org.apache.wink.common.internal.uritemplate.UriTemplateProcessor;

/**
 * @Path("/sfdsf"); public class MyResource {
 * @Context javax.ws.rs.core.Application app;
 * @OPTIONS public org.apache.wink.common.model.wadl.Application getWADL() {
 *          return new WADLGenerator().generate(app.getClasses()); } }
 */
public class WADLGenerator {

    private static final String XML_SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";

    public Application generate(String baseURI, Set<Class<?>> classes) {
        /*
         * the idea is that classes comes from the Application subclass
         */
        Application app = new Application();
        if (classes == null || classes.isEmpty()) {
            return app;
        }

        Resources resources = new Resources();
        resources.setBase(baseURI);

        Set<ClassMetadata> metadataSet = buildClassMetdata(classes);
        resources.getResource().addAll(buildResources(metadataSet));
        app.getResources().add(resources);
        return app;
    }

    /* package */Set<ClassMetadata> buildClassMetdata(Set<Class<?>> classes) {
        Set<ClassMetadata> metadataSet = new HashSet<ClassMetadata>(classes.size());
        for (Class<?> c : classes) {
            if (!isResource(c)) {
                /* not a resource, so skip it */
                continue;
            }

            ClassMetadata metadata = ResourceMetadataCollector.collectMetadata(c);
            metadataSet.add(metadata);
        }

        return metadataSet;
    }

    /* package */Set<Resource> buildResources(Set<ClassMetadata> metadataSet) {
        Set<Resource> s = new HashSet<Resource>(metadataSet.size());
        for (ClassMetadata metadata : metadataSet) {
            Resource r = buildResource(metadata);
            s.add(r);
        }

        return s;
    }

    /* package */Resource buildResource(ClassMetadata metadata) {
        Resource r = new Resource();

        if (metadata != null) {
            Class<?> resClass = metadata.getResourceClass();
            if (resClass != null) {
                WADLDoc d = resClass.getAnnotation(WADLDoc.class);
                if (d != null) {
                    r.getDoc().add(getDocument(d));
                }
            }
        }

        /* set the path */
        String path = metadata.getPath();

        if (path == null) {
            Class<?> resClass = metadata.getResourceClass();
            if (DynamicResource.class.isAssignableFrom(resClass)) {
                try {
                    DynamicResource dynRes = (DynamicResource)resClass.newInstance();
                    path = dynRes.getPath();
                } catch (Exception e) {
                    // Drop through and look for @Path annotation.
                }
            }
        }

        UriTemplateProcessor processor = JaxRsUriTemplateProcessor.newNormalizedInstance(path);
        String pathStr = processor.getTemplate();
        for (String var : processor.getVariableNames()) {
            pathStr = pathStr.replaceAll("\\{(\\s)*" + var + "(\\s)*:.*\\}", "{" + var + "}");
        }
        r.setPath(pathStr);

        List<Object> methodOrSubresource = r.getMethodOrResource();
        List<Param> resourceParams = r.getParam();

        List<MethodMetadata> methodMetadata = metadata.getResourceMethods();

        if (methodMetadata != null && !methodMetadata.isEmpty()) {
            for (MethodMetadata methodMeta : methodMetadata) {
                Method m = buildMethod(metadata, methodMeta);
                methodOrSubresource.add(m);

                /* also scan for all the path and matrix parameters */
                List<Injectable> params = methodMeta.getFormalParameters();
                if (params != null && params.size() > 0) {
                    for (Injectable p : params) {
                        Param param = null;
                        boolean isUnique = true;
                        switch (p.getParamType()) {
                            case QUERY:
                                /* do nothing */
                                break;
                            case HEADER:
                                /* do nothing */
                                break;
                            case ENTITY:
                                /* do nothing */
                                break;
                            case COOKIE:
                                /* not supported in WADL */
                                break;
                            case FORM:
                                /* should show up in the representation instead */
                                break;
                            case PATH:
                                param = buildParam(p);
                                for (Param rParam : resourceParams) {
                                    if (param.getName() != null && param.getName().equals(rParam
                                        .getName())
                                        && rParam.getStyle().equals(param.getStyle())) {
                                        isUnique = false;
                                    }
                                }
                                if (isUnique) {
                                    resourceParams.add(param);
                                }
                                break;
                            case MATRIX:
                                param = buildParam(p);
                                for (Param rParam : resourceParams) {
                                    if (param.getName() != null && param.getName().equals(rParam
                                        .getName())
                                        && rParam.getStyle().equals(param.getStyle())) {
                                        isUnique = false;
                                    }
                                }
                                if (isUnique) {
                                    resourceParams.add(param);
                                }
                                break;
                            case CONTEXT:
                                /* do nothing */
                                break;
                        }
                    }
                }
            }
        }

        /*
         * list the class level parameters
         */
        List<Injectable> fields = metadata.getInjectableFields();
        if (fields != null) {
            for (Injectable p : metadata.getInjectableFields()) {
                Param param = null;
                boolean isUnique = true;

                switch (p.getParamType()) {
                    case QUERY:
                        resourceParams.add(buildParam(p));
                        break;
                    case HEADER:
                        resourceParams.add(buildParam(p));
                        break;
                    case ENTITY:
                        /* do nothing */
                        break;
                    case COOKIE:
                        /* not supported in WADL */
                        break;
                    case FORM:
                        /* should show up in the representation instead */
                        break;
                    case PATH:
                        param = buildParam(p);
                        for (Param rParam : resourceParams) {
                            if (param.getName() != null && param.getName().equals(rParam.getName())
                                && rParam.getStyle().equals(param.getStyle())) {
                                isUnique = false;
                            }
                        }
                        if (isUnique) {
                            resourceParams.add(param);
                        }
                        break;
                    case MATRIX:
                        param = buildParam(p);
                        for (Param rParam : resourceParams) {
                            if (param.getName() != null && param.getName().equals(rParam.getName())
                                && rParam.getStyle().equals(param.getStyle())) {
                                isUnique = false;
                            }
                        }
                        if (isUnique) {
                            resourceParams.add(param);
                        }
                        break;
                    case CONTEXT:
                        /* do nothing */
                        break;
                }
            }
        }

        /*
         * list subresource methods
         */
        methodMetadata = metadata.getSubResourceMethods();
        if (methodMetadata != null && !methodMetadata.isEmpty()) {
            for (MethodMetadata methodMeta : methodMetadata) {
                Resource subRes = new Resource();
                Method m = buildMethod(metadata, methodMeta);
                subRes.getMethodOrResource().add(m);
                subRes.setPath(methodMeta.getPath());
                methodOrSubresource.add(subRes);
                List<Param> subResParams = subRes.getParam();
                /* also scan for all the path and matrix parameters */
                List<Injectable> params = methodMeta.getFormalParameters();
                if (params != null && params.size() > 0) {
                    for (Injectable p : params) {
                        Param param = null;
                        boolean isUnique = true;

                        switch (p.getParamType()) {
                            case QUERY:
                                /* do nothing */
                                break;
                            case HEADER:
                                /* do nothing */
                                break;
                            case ENTITY:
                                /* do nothing */
                                break;
                            case COOKIE:
                                /* not supported in WADL */
                                break;
                            case FORM:
                                /* should show up in the representation instead */
                                break;
                            case PATH:
                                param = buildParam(p);
                                for (Param rParam : subResParams) {
                                    if (param.getName() != null && param.getName().equals(rParam
                                        .getName())
                                        && rParam.getStyle().equals(param.getStyle())) {
                                        isUnique = false;
                                    }
                                }
                                if (isUnique) {
                                    subRes.getParam().add(param);
                                }
                                break;
                            case MATRIX:
                                param = buildParam(p);
                                for (Param rParam : subResParams) {
                                    if (param.getName() != null && param.getName().equals(rParam
                                        .getName())
                                        && rParam.getStyle().equals(param.getStyle())) {
                                        isUnique = false;
                                    }
                                }
                                if (isUnique) {
                                    subRes.getParam().add(param);
                                }
                                break;
                            case CONTEXT:
                                /* do nothing */
                                break;
                        }
                    }
                }
            }
        }

        /*
         * list subresource locators
         */
        methodMetadata = metadata.getSubResourceLocators();
        if (methodMetadata != null && !methodMetadata.isEmpty()) {
            for (MethodMetadata methodMeta : methodMetadata) {
                Resource subRes = new Resource();
                subRes.setPath(methodMeta.getPath());

                if (methodMeta != null) {
                    java.lang.reflect.Method reflMethod = methodMeta.getReflectionMethod();
                    if (reflMethod != null) {
                        WADLDoc d = reflMethod.getAnnotation(WADLDoc.class);
                        if (d != null) {
                            subRes.getDoc().add(getDocument(d));
                        }
                    }
                }

                methodOrSubresource.add(subRes);

                /* also scan for all the path and matrix parameters */
                List<Injectable> params = methodMeta.getFormalParameters();
                List<Param> subResParams = subRes.getParam();
                if (params != null && params.size() > 0) {
                    for (Injectable p : params) {
                        Param param = null;
                        boolean isUnique = true;

                        switch (p.getParamType()) {
                            case QUERY:
                                param = buildParam(p);
                                for (Param rParam : subResParams) {
                                    if (param.getName() != null && param.getName().equals(rParam
                                        .getName())
                                        && rParam.getStyle().equals(param.getStyle())) {
                                        isUnique = false;
                                    }
                                }
                                if (isUnique) {
                                    subRes.getParam().add(param);
                                }
                                break;
                            case HEADER:
                                param = buildParam(p);
                                for (Param rParam : subResParams) {
                                    if (param.getName() != null && param.getName().equals(rParam
                                        .getName())
                                        && rParam.getStyle().equals(param.getStyle())) {
                                        isUnique = false;
                                    }
                                }
                                if (isUnique) {
                                    subRes.getParam().add(param);
                                }
                                break;
                            case ENTITY:
                                /* do nothing */
                                break;
                            case COOKIE:
                                /* not supported in WADL */
                                break;
                            case FORM:
                                /* should show up in the representation instead */
                                break;
                            case PATH:
                                param = buildParam(p);
                                for (Param rParam : subResParams) {
                                    if (param.getName() != null && param.getName().equals(rParam
                                        .getName())
                                        && rParam.getStyle().equals(param.getStyle())) {
                                        isUnique = false;
                                    }
                                }
                                if (isUnique) {
                                    subRes.getParam().add(param);
                                }
                                break;
                            case MATRIX:
                                param = buildParam(p);
                                for (Param rParam : subResParams) {
                                    if (param.getName() != null && param.getName().equals(rParam
                                        .getName())
                                        && rParam.getStyle().equals(param.getStyle())) {
                                        isUnique = false;
                                    }
                                }
                                if (isUnique) {
                                    subRes.getParam().add(param);
                                }
                                break;
                            case CONTEXT:
                                /* do nothing */
                                break;
                        }
                    }
                }
            }
        }

        return r;
    }

    /* package */Method buildMethod(ClassMetadata classMetadata, MethodMetadata metadata) {
        Method m = new Method();
        m.setName(metadata.getHttpMethod());

        java.lang.reflect.Method reflMethod = metadata.getReflectionMethod();
        if (reflMethod != null) {
            WADLDoc d = reflMethod.getAnnotation(WADLDoc.class);
            if (d != null) {
                m.getDoc().add(getDocument(d));
            }
        }

        Request r = buildRequest(classMetadata, metadata);
        if (r != null) {
            m.setRequest(r);
        }

        List<Response> resp = buildResponse(metadata);
        if (resp != null) {
            m.getResponse().addAll(resp);
        }
        return m;
    }

    /* package */Request buildRequest(ClassMetadata classMetadata, MethodMetadata metadata) {
        Request r = null;

        List<Injectable> params = metadata.getFormalParameters();
        if (params != null && params.size() > 0) {
            if (r == null) {
                r = new Request();
            }
            boolean hasValidParams = false;
            List<Param> requestParams = r.getParam();
            for (Injectable p : params) {
                Param param = null;
                boolean isUnique = true;
                switch (p.getParamType()) {
                    case QUERY:
                        hasValidParams = true;
                        param = buildParam(p);
                        for (Param rParam : requestParams) {
                            if (param.getName() != null && param.getName().equals(rParam.getName())
                                && rParam.getStyle().equals(param.getStyle())) {
                                isUnique = false;
                            }
                        }
                        if (isUnique) {
                            requestParams.add(buildParam(p));
                        }

                        break;
                    case HEADER:
                        hasValidParams = true;
                        param = buildParam(p);
                        for (Param rParam : requestParams) {
                            if (param.getName() != null && param.getName().equals(rParam.getName())
                                && rParam.getStyle().equals(param.getStyle())) {
                                isUnique = false;
                            }
                        }
                        if (isUnique) {
                            requestParams.add(buildParam(p));
                        }
                        break;
                    case ENTITY:
                        hasValidParams = true;

                        Annotation[] anns = p.getAnnotations();
                        if (anns != null && anns.length > 0) {
                            for (Annotation a : anns) {
                                if (WADLDoc.class.equals(a.annotationType())) {
                                    WADLDoc d = (WADLDoc)a;
                                    r.getDoc().add(getDocument(d));
                                }
                            }
                        }

                        /* need to build the representation */
                        Set<Representation> representations =
                            buildIncomingRepresentation(classMetadata, metadata, p);
                        if (representations != null)
                            r.getRepresentation().addAll(representations);
                        break;
                    case COOKIE:
                        /* not supported in WADL */
                        break;
                    case FORM:
                        /* should show up in the representation instead */
                        break;
                    case PATH:
                        /* should show up in the containing resource */
                        break;
                    case MATRIX:
                        /* should show up in the containing resource */
                        break;
                    case CONTEXT:
                        /* do nothing */
                        break;
                }
            }
            if (!hasValidParams) {
                r = null;
            }
        }

        return r;
    }

    /* package */Param buildParam(Injectable paramMetadata) {
        Param p = null;
        ParamType pt = paramMetadata.getParamType();
        Annotation[] annotations = paramMetadata.getAnnotations();
        switch (pt) {
            case HEADER:
                p = new Param();
                p.setStyle(ParamStyle.HEADER);
                if (annotations != null) {
                    for (Annotation a : annotations) {
                        if (HeaderParam.class.equals(a.annotationType())) {
                            HeaderParam paramAnn = (HeaderParam)a;
                            p.setName(paramAnn.value());
                        }
                    }
                }
                break;
            case MATRIX:
                p = new Param();
                p.setStyle(ParamStyle.MATRIX);
                for (Annotation a : annotations) {
                    if (MatrixParam.class.equals(a.annotationType())) {
                        MatrixParam paramAnn = (MatrixParam)a;
                        p.setName(paramAnn.value());
                    }
                }
                break;
            case PATH:
                p = new Param();
                p.setStyle(ParamStyle.TEMPLATE);
                for (Annotation a : annotations) {
                    if (PathParam.class.equals(a.annotationType())) {
                        PathParam paramAnn = (PathParam)a;
                        p.setName(paramAnn.value());
                    }
                }
                break;
            case QUERY:
                p = new Param();
                p.setStyle(ParamStyle.QUERY);
                for (Annotation a : annotations) {
                    if (QueryParam.class.equals(a.annotationType())) {
                        QueryParam paramAnn = (QueryParam)a;
                        p.setName(paramAnn.value());
                    }
                }
                break;
            case FORM:
                p = new Param();
                p.setStyle(ParamStyle.QUERY);
                for (Annotation a : annotations) {
                    if (FormParam.class.equals(a.annotationType())) {
                        FormParam paramAnn = (FormParam)a;
                        p.setName(paramAnn.value());
                    }
                }
                break;
            case CONTEXT:
                break;
            case COOKIE:
                break;
            case ENTITY:
                break;
        }

        if (p == null) {
            /*
             * The paramtype was never set so return null. This might have been
             * some other type of injectable that shouldn't be created as a
             * Param. This is a preventive measure.
             */
            return null;
        }

        Class<?> memberType = paramMetadata.getType();
        if (memberType.equals(int.class) || memberType.equals(Integer.class)) {
            p.setType(new QName(XML_SCHEMA_NS, "int"));
        } else if (memberType.equals(float.class) || memberType.equals(Float.class)) {
            p.setType(new QName(XML_SCHEMA_NS, "float"));
        } else if (memberType.equals(long.class) || memberType.equals(Long.class)) {
            p.setType(new QName(XML_SCHEMA_NS, "long"));
        } else if (memberType.equals(boolean.class) || memberType.equals(Boolean.class)) {
            p.setType(new QName(XML_SCHEMA_NS, "boolean"));
        } else if (memberType.equals(short.class) || memberType.equals(Short.class)) {
            p.setType(new QName(XML_SCHEMA_NS, "short"));
        } else if (memberType.equals(double.class) || memberType.equals(Double.class)) {
            p.setType(new QName(XML_SCHEMA_NS, "double"));
        } else if (memberType.equals(byte.class) || memberType.equals(Byte.class)) {
            p.setType(new QName(XML_SCHEMA_NS, "byte"));
        }

        for (Annotation a : annotations) {
            if (DefaultValue.class.equals(a.annotationType())) {
                DefaultValue paramAnn = (DefaultValue)a;
                p.setDefault(paramAnn.value());
            }

            if (WADLDoc.class.equals(a.annotationType())) {
                WADLDoc d = (WADLDoc)a;
                p.getDoc().add(getDocument(d));
            }
        }

        return p;
    }

    /* package */Set<Representation> buildIncomingRepresentation(ClassMetadata classMetadata,
                                                                 MethodMetadata methodMetadata,
                                                                 Injectable metadata) {
        if (methodMetadata == null) {
            return null;
        }

        if (metadata != null && metadata.getParamType() != ParamType.ENTITY) {
            throw new IllegalArgumentException("Parameter data is not for an entity.");
        }

        /*
         * first check all the consumes
         */
        Set<MediaType> consumesMT = methodMetadata.getConsumes();
        Set<Representation> reps = null;
        if (consumesMT != null && !consumesMT.isEmpty()) {
            reps = new HashSet<Representation>();
            for (MediaType mt : consumesMT) {
                Representation r = new Representation();
                r.setMediaType(mt.toString());

                /*
                 * if the representation is a special case, we need to build for
                 * it
                 */
                /*
                 * special cases include application/xml, text/xml; should set
                 * the element attribute
                 */
                List<Param> params = new ArrayList<Param>();
                if (mt.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE) || mt
                    .isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
                    List<Injectable> injectables = methodMetadata.getFormalParameters();
                    if (injectables != null) {
                        for (Injectable i : injectables) {
                            if (ParamType.FORM.equals(i.getParamType())) {
                                params.add(buildParam(i));
                            }
                        }
                    }

                    /*
                     * should we scan the class metadata too? FormParams aren't
                     * required to be supported in other places than the
                     * resource method parameters
                     */
                    if (!params.isEmpty()) {
                        r.getParam().addAll(params);
                    }
                }
                reps.add(r);
            }
        } else {
            if (metadata == null) {
                return null;
            }

            /*
             * there aren't any consumes so we can't look for that but maybe we
             * can look at the Providers registry and find all the relevant
             * media types there
             */
        }

        return reps;
    }

    /* package */List<Response> buildResponse(MethodMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        Response r = null;
        // Set<MediaType> produces = metadata.getProduces();
        Set<Representation> representations = buildOutgoingRepresentation(metadata, null);
        if (representations != null && !representations.isEmpty()) {
            r = new Response();
            r.getRepresentation().addAll(representations);
        }

        java.lang.reflect.Method m = metadata.getReflectionMethod();
        if (Void.TYPE.equals(m.getReturnType())) {
            if (r == null) {
                r = new Response();
            }
            r.getStatus().add(Long.valueOf(204));
        } else if (!javax.ws.rs.core.Response.class.isAssignableFrom(m.getReturnType())) {
            if (r == null) {
                r = new Response();
            }
            r.getStatus().add(Long.valueOf(200));
        }

        return Collections.singletonList(r);
    }

    /* package */Set<Representation> buildOutgoingRepresentation(MethodMetadata methodMetadata,
                                                                 Class<?> returnType) {
        if (methodMetadata == null) {
            return null;
        }

        /*
         * first check all the consumes
         */
        Set<MediaType> producesMT = methodMetadata.getProduces();
        Set<Representation> reps = null;
        if (producesMT != null && !producesMT.isEmpty()) {
            reps = new HashSet<Representation>();
            for (MediaType mt : producesMT) {
                Representation r = new Representation();
                r.setMediaType(mt.toString());

                /*
                 * if the representation is a special case, we need to build for
                 * it
                 */
                /*
                 * TODO: special cases include application/xml, text/xml; should
                 * set the element attribute
                 */
                reps.add(r);
            }
        } else {
            /*
             * there aren't any produces so we can't look for that but maybe we
             * can look at the Providers registry and find all the relevant
             * media types there
             */
        }

        return reps;
    }

    /* package */Doc getDocument(WADLDoc desc) {
        Doc d = new Doc();
        d.setTitle(desc.value());
        if (!"".equals(desc.content())) {
            d.getContent().add(desc.content());
        }
        return d;
    }

    /*
     * Customized isResource method so it accepts interfaces.
     */
    protected boolean isResource(Class<?> cls) {
        if (ResourceMetadataCollector.isDynamicResource(cls)) {
            return true;
        }

        if (cls.getAnnotation(Path.class) != null) {
            return true;
        }

        Class<?> declaringClass = cls;

        while (!declaringClass.equals(Object.class)) {
            // try a superclass
            Class<?> superclass = declaringClass.getSuperclass();
            if (superclass.getAnnotation(Path.class) != null) {
                return true;
            }

            // try interfaces
            Class<?>[] interfaces = declaringClass.getInterfaces();
            for (Class<?> interfaceClass : interfaces) {
                if (interfaceClass.getAnnotation(Path.class) != null) {
                    return true;
                }
            }
            declaringClass = declaringClass.getSuperclass();
        }
        return false;
    }

}
