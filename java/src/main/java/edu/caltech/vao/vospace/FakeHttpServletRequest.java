
package edu.caltech.vao.vospace;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class FakeHttpServletRequest implements HttpServletRequest {

  private final UrlInfo requestUrl;
  private final Map<String, Object> attributes;
  private final Map<String, String> parameters;
  private final String method;
  private final HttpSession session;
  private final String remoteAddr;
  private final String scheme;
  private final String serverName;
  private final int serverPort;
  private final Map<String, String> headers;

  private ServletInputStream inputStream = new ServletInputStream() {
    @Override
    public int read() throws IOException {
      return 0;
    }
  };

    public FakeHttpServletRequest(String method, UrlInfo requestUrl, HttpSession session, String remoteAddr, String scheme, String serverName, int serverPort, String userAgent) {
    this.attributes = new HashMap<String, Object>();
    this.parameters = new HashMap<String, String>();
    this.headers = new HashMap<String, String>();
    this.method = method.toUpperCase();
    this.requestUrl = requestUrl;
    this.session = session;
    this.remoteAddr = remoteAddr;
    this.scheme = scheme;
    this.serverName = serverName;
    this.serverPort = serverPort;
    headers.put("User-Agent", userAgent);

    setBody("");
  }
  
  public void setParameters(Map<String, String> parameters) {
    this.parameters.clear();
    this.parameters.putAll(parameters);
  }

  public void setBody(final String data) {
    this.inputStream = new ServletInputStream() {
      private final ByteArrayInputStream delegate =
          new ByteArrayInputStream(data.getBytes());

      @Override
      public void close() throws IOException {
        delegate.close();
      }

      @Override
      public int read() throws IOException {
        return delegate.read();
      }
    };
  }

    //    public void setSession(HttpSession session) {
    //	this.session = session;
    //    }

    //    public void setRemoteAddr(String remoteAddr) {
    //	this.remoteAddr = remoteAddr;
    //    }

  /////////////////////////////////////////////////////////////////////////////
  //
  //  HttpServletRequest methods.
  //
  /////////////////////////////////////////////////////////////////////////////

  public String getAuthType() {
    throw new UnsupportedOperationException();
  }

  public Cookie[] getCookies() {
    throw new UnsupportedOperationException();
  }

  public String getMethod() {
    return method;
  }

  public String getPathInfo() {
    return requestUrl.getPathInfo();
  }

  public String getPathTranslated() {
    throw new UnsupportedOperationException();
  }

  public String getContextPath() {
    return requestUrl.getContextPath();
  }

  public String getQueryString() {
    throw new UnsupportedOperationException();
  }

  public String getRemoteUser() {
    throw new UnsupportedOperationException();
  }

  public boolean isUserInRole(String s) {
    throw new UnsupportedOperationException();
  }

  public Principal getUserPrincipal() {
    throw new UnsupportedOperationException();
  }

  public String getRequestedSessionId() {
    throw new UnsupportedOperationException();
  }

  public String getRequestURI() {
    return requestUrl.toString();
  }

  public StringBuffer getRequestURL() {
    return new StringBuffer(requestUrl.toString());
  }

  public String getServletPath() {
    return requestUrl.getServletPath();
  }

  public HttpSession getSession(boolean b) {
      return this.session;
      //    throw new UnsupportedOperationException();
  }

  public HttpSession getSession() {
      return this.session;
      //    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdValid() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromCookie() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromURL() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromUrl() {
    throw new UnsupportedOperationException();
  }

  public Object getAttribute(String s) {
    return attributes.get(s);
  }

  public Enumeration getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  public String getCharacterEncoding() {
    throw new UnsupportedOperationException();
  }

  public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
  throw new UnsupportedOperationException();
  }

  public int getContentLength() {
    throw new UnsupportedOperationException();
  }

  public String getContentType() {
    throw new UnsupportedOperationException();
  }

  public ServletInputStream getInputStream() throws IOException {
    return inputStream;
  }

  public long getDateHeader(String name) throws IllegalArgumentException {
      return -1; 
  }

  public String getHeader(String name) {
      return headers.get(name);
  }

  public Enumeration<String> getHeaders(String name) {
      Vector<String> v = new Vector<String>();
      return v.elements();
  }

  public Enumeration<String> getHeaderNames() {
      Vector<String> v = new Vector<String>();
      return v.elements();
  }

  public int getIntHeader(String s) throws NumberFormatException {
    return -1;
  }

  public String getParameter(String s) {
    return parameters.get(s);
  }

  public Enumeration getParameterNames() {
    return Collections.enumeration(parameters.keySet());
  }

  public String[] getParameterValues(String s) {
    Collection<String> values = parameters.values();
    return values.toArray(new String[values.size()]);
  }

  public Map getParameterMap() {
    return Collections.unmodifiableMap(parameters);
  }

  public String getProtocol() {
    throw new UnsupportedOperationException();
  }

  public String getScheme() {
      return this.scheme;
  }

  public String getServerName() {
      return this.serverName;
  }

  public int getServerPort() {
    return this.serverPort;
  }

  public BufferedReader getReader() throws IOException {
    throw new UnsupportedOperationException();
  }

  public String getRemoteAddr() {
      return this.remoteAddr;
  }

  public String getRemoteHost() {
    throw new UnsupportedOperationException();
  }

  public void setAttribute(String name, Object value) {
    if (name.startsWith(":")) {
      name = name.substring(1);
    }
    attributes.put(name, value);
  }

  public void removeAttribute(String name) {
    if (name.startsWith(":")) {
      name = name.substring(1);
    }
    attributes.remove(name);
  }

  public Locale getLocale() {
    throw new UnsupportedOperationException();
  }

  public Enumeration getLocales() {
    throw new UnsupportedOperationException();
  }

  public boolean isSecure() {
    throw new UnsupportedOperationException();
  }

  public RequestDispatcher getRequestDispatcher(String s) {
    throw new UnsupportedOperationException();
  }

  public String getRealPath(String s) {
    throw new UnsupportedOperationException();
  }

  public int getRemotePort() {
    throw new UnsupportedOperationException();
  }

  public String getLocalName() {
    throw new UnsupportedOperationException();
  }

  public String getLocalAddr() {
    throw new UnsupportedOperationException();
  }

  public int getLocalPort() {
    throw new UnsupportedOperationException();
  }
}
