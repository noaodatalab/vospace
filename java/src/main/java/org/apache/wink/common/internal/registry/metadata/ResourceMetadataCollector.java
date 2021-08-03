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

package org.apache.wink.common.internal.registry.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;

import org.apache.wink.common.DynamicResource;
import org.apache.wink.common.annotations.Parent;
import org.apache.wink.common.annotations.Workspace;
import org.apache.wink.common.internal.i18n.Messages;
import org.apache.wink.common.internal.registry.Injectable;
import org.apache.wink.common.internal.registry.InjectableFactory;
import org.apache.wink.common.internal.utils.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects ClassMetadata from JAX-RS Resource classes
 */
public class ResourceMetadataCollector extends AbstractMetadataCollector {

    private static final Logger logger = LoggerFactory.getLogger(ResourceMetadataCollector.class);

    private ResourceMetadataCollector(Class<?> clazz) {
        super(clazz);
    }

    public static boolean isResource(Class<?> cls) {
        return (isStaticResource(cls) || isDynamicResource(cls));
    }

    public static boolean isStaticResource(Class<?> cls) {
        if (Modifier.isInterface(cls.getModifiers()) || Modifier.isAbstract(cls.getModifiers())) {
            logger.trace("isStaticResource() exit returning false because interface or abstract");

            return false;
        }

        if (cls.getAnnotation(Path.class) != null) {
            logger.trace("isStaticResource() exit returning true");
            return true;
        }

        Class<?> declaringClass = cls;

        while (!declaringClass.equals(Object.class)) {
            // try a superclass
            Class<?> superclass = declaringClass.getSuperclass();
            if (superclass.getAnnotation(Path.class) != null) {
                if (logger.isWarnEnabled()) {
                    logger.warn(Messages
                        .getMessage("rootResourceShouldBeAnnotatedDirectly", cls, superclass)); //$NON-NLS-1$
                }
                logger.trace("isStaticResource() exit returning true because {} has @Path",
                             superclass);
                return true;
            }

            // try interfaces
            Class<?>[] interfaces = declaringClass.getInterfaces();
            for (Class<?> interfaceClass : interfaces) {
                if (interfaceClass.getAnnotation(Path.class) != null) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(Messages.getMessage("rootResourceShouldBeAnnotatedDirectly", //$NON-NLS-1$
                                                        cls,
                                                        interfaceClass));
                    }
                    logger.trace("isStaticResource() exit returning true because {} has @Path",
                                 interfaceClass);
                    return true;
                }
            }
            declaringClass = declaringClass.getSuperclass();
        }
        logger.trace("isStaticResource() exit returning false");
        return false;
    }

    public static boolean isDynamicResource(Class<?> cls) {
        return DynamicResource.class.isAssignableFrom(cls);
    }

    public static ClassMetadata collectMetadata(Class<?> clazz) {
        logger.trace("collectMetadata({}) entry", clazz);
        ResourceMetadataCollector collector = new ResourceMetadataCollector(clazz);
        collector.parseClass();
        collector.parseFields();
        collector.parseConstructors();
        collector.parseMethods();
        ClassMetadata md = collector.getMetadata();
        logger.trace("collectMetadata() exit returning {}", md);
        return md;
    }

    @Override
    protected final Injectable parseAccessibleObject(AccessibleObject field, Type fieldType) {
        Injectable injectable =
            InjectableFactory.getInstance().create(fieldType,
                                                   field.getAnnotations(),
                                                   (Member)field,
                                                   getMetadata().isEncoded(),
                                                   null);
        if (injectable.getParamType() == Injectable.ParamType.ENTITY) {
            // EntityParam should be ignored for fields (see JSR-311 3.2)
            return null;
        }
        return injectable;
    }

    private void parseClass() {
        Class<?> cls = getMetadata().getResourceClass();
        parseClass(cls);
    }

    private boolean parseClass(Class<?> cls) {
        logger.trace("parseClass({})", cls);
        boolean workspacePresent = parseWorkspace(cls);
        boolean pathPresent = parsePath(cls);
        boolean consumesPresent = parseClassConsumes(cls);
        boolean producesPresent = parseClassProduces(cls);

        Parent parent = cls.getAnnotation(Parent.class);
        if (parent != null) {
            getMetadata().getParents().add(parent.value());
        }

        parseEncoded(cls);

        // if the class contained any annotations, we can to stop
        if (workspacePresent || pathPresent || consumesPresent || producesPresent) {
            return true;
        }

        // no annotations
        return false;
    }

    private boolean parseWorkspace(Class<?> cls) {
        Workspace workspace = cls.getAnnotation(Workspace.class);
        if (workspace != null) {
            getMetadata().setWorkspaceName(workspace.workspaceTitle());
            getMetadata().setCollectionTitle(workspace.collectionTitle());
            return true;
        }
        return false;
    }

    private boolean parsePath(Class<?> cls) {
        Path path = cls.getAnnotation(Path.class);
        if (path != null) {
            getMetadata().addPath(path.value());
            logger.trace("parseClass() returning true for class direct");
            return true;
        }

        Class<?> declaringClass = cls;

        while (!declaringClass.equals(Object.class)) {
            // try a superclass
            Class<?> superclass = declaringClass.getSuperclass();
            path = superclass.getAnnotation(Path.class);
            if (path != null) {
                getMetadata().addPath(path.value());
                logger.trace("parseClass() returning true for superclass {}", superclass);
                return true;
            }

            // try interfaces
            Class<?>[] interfaces = declaringClass.getInterfaces();
            for (Class<?> interfaceClass : interfaces) {
                path = interfaceClass.getAnnotation(Path.class);
                if (path != null) {
                    getMetadata().addPath(path.value());
                    logger.trace("parseClass() returning true for interface {}", interfaceClass);
                    return true;
                }
            }
            declaringClass = declaringClass.getSuperclass();
        }
        logger.trace("parseClass() returning false");
        return false;
    }

    private void parseMethods() {
        logger.trace("entry");
        F1: for (Method method : getMetadata().getResourceClass().getMethods()) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
                continue F1;
            }
            MethodMetadata methodMetadata = createMethodMetadata(method);
            logger.trace("Found methodMetadata {} for method {}", methodMetadata, method);

            if (methodMetadata != null) {
                String path = methodMetadata.getPath();
                String httpMethod = methodMetadata.getHttpMethod();
                if (path != null) {
                    // sub-resource
                    if (httpMethod != null) {
                        logger.trace("Was subresource method");
                        // sub-resource method
                        getMetadata().getSubResourceMethods().add(methodMetadata);
                    } else {
                        logger.trace("Was subresource locator");
                        // sub-resource locator
                        // verify that the method does not take an entity
                        // parameter
                        String methodName =
                            String.format("%s.%s", declaringClass.getName(), method.getName()); //$NON-NLS-1$
                        for (Injectable id : methodMetadata.getFormalParameters()) {
                            if (id.getParamType() == Injectable.ParamType.ENTITY) {
                                if (logger.isWarnEnabled()) {
                                    logger.warn(Messages
                                        .getMessage("subresourceLocatorIllegalEntityParameter", //$NON-NLS-1$
                                                    methodName));
                                }
                                continue F1;
                            }
                        }
                        // log a warning if the locator has a Produces or
                        // Consumes annotation
                        if (!methodMetadata.getConsumes().isEmpty() || !methodMetadata
                            .getProduces().isEmpty()) {
                            if (logger.isWarnEnabled()) {
                                logger.warn(Messages
                                    .getMessage("subresourceLocatorAnnotatedConsumesProduces", //$NON-NLS-1$
                                                methodName));
                            }
                        }
                        getMetadata().getSubResourceLocators().add(methodMetadata);
                    }
                } else {
                    logger.trace("Was resource method");
                    // resource method
                    getMetadata().getResourceMethods().add(methodMetadata);
                }
            }
        }
        logger.trace("exit");
    }

    private MethodMetadata createMethodMetadata(Method method) {
        logger.trace("createMethodMetadata({})", method);
        int modifiers = method.getModifiers();
        // only public, non-static methods
        if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            return null;
        }

        MethodMetadata metadata = new MethodMetadata(getMetadata());
        metadata.setReflectionMethod(method);

        boolean hasAnnotation = false;

        HttpMethod httpMethod = getHttpMethod(method);
        if (httpMethod != null) {
            hasAnnotation = true;
            metadata.setHttpMethod(httpMethod.value());
        }

        Path path = getPath(method);
        if (path != null) {
            hasAnnotation = true;
            metadata.addPath(path.value());
        }

        String[] consumes = getConsumes(method);
        for (String mediaType : consumes) {
            hasAnnotation = true;
            metadata.addConsumes(MediaType.valueOf(mediaType));
        }

        String[] produces = getProduces(method);
        for (String mediaType : produces) {
            hasAnnotation = true;
            metadata.addProduces(MediaType.valueOf(mediaType));
        }

        String defaultValue = getDefaultValue(method);
        if (defaultValue != null) {
            metadata.setDefaultValue(defaultValue);
            hasAnnotation = true;
        }

        if (method.getAnnotation(Encoded.class) != null) {
            metadata.setEncoded(true);
            hasAnnotation = true;
        }

        // if the method has no annotation at all,
        // then it may override a method in a superclass or interface that has
        // annotations,
        // so try looking at the overridden method annotations
        // but keep the method params as the super may have declared a generic
        // type param
        if (!hasAnnotation) {
            logger
                .trace("Method did not directly have annotation so going up the class hierarchy chain");
            Class<?> declaringClass = method.getDeclaringClass();

            // try a superclass
            Class<?> superclass = declaringClass.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                MethodMetadata createdMetadata = createMethodMetadata(superclass, method);
                // stop with if the method found
                if (createdMetadata != null) {
                    mergeFormalParameterMetadata(createdMetadata, method);
                    logger.trace("createMethodMetadata() exit returning {} from superclass {}",
                                 createdMetadata,
                                 superclass);
                    return createdMetadata;
                }
            }

            // try interfaces
            Class<?>[] interfaces = declaringClass.getInterfaces();
            for (Class<?> interfaceClass : interfaces) {
                MethodMetadata createdMetadata = createMethodMetadata(interfaceClass, method);
                // stop with the first method found
                if (createdMetadata != null) {
                    mergeFormalParameterMetadata(createdMetadata, method);
                    logger.trace("createMethodMetadata() exit returning {} from interface {}",
                                 createdMetadata,
                                 interfaceClass);
                    return createdMetadata;
                }
            }

            // annotations are not inherited. ignore this method.
            logger.trace("createdMethodMetadata() returning null");
            return null;
        }

        // check if it's a valid resource method/sub-resource
        // method/sub-resource locator,
        // since there is at least one JAX-RS annotation on the method
        if (metadata.getHttpMethod() == null && metadata.getPath() == null) {
            if (metadata.isEncoded() || defaultValue != null) {
                // property methods may have @Encoded or @DefaultValue but
                // are not HTTP methods/paths
                logger.trace("createdMethodMetadata() returning null");
                return null;
            }
            if (logger.isWarnEnabled()) {
                logger.warn(Messages.getMessage("methodNotAnnotatedCorrectly", //$NON-NLS-1$
                                                method.getName(),
                                                method.getDeclaringClass().getCanonicalName()));
            }
            logger.trace("createdMethodMetadata() returning null");
            return null;
        }

        parseMethodParameters(method, metadata);

        logger.trace("createMethodMetadata() exit returning {}", metadata);
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private MethodMetadata createMethodMetadata(Class<?> declaringClass, Method method) {
        logger.trace("createMethodMetadata({}, {}) entry", declaringClass, method);
        try {
            Method declaredMethod =
                declaringClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            return createMethodMetadata(declaredMethod);
        } catch (SecurityException e) {
            // can't get to overriding method
            logger.trace("createMethodMetadata() exit returning null because of SecurityException");
            return null;
        } catch (NoSuchMethodException e) {
            // see if declaringClass's declaredMethod uses generic parameters
            Method[] methods = declaringClass.getMethods();
            for (Method candidateMethod : methods) {
                boolean matchFound = true;
                if (candidateMethod.getName().equals(method.getName())) {
                    // name matches, now check the param signature:
                    if (candidateMethod.getParameterTypes().length == method.getParameterTypes().length) {
                        // so far so good. Now make sure the params are
                        // acceptable:
                        for (int i = 0; i < candidateMethod.getParameterTypes().length; i++) {
                            Class clazz = candidateMethod.getParameterTypes()[i];
                            if (clazz.isPrimitive() && !clazz.equals(candidateMethod
                                .getParameterTypes()[i])) {
                                matchFound = false; // signature doesn't match,
                                                    // otherwise it
                                // would have been found in
                                // getDeclaredMethod above
                            }
                            if (!clazz.isAssignableFrom(method.getParameterTypes()[i])) {
                                matchFound = false;
                            }
                        }
                        if (matchFound) {
                            return createMethodMetadata(candidateMethod);
                        }
                    }
                }
            }
            // no overriding method exists
            logger
                .trace("createMethodMetadata() exit returning null because of NoSuchMethodException");
            return null;
        }
    }

    private boolean parseClassConsumes(Class<?> cls) {
        String[] consumes = getConsumes(cls);
        // if (consumes.length == 0) {
        // getMetadata().addConsumes(MediaType.WILDCARD_TYPE);
        // return false;
        // }
        for (String mediaType : consumes) {
            getMetadata().addConsumes(MediaType.valueOf(mediaType));
        }
        return true;
    }

    private boolean parseClassProduces(Class<?> cls) {
        String[] consumes = getProduces(cls);
        // if (consumes.length == 0) {
        // getMetadata().addProduces(MediaType.WILDCARD_TYPE);
        // return false;
        // }
        for (String mediaType : consumes) {
            getMetadata().addProduces(MediaType.valueOf(mediaType));
        }
        return true;
    }

    private String[] getConsumes(AnnotatedElement element) {
        Consumes consumes = element.getAnnotation(Consumes.class);
        if (consumes != null) {
            return AnnotationUtils.parseConsumesProducesValues(consumes.value());
        }
        return new String[] {};
    }

    private String[] getProduces(AnnotatedElement element) {
        Produces produces = element.getAnnotation(Produces.class);
        if (produces != null) {
            return AnnotationUtils.parseConsumesProducesValues(produces.value());
        }
        return new String[] {};
    }

    private Path getPath(Method method) {
        return method.getAnnotation(Path.class);
    }

    private HttpMethod getHttpMethod(Method method) {
        // search if any of the annotations is annotated with HttpMethod
        // such as @GET
        HttpMethod httpMethod = null;
        for (Annotation annotation : method.getAnnotations()) {
            HttpMethod httpMethodCurr = annotation.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethodCurr != null) {
                if (httpMethod != null) {
                    throw new IllegalStateException(Messages
                        .getMessage("multipleHttpMethodAnnotations", method //$NON-NLS-1$
                            .getName(), method.getDeclaringClass().getCanonicalName()));
                }
                httpMethod = httpMethodCurr;
            }
        }
        return httpMethod;
    }

    private String getDefaultValue(Method method) {
        DefaultValue defaultValueAnn = method.getAnnotation(DefaultValue.class);
        if (defaultValueAnn != null) {
            return defaultValueAnn.value();
        }
        return null;
    }

    private void parseMethodParameters(Method method, MethodMetadata methodMetadata) {
        logger.trace("parseMethodParameters({}, {}), entry", method, methodMetadata);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Type[] paramTypes = getParamTypesFilterByXmlElementAnnotation(method);
        boolean entityParamExists = false;
        for (int pos = 0, limit = paramTypes.length; pos < limit; pos++) {
            Injectable fp =
                InjectableFactory.getInstance().create(paramTypes[pos],
                                                       parameterAnnotations[pos],
                                                       method,
                                                       getMetadata().isEncoded() || methodMetadata
                                                           .isEncoded(),
                                                       methodMetadata.getDefaultValue());
            if (fp.getParamType() == Injectable.ParamType.ENTITY) {
                if (entityParamExists) {
                    // we are allowed to have only one entity parameter
                    String methodName =
                        method.getDeclaringClass().getName() + "." + method.getName(); //$NON-NLS-1$
                    throw new IllegalStateException(Messages
                        .getMessage("resourceMethodMoreThanOneEntityParam", methodName)); //$NON-NLS-1$
                }
                entityParamExists = true;
            }
            methodMetadata.getFormalParameters().add(fp);
            logger.trace("Adding formal parameter {}", fp);
        }
        logger.trace("parseMethodParameters(), exit");
    }

    private Type[] getParamTypesFilterByXmlElementAnnotation(Method method) {
        int index = 0;
        Type[] paramTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (Annotation[] annos : paramAnnotations) {
            for (Annotation anno : annos) {
                if (anno.annotationType().equals(XmlElement.class)) {
                    XmlElement xmlElement = (XmlElement)anno;
                    Type type = xmlElement.type();
                    if (type != null) {
                        paramTypes[index] = type;
                    }
                }
            }
            index++;
        }
        return paramTypes;
    }

    private void mergeFormalParameterMetadata(MethodMetadata metadata, Method method) {
        logger.trace("mergeFormalParameterMetadata({})", new Object[] {metadata, method});
        Type[] parameterTypes = method.getGenericParameterTypes();
        List<Injectable> currentParameters =
            new ArrayList<Injectable>(metadata.getFormalParameters());
        metadata.getFormalParameters().clear();
        int i = 0;
        for (Injectable injectable : currentParameters) {
            Injectable fp =
                InjectableFactory.getInstance().create(parameterTypes[i],
                                                       injectable.getAnnotations(),
                                                       method,
                                                       getMetadata().isEncoded() || metadata
                                                           .isEncoded(),
                                                       metadata.getDefaultValue());
            metadata.getFormalParameters().add(fp);
            ++i;
        }
        logger.trace("mergeFormalParameterMetadata exit");
    }

    @Override
    protected final boolean isConstructorParameterValid(Injectable fp) {
        // This method is declared as final, since parseConstructors(), which
        // calls it, is invoked from the constructor
        return !(fp.getParamType() == Injectable.ParamType.ENTITY);
    }

}
