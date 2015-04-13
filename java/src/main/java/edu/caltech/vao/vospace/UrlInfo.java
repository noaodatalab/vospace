
package edu.caltech.vao.vospace;

public class UrlInfo {

  private final String baseUrl;
  private final String contextPath;
  private final String pathInfo;

  public UrlInfo(String baseUrl, String contextPath, String pathInfo) {
    this.baseUrl = baseUrl;
    this.contextPath = contextPath;
    this.pathInfo = pathInfo;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getServletPath() {
    return "";
  }

  public String getContextPath() {
    return contextPath;
  }

  public String getPathInfo() {
    return pathInfo;
  }

  @Override
  public String toString() {
    return baseUrl + contextPath + pathInfo;
  }
}
