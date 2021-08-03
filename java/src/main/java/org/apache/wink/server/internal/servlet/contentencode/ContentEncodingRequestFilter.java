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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.wink.server.internal.servlet.contentencode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet filter which changes the HttpServletRequest to automatically
 * inflate or GZIP decode an incoming request that has an appropriate
 * Content-Encoding request header value. Add to your web.xml like: <br/>
 * <code>
 * &lt;filter&gt;<br/>
        &lt;filter-name&gt;ContentEncodingRequestFilter&lt;/filter-name&gt;<br/>
        &lt;filter-class&gt;org.apache.wink.server.internal.servlet.contentencode.ContentEncodingRequestFilter&lt;/filter-class&gt;<br/>
    &lt;/filter&gt;<br/>
    <br/>
    &lt;filter-mapping&gt;<br/>
        &lt;filter-name&gt;ContentEncodingRequestFilter&lt;/filter-name&gt;<br/>
        &lt;url-pattern&gt;/*&lt;/url-pattern&gt;<br/>
    &lt;/filter-mapping&gt;<br/>
 * </code>
 */
public class ContentEncodingRequestFilter implements Filter {

    private static final Logger logger =
                                           LoggerFactory
                                               .getLogger(ContentEncodingRequestFilter.class);

    public void init(FilterConfig arg0) throws ServletException {
        logger.trace("init({}) entry", arg0); //$NON-NLS-1$
        /* do nothing */
        logger.trace("init() exit"); //$NON-NLS-1$
    }

    public void destroy() {
        logger.trace("destroy() entry"); //$NON-NLS-1$
        /* do nothing */
        logger.trace("destroy() exit"); //$NON-NLS-1$
    }

    private String getContentEncoding(HttpServletRequest httpServletRequest) {
        String contentEncoding = httpServletRequest.getHeader(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding == null) {
            return null;
        }
        contentEncoding.trim();
        return contentEncoding;
    }

    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        if (logger.isTraceEnabled()) {
            logger.trace("doFilter({}, {}, {}) entry", new Object[] {servletRequest, //$NON-NLS-1$
                servletResponse, chain});
        }
        if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
            HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
            String contentEncoding = getContentEncoding(httpServletRequest);
            logger.trace("Content-Encoding was {}", contentEncoding); //$NON-NLS-1$
            if (contentEncoding != null) {
                if ("gzip".equals(contentEncoding) || "deflate".equals(contentEncoding)) { //$NON-NLS-1$ //$NON-NLS-2$
                    logger
                        .trace("Wrapping HttpServletRequest because Content-Encoding was set to gzip or deflate"); //$NON-NLS-1$
                    httpServletRequest =
                        new HttpServletRequestContentEncodingWrapperImpl(httpServletRequest,
                                                                         contentEncoding);
                    logger.trace("Invoking chain with wrapped HttpServletRequest"); //$NON-NLS-1$
                    chain.doFilter(httpServletRequest, servletResponse);
                    logger.trace("doFilter exit()"); //$NON-NLS-1$
                    return;
                }
            }
        }
        logger
            .trace("Invoking normal chain since Content-Encoding request header was not understood"); //$NON-NLS-1$
        chain.doFilter(servletRequest, servletResponse);
        logger.trace("doFilter exit()"); //$NON-NLS-1$
    }

    static class DecoderServletInputStream extends ServletInputStream {

        final private InputStream is;

        public DecoderServletInputStream(InputStream is) {
            this.is = is;
        }

        @Override
        public int readLine(byte[] b, int off, int len) throws IOException {
            return is.read(b, off, len);
        }

        @Override
        public int available() throws IOException {
            return is.available();
        }

        @Override
        public void close() throws IOException {
            is.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            is.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return is.markSupported();
        }

        @Override
        public int read() throws IOException {
            return is.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return is.read(b, off, len);
        }

        @Override
        public int read(byte[] b) throws IOException {
            return is.read(b);
        }

        @Override
        public synchronized void reset() throws IOException {
            is.reset();
        }

        @Override
        public long skip(long n) throws IOException {
            return is.skip(n);
        }
    }

    static class GZIPDecoderInputStream extends DecoderServletInputStream {

        public GZIPDecoderInputStream(InputStream is) throws IOException {
            super(new GZIPInputStream(is));
        }
    }

    static class InflaterDecoderInputStream extends DecoderServletInputStream {

        public InflaterDecoderInputStream(InputStream is) {
            super(new InflaterInputStream(is));
        }

    }

    static class HttpServletRequestContentEncodingWrapperImpl extends HttpServletRequestWrapper {

        private ServletInputStream inputStream;

        final private String       contentEncoding;

        public HttpServletRequestContentEncodingWrapperImpl(HttpServletRequest request,
                                                            String contentEncoding) {
            super(request);
            this.contentEncoding = contentEncoding;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            logger.trace("getInputStream() entry"); //$NON-NLS-1$
            if (inputStream == null) {
                inputStream = super.getInputStream();
                if ("gzip".equals(contentEncoding)) { //$NON-NLS-1$
                    logger.trace("Wrapping ServletInputStream with GZIPDecoder"); //$NON-NLS-1$
                    inputStream = new GZIPDecoderInputStream(inputStream);
                } else if ("deflate".equals(contentEncoding)) { //$NON-NLS-1$
                    logger.trace("Wrapping ServletInputStream with Inflater"); //$NON-NLS-1$
                    inputStream = new InflaterDecoderInputStream(inputStream);
                }
            }
            logger.trace("getInputStream() exit - returning {}", inputStream); //$NON-NLS-1$
            return inputStream;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(name)) {
                return null;
            }
            return super.getHeader(name);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(name)) {
                // an empty enumeration
                return new Enumeration<String>() {

                    public boolean hasMoreElements() {
                        return false;
                    }

                    public String nextElement() {
                        return null;
                    }
                };
            }
            return super.getHeaders(name);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Enumeration getHeaderNames() {
            final Enumeration<String> headers = super.getHeaderNames();
            List<String> httpHeaders = new ArrayList<String>();
            while (headers.hasMoreElements()) {
                String header = headers.nextElement();
                if (!HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(header)) {
                    httpHeaders.add(header);
                }
            }
            final Iterator<String> iterator = httpHeaders.iterator();
            return new Enumeration<String>() {

                public boolean hasMoreElements() {
                    return iterator.hasNext();
                }

                public String nextElement() {
                    return iterator.next();
                }

            };
        }
    }

}
