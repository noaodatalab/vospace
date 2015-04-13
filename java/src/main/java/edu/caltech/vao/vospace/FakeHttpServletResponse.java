
package edu.caltech.vao.vospace;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class FakeHttpServletResponse implements HttpServletResponse {

  private final StringWriter stringWriter = new StringWriter();
  private final ServletOutputStream servletOutputStream =
      new StringServletOutputStream(stringWriter);
  private int status = HttpServletResponse.SC_OK;
  private Map<String, String> headers;
  private Map<String, Integer> intHeaders;  
  private boolean isCommitted;
  private String contentType;
 
  public FakeHttpServletResponse(boolean isCommitted, String contentType) {
      headers = new HashMap<String, String>();
      intHeaders = new HashMap<String, Integer>();
      this.isCommitted = isCommitted;
      this.contentType = contentType;
  }

  public int getStatus() {
    return status;
  }
  
  public String getBody() {
    return stringWriter.toString();
  }

  private void setLocalHeader(String keyword, String value) {
      headers.put(keyword, value);
  }

  private void setLocalIntHeader(String keyword, int value) {
      intHeaders.put(keyword, value);
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  //  HttpServletResponse methods.
  //
  /////////////////////////////////////////////////////////////////////////////

  public void addCookie(Cookie cookie) {
    throw new UnsupportedOperationException();
  }

  public String encodeURL(String s) {
    throw new UnsupportedOperationException();
  }

  public String encodeRedirectURL(String s) {
    throw new UnsupportedOperationException();
  }

  public String encodeUrl(String s) {
    throw new UnsupportedOperationException();
  }

  public String encodeRedirectUrl(String s) {
    throw new UnsupportedOperationException();
  }

  public void sendError(int i, String s) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void sendError(int i) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void sendRedirect(String s) throws IOException {
    setStatus(SC_SEE_OTHER);
    setLocalHeader("Location", s);
  }

  public void setStatus(int i) {
    this.status = i;
  }

  public void setStatus(int i, String s) {
    throw new UnsupportedOperationException();
  }

  public String getCharacterEncoding() {
    throw new UnsupportedOperationException();
  }

  public String getContentType() {
      return this.contentType;
  }

  public ServletOutputStream getOutputStream() throws IOException {
    return servletOutputStream;
  }

  public PrintWriter getWriter() throws IOException {
      return new PrintWriter(stringWriter);
  }

  public void setCharacterEncoding(String s) {
    String type = getHeader("content-type");
    setLocalHeader("content-type", type + "; charset=" + s);
  }

  public void setContentLength(int i) {
    setLocalIntHeader("content-length", i);
  }

  public void addIntHeader(String name, int value) {
      setLocalIntHeader(name, value);
  }

  public void setIntHeader(String name, int value) {
      setLocalIntHeader(name, value);
  }

  public void addHeader(String name, String value) {
      setLocalHeader(name, value);
  }

  public void setHeader(String name, String value) {
      setLocalHeader(name, value);
  }

  public void setContentType(String type) {
    setLocalHeader("content-type", type);
  }

  public void addDateHeader(String name, long date) {}

  public void setDateHeader(String name, long date) {}

  public boolean containsHeader(String name) {
      return headers.containsKey(name);
  }

  public String getHeader(String keyword) {
      return headers.get(keyword);
  }

  public Collection<String> getHeaders(String name) {
      return headers.values();
  }

  public Collection<String> getHeaderNames() {
      return headers.keySet();
  }

  public void setBufferSize(int i) {
    throw new UnsupportedOperationException();
  }

  public int getBufferSize() {
    throw new UnsupportedOperationException();
  }

  public void flushBuffer() throws IOException {
    // no-op
  }

  public void resetBuffer() {
    // no-op;
  }

  public boolean isCommitted() {
      return this.isCommitted;
  }

  public void reset() {
    throw new UnsupportedOperationException();
  }

  public void setLocale(Locale locale) {
    throw new UnsupportedOperationException();
  }

  public Locale getLocale() {
    throw new UnsupportedOperationException();
  }

  private static class StringServletOutputStream extends ServletOutputStream {

    private final PrintWriter printWriter;

    private StringServletOutputStream(StringWriter stringWriter) {
      this.printWriter = new PrintWriter(stringWriter);
    }

    @Override
    public void write(int i) throws IOException {
      printWriter.write(i);
    }
  }
}