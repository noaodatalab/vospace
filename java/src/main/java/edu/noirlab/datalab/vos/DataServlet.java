package edu.noirlab.datalab.vos;


import ca.nrc.cadc.util.Enumerator;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import javax.security.auth.Subject;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.nrc.cadc.vos.VOS;
import org.apache.catalina.WebResource;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.catalina.servlets.Constants;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.webresources.CachedResource;
import org.apache.log4j.Logger;
import org.apache.naming.StringManager;

/**
 * Very simple RESTful servlet that loads a separate RestAction subclass for each
 * supported HTTP action: get, post, put, delete.
 *
 * @author pdowler
 */
public class DataServlet extends DefaultServlet {
    private static final long serialVersionUID = 201211071520L;

    private static final Logger log = Logger.getLogger(DataServlet.class);

    protected static final StringManager sm = StringManager.getManager(DefaultServlet.class);
    private static final List<String> CITEMS = new ArrayList<String>();

    static {
        CITEMS.add("init");
        CITEMS.add("head");
        CITEMS.add("get");
        CITEMS.add("post");
        CITEMS.add("put");
        CITEMS.add("delete");
    }

    private final Map<String,String> initParams = new TreeMap<String,String>();

    protected String appName;
    protected String componentID;
    protected boolean augmentSubject = true;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String get = loadAction(config, "get");
        String post = loadAction(config, "post");
        String put = loadAction(config, "put");
        String delete = loadAction(config, "delete");
        String head = loadAction(config, "head");

        this.appName = config.getServletContext().getServletContextName();
        this.componentID = appName  + "." + config.getServletName();
        String augment = config.getInitParameter("augmentSubject");
        if (augment != null && augment.equalsIgnoreCase(Boolean.FALSE.toString())) {
            augmentSubject = false;
        }

        // application specific config
        for (String name : new Enumerator<String>(config.getInitParameterNames())) {
            if (!CITEMS.contains(name)) {
                initParams.put(name, config.getInitParameter(name));
            }
        }
    }

    private String loadAction(ServletConfig config, String method) {
        String cname = config.getInitParameter(method);
        return cname;
    }

    /**
     * The default error response when a RestAction is not configured for a requested
     * HTTP action is status code 400 and text/plain error message.
     *
     * @param action action label
     * @param response servlet response object
     * @throws IOException failure to write output
     */
    protected void handleUnsupportedAction(String action, HttpServletResponse response)
            throws IOException {
        response.setStatus(400);
        response.setHeader("Content-Type", "text/plain");
        PrintWriter w = response.getWriter();
        w.println("unsupported: HTTP " + action);
        w.flush();
    }

    private static boolean isText(String contentType) {
        return contentType == null || contentType.startsWith("text") || contentType.endsWith("xml") || contentType.contains("/javascript");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        boolean serveContent = true;
        log.debug("doGet: " + request.getPathInfo());
        String path = this.getRelativePath(request, true);
        path = "/net/mss1/archive/hlsp/decals/dr8/south/tractor/275/tractor-2759p330.fits";
        WebResource resource = this.resources.getResource(path);
        boolean isError = DispatcherType.ERROR == request.getDispatcherType();
        String requestUri;

        boolean included = false;

        String contentType = resource.getMimeType();
        if (contentType == null) {
            contentType = this.getServletContext().getMimeType(resource.getName());
            resource.setMimeType(contentType);
        }

        String eTag = null;
        String lastModifiedHttp = null;
        if (resource.isFile() && !isError) {
            eTag = this.generateETag(resource);
            lastModifiedHttp = resource.getLastModifiedHttp();
        }

        boolean usingPrecompressedVersion = false;
        boolean outputEncodingSpecified = false;

        ArrayList<DefaultServlet.Range> ranges = FULL;
        FileSystem fs = FileSystems.getDefault();
        Path source = fs.getPath(path);
        Files.exists(source);
        int contentLength = -1;
        if (!Files.exists(source)) {
            response.setStatus(404);
            return;
        } else {
            if (!isError) {
                if (this.useAcceptRanges) {
                    response.setHeader("Accept-Ranges", "bytes");
                }

                ranges = this.parseRange(request, response, resource);
                if (ranges == null) {
                    return;
                }

                response.setHeader("ETag", eTag);
                response.setHeader("Last-Modified", lastModifiedHttp);
            }

            contentLength = (int) resource.getContentLength();
            if (contentLength == 0L) {
                serveContent = false;
            }
        }

        ServletOutputStream ostream = null;
        PrintWriter writer = null;
        if (serveContent) {
            try {
                ostream = response.getOutputStream();
            } catch (IllegalStateException var34) {
                if (usingPrecompressedVersion || !isText(contentType)) {
                    throw var34;
                }

                writer = response.getWriter();
                ranges = FULL;
            }
        }

        ServletResponse r = response;

        long contentWritten;
        for (contentWritten = 0L; r instanceof ServletResponseWrapper; r = ((ServletResponseWrapper) r).getResponse()) {
        }

        if (r instanceof ResponseFacade) {
            contentWritten = ((ResponseFacade) r).getContentWritten();
        }

        if (contentWritten > 0L) {
            ranges = FULL;
        }

        String outputEncoding = response.getCharacterEncoding();
        boolean conversionRequired;
        conversionRequired = false;

        /* Partial content */
        if (!Files.isReadable(source)) {
           response.setStatus(403);
           return;
        }

        if (Files.isReadable(source) && !isError && ranges != FULL) {
            if (ranges == null || ranges.isEmpty()) {
                return;
            }

            response.setStatus(206); // Partial status
            response.setHeader("Content-Disposition", "inline; filename=" + path);
            //response.setHeader("Content-Type", node.getPropertyValue(VOS.PROPERTY_URI_TYPE));
            //response.setHeader("Content-Encoding", contentEncoding);
            //response.setHeader("Content-Length", contentLength);
            //response.setHeader("Content-MD5", contentMD5);

            OutputStream out = response.getOutputStream();

            if (ranges.size() == 1) {
                DefaultServlet.Range range = (DefaultServlet.Range) ranges.get(0);
                response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
                int length = (int) range.end - (int) range.start + 1;
                response.setContentLength(length);
                try {
                    response.setBufferSize(this.output);
                } catch (IllegalStateException var32) {
                }

                if (ostream == null) {
                    throw new IllegalStateException();
                }

                    /*
                    if (!this.checkSendfile(request, response, resource, range.end - range.start + 1L, range)) {
                        this.copy(resource, ostream, range);
                    }
                    */
                InputStream is = new FileInputStream(source.toFile());
                this.copy(is, ostream, range);
            } else {
                //TODO multi-ranges error not supported
                /*
                response.setContentType("multipart/byteranges; boundary=CATALINA_MIME_BOUNDARY");
                if (serveContent) {
                    try {
                        response.setBufferSize(this.output);
                    } catch (IllegalStateException var31) {
                    }

                    if (ostream == null) {
                        throw new IllegalStateException();
                    }

                    this.copy(resource, ostream, ranges.iterator(), contentType);
                }
                 */
            }
        } else {
            if (contentType != null) {
                if (this.debug > 0) {
                    this.log("DefaultServlet.serveFile:  contentType='" + contentType + "'");
                }

                if (response.getContentType() == null) {
                    response.setContentType(contentType);
                }
            }
            response.setContentLength(contentLength);

            try {
                response.setBufferSize(this.output);
            } catch (IllegalStateException var33) {
            }

            InputStream renderResult = null;
            if (ostream == null) {
                renderResult = new FileInputStream(source.toFile());
                this.copy((InputStream) renderResult, ostream);
            }

        }
        //super.doGet(request, response);
    }

    protected void copy(InputStream resource, ServletOutputStream ostream, DefaultServlet.Range range) throws IOException {
        IOException exception = null;
        InputStream resourceInputStream = resource;
        InputStream istream = new BufferedInputStream(resourceInputStream, this.input);
        exception = this.copyRange(istream, ostream, range.start, range.end);
        istream.close();
        if (exception != null) {
            throw exception;
        }
    }

    protected void copy(WebResource resource, ServletOutputStream ostream, DefaultServlet.Range range) throws IOException {
        IOException exception = null;
        InputStream resourceInputStream = resource.getInputStream();
        InputStream istream = new BufferedInputStream(resourceInputStream, this.input);
        exception = this.copyRange(istream, ostream, range.start, range.end);
        istream.close();
        if (exception != null) {
            throw exception;
        }
    }
}
