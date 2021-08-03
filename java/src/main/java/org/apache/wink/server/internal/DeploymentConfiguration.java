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
package org.apache.wink.server.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;

import org.apache.commons.lang.ClassUtils;
import org.apache.wink.common.internal.WinkConfiguration;
import org.apache.wink.common.internal.application.ApplicationValidator;
import org.apache.wink.common.internal.i18n.Messages;
import org.apache.wink.common.internal.lifecycle.JSR250LifecycleManager;
import org.apache.wink.common.internal.lifecycle.LifecycleManagersRegistry;
import org.apache.wink.common.internal.lifecycle.ObjectFactory;
import org.apache.wink.common.internal.lifecycle.ScopeLifecycleManager;
import org.apache.wink.common.internal.registry.InjectableFactory;
import org.apache.wink.common.internal.registry.ProvidersRegistry;
import org.apache.wink.common.internal.utils.FileLoader;
import org.apache.wink.server.handlers.Handler;
import org.apache.wink.server.handlers.HandlersFactory;
import org.apache.wink.server.handlers.MediaTypeMapperFactory;
import org.apache.wink.server.handlers.RequestHandler;
import org.apache.wink.server.handlers.RequestHandlersChain;
import org.apache.wink.server.handlers.ResponseHandler;
import org.apache.wink.server.handlers.ResponseHandlersChain;
import org.apache.wink.server.internal.application.ApplicationProcessor;
import org.apache.wink.server.internal.handlers.CheckLocationHeaderHandler;
import org.apache.wink.server.internal.handlers.CreateInvocationParametersHandler;
import org.apache.wink.server.internal.handlers.FindResourceMethodHandler;
import org.apache.wink.server.internal.handlers.FindRootResourceHandler;
import org.apache.wink.server.internal.handlers.FlushResultHandler;
import org.apache.wink.server.internal.handlers.HeadMethodHandler;
import org.apache.wink.server.internal.handlers.InvokeMethodHandler;
import org.apache.wink.server.internal.handlers.OptionsMethodHandler;
import org.apache.wink.server.internal.handlers.PopulateErrorResponseHandler;
import org.apache.wink.server.internal.handlers.PopulateResponseMediaTypeHandler;
import org.apache.wink.server.internal.handlers.PopulateResponseStatusHandler;
import org.apache.wink.server.internal.handlers.SearchResultHandler;
import org.apache.wink.server.internal.log.Requests;
import org.apache.wink.server.internal.log.ResourceInvocation;
import org.apache.wink.server.internal.log.Responses;
import org.apache.wink.server.internal.registry.ResourceRegistry;
import org.apache.wink.server.internal.registry.ServerInjectableFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class implements a default deployment configuration for Wink. In order
 * to change this configuration, extend this class and override the relevant
 * methods. In general it's possible to override any methods of this class, but
 * the best practices are to override methods the "init" methods. See the
 * javadoc for each method for more details.
 */
public class DeploymentConfiguration implements WinkConfiguration {

    private static final Logger       logger                              =
                                                                              LoggerFactory
                                                                                  .getLogger(DeploymentConfiguration.class);
    private static final String       ALTERNATIVE_SHORTCUTS               =
                                                                              "META-INF/wink-alternate-shortcuts.properties"; //$NON-NLS-1$
    private static final String       HTTP_METHOD_OVERRIDE_HEADERS_PROP   =
                                                                              "wink.httpMethodOverrideHeaders";              //$NON-NLS-1$
    private static final String       HANDLERS_FACTORY_CLASS_PROP         =
                                                                              "wink.handlersFactoryClass";                   //$NON-NLS-1$
    private static final String       MEDIATYPE_MAPPER_FACTORY_CLASS_PROP =
                                                                              "wink.mediaTypeMapperFactoryClass";            //$NON-NLS-1$
    private static final String       VALIDATE_LOCATION_HEADER            =
                                                                              "wink.validateLocationHeader";                 //$NON-NLS-1$
    private static final String       DEFAULT_RESPONSE_CHARSET            =
                                                                              "wink.response.defaultCharset";                // $NON-NLS-1$
    private static final String       USE_ACCEPT_CHARSET                  =
                                                                              "wink.response.useAcceptCharset";              // $NON-NLS-1$
    // handler chains
    private boolean                   isChainInitialized                  = false;
    private RequestHandlersChain      requestHandlersChain;
    private ResponseHandlersChain     responseHandlersChain;
    private ResponseHandlersChain     errorHandlersChain;

    private List<RequestHandler>      requestUserHandlers;
    private List<ResponseHandler>     responseUserHandlers;
    private List<ResponseHandler>     errorUserHandlers;

    // registries
    private ProvidersRegistry         providersRegistry;
    private ResourceRegistry          resourceRegistry;
    private LifecycleManagersRegistry ofFactoryRegistry;

    // mappers
    private MediaTypeMapper           mediaTypeMapper;
    private Map<String, String>       alternateShortcutMap;

    // external properties
    private Properties                properties;

    // servlet configuration
    private ServletConfig             servletConfig;
    private ServletContext            servletContext;
    private FilterConfig              filterConfig;

    // jax-rs application subclass
    private List<Application>         applications;

    private String[]                  httpMethodOverrideHeaders;

    private Set<ObjectFactory<?>>     appObjectFactories;

    private boolean                   isUseAcceptCharset                  = false;

    private boolean                   isDefaultResponseCharset            = false;

    /**
     * Makes sure that the object was properly initialized. Should be invoked
     * AFTER all the setters were invoked.
     */
    public void init() {
        if (properties == null) {
            properties = new Properties();
        }
        appObjectFactories = new HashSet<ObjectFactory<?>>(8);

        logger.trace("Deployment configuration properties: {}", properties); //$NON-NLS-1$

        // check to see if an override property was specified. if so, then
        // configure
        // the headers from there using a comma delimited string.
        String httpMethodOverrideHeadersProperty =
            properties.getProperty(HTTP_METHOD_OVERRIDE_HEADERS_PROP);
        httpMethodOverrideHeaders =
            (httpMethodOverrideHeadersProperty != null && httpMethodOverrideHeadersProperty
                .length() > 0) ? httpMethodOverrideHeadersProperty.split(",") : null; //$NON-NLS-1$

        initRegistries();
        initAlternateShortcutMap();
        initMediaTypeMapper();
        initHandlers();

        // this next code is to dump the config to trace after initialization
        if (logger.isDebugEnabled()) {
            try {
                logger.debug("Configuration Settings:");
                logger.trace("Request Handlers Chain : {}", requestHandlersChain);
                logger.trace("Response Handlers Chain: {}", responseHandlersChain);
                logger.trace("Error Handlers Chain: {}", errorHandlersChain);
                logger.debug("Request User Handlers: {}", String
                    .format("%1$s", requestUserHandlers));
                logger.debug("Response User Handlers: {}", String.format("%1$s",
                                                                         responseUserHandlers));
                logger.debug("Error User Handlers: {}", String.format("%1$s", errorUserHandlers));
                logger.trace("LifecycleManagerRegistry: {}", this.ofFactoryRegistry
                    .getLifecycleManagers());
                logger.debug("MediaTypeMapper: {}", this.mediaTypeMapper); // TODO
                // add
                // toString
                // method
                // for
                // MediaTypeMapper
                logger.debug("AlternateShortcutMap: {}", this.alternateShortcutMap);
                logger.debug("Properties: {}", this.properties);
                logger.trace("ServletConfig: {}", this.servletConfig);
                logger.trace("ServletContext: {}", this.servletContext);
                logger.trace("FilterConfig: {}", this.filterConfig);
                logger.trace("Applications: {}", this.applications);
                List<String> httpMethodOverrideHeadersList = new ArrayList<String>();
                if (this.httpMethodOverrideHeaders != null) {
                    for (int i = 0; i < this.httpMethodOverrideHeaders.length; ++i)
                        httpMethodOverrideHeadersList.add(this.httpMethodOverrideHeaders[i]);
                }
                logger.debug("HttpMethodOverrideHeaders: {}", httpMethodOverrideHeadersList);
            } catch (Exception e) {
                // make sure we don't fail in case anything wrong happens here
                logger.debug("An Exception occurred when logging the configuration {}", e);
            }
        }
    }

    public RequestHandlersChain getRequestHandlersChain() {
        if (!isChainInitialized) {
            initHandlersChain();
        }
        return requestHandlersChain;
    }

    public void setRequestHandlersChain(RequestHandlersChain requestHandlersChain) {
        this.requestHandlersChain = requestHandlersChain;
    }

    public ResponseHandlersChain getResponseHandlersChain() {
        if (!isChainInitialized) {
            initHandlersChain();
        }
        return responseHandlersChain;
    }

    public void setResponseHandlersChain(ResponseHandlersChain responseHandlersChain) {
        this.responseHandlersChain = responseHandlersChain;
    }

    public ResponseHandlersChain getErrorHandlersChain() {
        if (!isChainInitialized) {
            initHandlersChain();
        }
        return errorHandlersChain;
    }

    public void setErrorHandlersChain(ResponseHandlersChain errorHandlersChain) {
        this.errorHandlersChain = errorHandlersChain;
    }

    public ProvidersRegistry getProvidersRegistry() {
        return providersRegistry;
    }

    public ResourceRegistry getResourceRegistry() {
        return resourceRegistry;
    }

    public MediaTypeMapper getMediaTypeMapper() {
        return mediaTypeMapper;
    }

    public void setMediaTypeMapper(MediaTypeMapper mediaTypeMapper) {
        this.mediaTypeMapper = mediaTypeMapper;
    }

    public void setOfFactoryRegistry(LifecycleManagersRegistry ofFactoryRegistry) {
        this.ofFactoryRegistry = ofFactoryRegistry;
    }

    public LifecycleManagersRegistry getOfFactoryRegistry() {
        return ofFactoryRegistry;
    }

    public Map<String, String> getAlternateShortcutMap() {
        return alternateShortcutMap;
    }

    public void setAlternateShortcutMap(Map<String, String> alternateShortcutMap) {
        this.alternateShortcutMap = alternateShortcutMap;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
        String val = properties.getProperty(USE_ACCEPT_CHARSET);
        isUseAcceptCharset = Boolean.valueOf(val).booleanValue();
        val = properties.getProperty(DEFAULT_RESPONSE_CHARSET);
        isDefaultResponseCharset = Boolean.valueOf(val).booleanValue();
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setRequestUserHandlers(List<RequestHandler> requestUserHandlers) {
        this.requestUserHandlers = requestUserHandlers;
    }

    public void setResponseUserHandlers(List<ResponseHandler> responseUserHandlers) {
        this.responseUserHandlers = responseUserHandlers;
    }

    public List<ResponseHandler> getResponseUserHandlers() {
        return responseUserHandlers;
    }

    public List<RequestHandler> getRequestUserHandlers() {
        return requestUserHandlers;
    }

    public void setErrorUserHandlers(List<ResponseHandler> errorUserHandlers) {
        this.errorUserHandlers = errorUserHandlers;
    }

    public List<ResponseHandler> getErrorUserHandlers() {
        return errorUserHandlers;
    }

    public void addApplication(Application application, boolean isSystemApplication) {
        if (applications == null) {
            applications = new ArrayList<Application>(1);
        }
        new ApplicationProcessor(application, resourceRegistry, providersRegistry,
                                 isSystemApplication).process();
        applications.add(application);
    }

    public List<Application> getApplications() {
        return applications;
    }

    // init methods

    /**
     * Initializes registries. Usually there should be no need to override this
     * method. When creating Resources or Providers registry, ensure that they
     * use the same instance of the ApplicationValidator.
     */
    protected void initRegistries() {
        InjectableFactory.setInstance(new ServerInjectableFactory());
        if (ofFactoryRegistry == null) {
            ofFactoryRegistry = new LifecycleManagersRegistry();
            ofFactoryRegistry.addFactoryFactory(new ScopeLifecycleManager<Object>());
            ofFactoryRegistry.addFactoryFactory(new JSR250LifecycleManager<Object>());
        }
        ApplicationValidator applicationValidator = new ApplicationValidator();
        providersRegistry = new ProvidersRegistry(ofFactoryRegistry, applicationValidator);
        resourceRegistry =
            new ResourceRegistry(ofFactoryRegistry, applicationValidator, properties);
    }

    /**
     * Initializes the AlternateShortcutMap. Override this method in order to
     * provide a custom AlternateShortcutMap.
     */
    protected void initAlternateShortcutMap() {
        if (alternateShortcutMap == null) {
            InputStream is = null;
            try {
                is = FileLoader.loadFileAsStream(ALTERNATIVE_SHORTCUTS);
                Properties lproperties = new Properties();

                lproperties.load(is);
                logger.trace("Alternative shortcuts properties: {}", lproperties); //$NON-NLS-1$
                alternateShortcutMap = new HashMap<String, String>();
                for (Entry<Object, Object> entry : lproperties.entrySet()) {
                    alternateShortcutMap.put((String)entry.getKey(), (String)entry.getValue());
                }
                logger.trace("Alternative shortcuts map: {}", alternateShortcutMap); //$NON-NLS-1$
            } catch (IOException e) {
                logger.error(Messages.getMessage("alternateShortcutMapLoadFailure"), e); //$NON-NLS-1$
                throw new WebApplicationException(e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    logger
                        .info(Messages.getMessage("alternateShortcutMapCloseFailure") + ALTERNATIVE_SHORTCUTS, //$NON-NLS-1$
                              e);
                }
            }
        }
    }

    /**
     * Initializes the MediaTypeMapper. Override it to provide a custom
     * MediaTypeMapper.
     */
    @SuppressWarnings("unchecked")
    protected void initMediaTypeMapper() {
        if (mediaTypeMapper == null) {
            String mediaTypeMapperFactoryClassName =
                properties.getProperty(MEDIATYPE_MAPPER_FACTORY_CLASS_PROP);
            if (mediaTypeMapperFactoryClassName != null) {
                mediaTypeMapper = new MediaTypeMapper();
                try {
                    logger.trace("MediaTypeMappingFactory Class is: {}", //$NON-NLS-1$
                                 mediaTypeMapperFactoryClassName);
                    Class<MediaTypeMapperFactory> handlerFactoryClass =
                        (Class<MediaTypeMapperFactory>)Class
                            .forName(mediaTypeMapperFactoryClassName);
                    MediaTypeMapperFactory handlersFactory = handlerFactoryClass.newInstance();
                    mediaTypeMapper.addMappings(handlersFactory.getMediaTypeMappings());
                } catch (ClassNotFoundException e) {
                    if (logger.isErrorEnabled()) {
                        logger.error(Messages.getMessage("isNotAClassWithMsgFormat", //$NON-NLS-1$
                                                         mediaTypeMapperFactoryClassName), e);
                    }
                } catch (InstantiationException e) {
                    if (logger.isErrorEnabled()) {
                        logger.error(Messages
                            .getMessage("classInstantiationExceptionWithMsgFormat", //$NON-NLS-1$
                                        mediaTypeMapperFactoryClassName), e);
                    }
                } catch (IllegalAccessException e) {
                    if (logger.isErrorEnabled()) {
                        logger.error(Messages.getMessage("classIllegalAccessWithMsgFormat", //$NON-NLS-1$
                                                         mediaTypeMapperFactoryClassName), e);
                    }
                }
            }
        }
    }

    /**
     * Initializes the main handlers chain. Override in order to change the
     * chains.
     */
    @SuppressWarnings("unchecked")
    private void initHandlers() {

        String handlersFactoryClassName = properties.getProperty(HANDLERS_FACTORY_CLASS_PROP);
        if (handlersFactoryClassName != null) {
            try {
                logger.trace("Handlers Factory Class is: {}", handlersFactoryClassName); //$NON-NLS-1$
                // use ClassUtils.getClass instead of Class.forName so we have
                // classloader visibility into the Web module in J2EE
                // environments
                Class<HandlersFactory> handlerFactoryClass =
                    (Class<HandlersFactory>)ClassUtils.getClass(handlersFactoryClassName);
                HandlersFactory handlersFactory = handlerFactoryClass.newInstance();
                if (requestUserHandlers == null) {
                    requestUserHandlers =
                        (List<RequestHandler>)handlersFactory.getRequestHandlers();
                }
                if (responseUserHandlers == null) {
                    responseUserHandlers =
                        (List<ResponseHandler>)handlersFactory.getResponseHandlers();
                }
                if (errorUserHandlers == null) {
                    errorUserHandlers = (List<ResponseHandler>)handlersFactory.getErrorHandlers();
                }
            } catch (ClassNotFoundException e) {
                logger.error(Messages.getMessage("isNotAClassWithMsgFormat", //$NON-NLS-1$
                                                 handlersFactoryClassName), e);
            } catch (InstantiationException e) {
                logger.error(Messages.getMessage("classInstantiationExceptionWithMsgFormat", //$NON-NLS-1$
                                                 handlersFactoryClassName), e);
            } catch (IllegalAccessException e) {
                logger.error(Messages.getMessage("classIllegalAccessWithMsgFormat", //$NON-NLS-1$
                                                 handlersFactoryClassName), e);
            }
        }

        if (requestUserHandlers == null) {
            requestUserHandlers = initRequestUserHandlers();
        }
        if (responseUserHandlers == null) {
            responseUserHandlers = initResponseUserHandlers();
        }
        if (errorUserHandlers == null) {
            errorUserHandlers = initErrorUserHandlers();
        }

    }

    private void initHandlersChain() {
        if (requestHandlersChain == null) {
            requestHandlersChain = initRequestHandlersChain();
        }
        if (responseHandlersChain == null) {
            responseHandlersChain = initResponseHandlersChain();
        }
        if (errorHandlersChain == null) {
            errorHandlersChain = initErrorHandlersChain();
        }

        isChainInitialized = true;
    }

    /**
     * Initializes the Request Handlers Chain (the chain that handles the
     * in-bound request). Usually the user won't need to override this method,
     * but <tt>initRequestUserHandlers</tt> instead.
     * 
     * @see initRequestUserHandlers
     */
    @SuppressWarnings("unchecked")
    protected RequestHandlersChain initRequestHandlersChain() {
        RequestHandlersChain handlersChain = new RequestHandlersChain();
        handlersChain.addHandler(createHandler(Requests.class));
        handlersChain.addHandler(createHandler(ResourceInvocation.class));
        handlersChain.addHandler(createHandler(SearchResultHandler.class));
        String optionsHandler =
            properties.getProperty("org.apache.wink.server.options.handler",
                                   OptionsMethodHandler.class.getName());
        if ("none".equals(optionsHandler)) {
            optionsHandler = OptionsMethodHandler.class.getName();
        }
        logger.trace("org.apache.wink.server.options.handler value is {}", optionsHandler);
        try {
            handlersChain.addHandler(createHandler((Class<? extends RequestHandler>)Class
                .forName(optionsHandler)));
        } catch (Exception e) {
            logger.trace("Could not load handlers class so adding default");
            handlersChain.addHandler(createHandler(OptionsMethodHandler.class));
        }

        handlersChain.addHandler(createHandler(HeadMethodHandler.class));
        handlersChain.addHandler(createHandler(FindRootResourceHandler.class));
        handlersChain.addHandler(createHandler(FindResourceMethodHandler.class));
        handlersChain.addHandler(createHandler(CreateInvocationParametersHandler.class));
        if (requestUserHandlers != null) {
            for (RequestHandler h : requestUserHandlers) {
                h.init(properties);
                handlersChain.addHandler(h);
            }
        }
        handlersChain.addHandler(createHandler(InvokeMethodHandler.class));
        logger.trace("Request handlers chain is: {}", handlersChain); //$NON-NLS-1$
        return handlersChain;
    }

    /**
     * Initializes request (inbound) user handler. By default this method
     * returns an empty list. Override to add user handlers.
     * 
     * @return list of RequestHandler
     * @see RequestHandler
     */
    protected List<RequestHandler> initRequestUserHandlers() {
        return Collections.emptyList();
    }

    /**
     * Initializes response (outbound) user handlers. By default this method
     * returns an empty list. Override to add user handlers.
     * 
     * @return list of ResponseHandler
     * @see ResponseHandler
     */
    protected List<ResponseHandler> initResponseUserHandlers() {
        ArrayList<ResponseHandler> list = new ArrayList<ResponseHandler>(1);
        if (Boolean.parseBoolean(properties.getProperty(VALIDATE_LOCATION_HEADER))) {
            list.add(new CheckLocationHeaderHandler());
        }
        return list;

    }

    /**
     * Initializes error user handlers.By default this method returns an empty
     * list. Override to add user handlers.
     * 
     * @return list of ResponseHandler
     * @see ResponseHandler
     */
    protected List<ResponseHandler> initErrorUserHandlers() {
        return Collections.emptyList();
    }

    /**
     * Initializes the Response Handlers Chain (the chain that handles the
     * out-bound response). Usually the user won't need to override this method,
     * but <tt>initResponseUserHandlers</tt> instead.
     */
    protected ResponseHandlersChain initResponseHandlersChain() {
        ResponseHandlersChain handlersChain = new ResponseHandlersChain();
        handlersChain.addHandler(createHandler(Responses.class));
        handlersChain.addHandler(createHandler(PopulateResponseStatusHandler.class));
        handlersChain.addHandler(createHandler(PopulateResponseMediaTypeHandler.class));
        if (responseUserHandlers != null) {
            for (ResponseHandler h : responseUserHandlers) {
                h.init(properties);
                handlersChain.addHandler(h);
            }
        }
        handlersChain.addHandler(createHandler(FlushResultHandler.class));
        handlersChain.addHandler(createHandler(HeadMethodHandler.class));
        logger.trace("Response handlers chain is: {}", handlersChain); //$NON-NLS-1$
        return handlersChain;
    }

    /**
     * Initializes the Error Handlers Chain (the chain that handles the
     * exceptions). Usually the user won't need to override this method, but
     * <tt>initErrorUserHandlers</tt> instead.
     */
    protected ResponseHandlersChain initErrorHandlersChain() {
        ResponseHandlersChain handlersChain = new ResponseHandlersChain();

        Responses responsesHandler = createHandler(Responses.class);
        responsesHandler.setIsErrorFlow(true);
        handlersChain.addHandler(responsesHandler);

        handlersChain.addHandler(createHandler(PopulateErrorResponseHandler.class));
        handlersChain.addHandler(createHandler(PopulateResponseStatusHandler.class));
        PopulateResponseMediaTypeHandler populateMediaTypeHandler =
            createHandler(PopulateResponseMediaTypeHandler.class);
        populateMediaTypeHandler.setErrorFlow(true);
        handlersChain.addHandler(populateMediaTypeHandler);
        if (errorUserHandlers != null) {
            for (ResponseHandler h : errorUserHandlers) {
                h.init(properties);
                handlersChain.addHandler(h);
            }
        }
        handlersChain.addHandler(createHandler(FlushResultHandler.class));
        logger.trace("Error handlers chain is: {}", handlersChain); //$NON-NLS-1$
        return handlersChain;
    }

    protected <T extends Handler> T createHandler(Class<T> cls) {
        try {
            T handler = cls.newInstance();
            logger.trace("Calling {}.init(Properties)", cls); //$NON-NLS-1$
            handler.init(getProperties());
            return handler;
        } catch (InstantiationException e) {
            throw new WebApplicationException(e);
        } catch (IllegalAccessException e) {
            throw new WebApplicationException(e);
        }
    }

    public void setHttpMethodOverrideHeaders(String[] httpMethodOverrideHeaders) {
        this.httpMethodOverrideHeaders = httpMethodOverrideHeaders;
        if (logger.isTraceEnabled()) {
            List<String> overrideHeaders =
                (httpMethodOverrideHeaders == null) ? null : Arrays
                    .asList(httpMethodOverrideHeaders);
            logger.trace("Setting HTTP Method override headers: {}", overrideHeaders); //$NON-NLS-1$
        }
    }

    public String[] getHttpMethodOverrideHeaders() {
        return httpMethodOverrideHeaders;
    }

    /**
     * isDefaultResponseCharset will write charset=UTF-8 to the response
     * Content-Type header if a response charset is not already explicitly
     * defined.
     * 
     * @return boolean
     */
    public boolean isDefaultResponseCharset() {
        return isDefaultResponseCharset;
    }

    public void setDefaultResponseCharset(boolean val) {
        properties.setProperty(DEFAULT_RESPONSE_CHARSET, Boolean.toString(val));
        isDefaultResponseCharset = val;
    }

    /**
     * isUseAcceptCharset will use the Accept-Charset header, if present, to
     * write a charset to the response Content-Type header if a response charset
     * is not already explicitly defined. This setting will override the
     * isDefaultResponseCharset setting when the Accept-Charset header is
     * present.
     * 
     * @return
     */
    public boolean isUseAcceptCharset() {
        return isUseAcceptCharset;
    }

    public void setUseAcceptCharset(boolean val) {
        properties.setProperty(USE_ACCEPT_CHARSET, Boolean.toString(val));
        isUseAcceptCharset = val;
    }

    public void addApplicationObjectFactory(ObjectFactory<?> of) {
        appObjectFactories.add(of);
    }

    public Set<ObjectFactory<?>> getApplicationObjectFactories() {
        return appObjectFactories;
    }
}
