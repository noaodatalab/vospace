
package edu.caltech.vao.vospace;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.util.Map;
import java.util.TreeMap;

public final class SyncFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response,
    		FilterChain chain) throws IOException, ServletException {
	Map<String, String[]> extraParams = new TreeMap<String, String[]>();
	extraParams.put("PHASE", new String[] {"RUN"});
	HttpServletRequest newReq = new FilteredRequest((HttpServletRequest) request, extraParams);
	RequestDispatcher dispatch = request.getRequestDispatcher("/vospace/transfers");
	dispatch.forward(newReq, response);
    }

    public void destroy() {
    }

    public void init(FilterConfig filterConfig) {
    }
}