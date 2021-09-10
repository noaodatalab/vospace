package edu.noirlab.datalab.xml;

import ca.nrc.cadc.xml.XmlUtil;
import edu.caltech.vao.vospace.VOSpaceException;
import edu.caltech.vao.vospace.VOSpaceManager;
import edu.caltech.vao.vospace.xml.Param;
import org.apache.log4j.Logger;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static edu.noirlab.datalab.vos.Utils.log_error;


public class Protocol {
    private static Logger logger = Logger.getLogger(VOSpaceManager.class.getName());

    private org.jdom2.Document protocol;
    private static XPathFactory xpathFactory = XPathFactory.instance();

    private static String xpathUri = "/vos:protocol/@uri";
    private static XPathExpression exprUri = xpathFactory.compile(xpathUri,
            Filters.fpassthrough() ,null,
            Namespace.getNamespace("vos",
                    "http://www.ivoa.net/xml/VOSpace/v2.0"));

    private static String xpathEndpoint = "/vos:protocol/vos:endpoint";
    private static XPathExpression exprEndpoint = xpathFactory.compile(xpathEndpoint,
            Filters.fpassthrough() ,null,
            Namespace.getNamespace("vos", "http://www.ivoa.net/xml/VOSpace/v2.0"));

    private static XPathExpression exprEndpointElem = xpathFactory.compile(xpathEndpoint,
            Filters.element() ,null,
            Namespace.getNamespace("vos",
                    "http://www.ivoa.net/xml/VOSpace/v2.0"));

    private static String xpathProtocol = "/vos:protocol";
    private static XPathExpression exprProtocalElem = xpathFactory.compile(xpathProtocol,
            Filters.element() ,null,
            Namespace.getNamespace("vos",
                    "http://www.ivoa.net/xml/VOSpace/v2.0"));

    private static String xpathProtocolParam = "/vos:protocol/vos:param";
    private static XPathExpression exprProtocolParam = xpathFactory.compile(xpathProtocolParam,
            Filters.fpassthrough() ,null,
            Namespace.getNamespace("vos", "http://www.ivoa.net/xml/VOSpace/v2.0"));
    /**
     * Construct an empty Protocol
     */
    public Protocol() throws VOSpaceException {
        String blank = "<vos:protocol uri=\"\" xmlns:vos=\"http://www.ivoa.net/xml/VOSpace/v2.0\"><vos:endpoint></vos:endpoint></vos:protocol>";
        try {
            protocol = XmlUtil.buildDocument(new StringReader(blank), null);
        } catch (IOException e) {
            log_error(logger, e);
        } catch (JDOMException e) {
            log_error(logger, e);
        }
    }

    /**
     * Construct a Protocol from the string representation
     * @param xml The string containing the Protocol
     */
    public Protocol(String xml) throws VOSpaceException {
        String blank = "<vos:protocol uri=\"\" xmlns:vos=\"http://www.ivoa.net/xml/VOSpace/v2.0\"><vos:endpoint></vos:endpoint></vos:protocol>";
        try {
            String protocolXML = xml == null? blank: xml;
            protocol = XmlUtil.buildDocument(new StringReader(protocolXML), null);
        } catch (IOException e) {
            log_error(logger, e);
            throw new VOSpaceException(e);
        } catch (JDOMException e) {
            log_error(logger, e);
            throw new VOSpaceException(e);
        }
    }

    private static final Element extractElement(Object source) {
        if (source instanceof Element) {
            return (Element) source;
        }
        return null;
    }

    private static final String extractValue(Object source) {
        if (source instanceof Attribute) {
            return ((Attribute)source).getValue();
        }
        if (source instanceof Content) {
            return ((Content)source).getValue();
        }
        return String.valueOf(source);
    }

    /**
     * Get the endpoint of the protocol
     * @return The endpoint of the protocol
     */
    public String getEndpoint() throws VOSpaceException {
        List<String> values = (List<String>) exprEndpoint.evaluate(protocol).stream().
                map(o -> extractValue(o)).collect(Collectors.toList());
        if (values.size() > 0 ) {
            String str_expr = values.get(0);
            return str_expr;
        } else {
            return null;
        }
    }

    /**
     * Get the URI of the protocol
     * @return The URI of the protocol
     */
    public String getURI() throws VOSpaceException {
        if (protocol != null) {
            List<String> values = (List<String>) exprUri.evaluate(protocol).stream().
                    map(o -> extractValue(o)).collect(Collectors.toList());
            String str_expr = values.get(0);
            return str_expr;
        } else {
            return null;
        }
    }

    /**
     * Get the params of the protocol
     * @return The params of the protocol
     */
    public Param[] getParam() throws VOSpaceException {
        List<String> values = (List<String>) exprProtocolParam.evaluate(protocol).stream().
                map(o -> extractValue(o)).collect(Collectors.toList());
        ArrayList<Param> params = new ArrayList<Param>();
        for(String param:values) {
            params.add(new Param(param));
        }
        return params.toArray(new Param[0]);
    }

    /**
     * Set the endpoint of the protocol
     * @param endpoint The endpoint of the protocol
     */
    public void setEndpoint(String endpoint) throws VOSpaceException {
        List<Element> values = (List<Element>) exprEndpointElem.evaluate(protocol).stream().
                map(o -> extractElement(o)).collect(Collectors.toList());
        if (values.size() > 0 ) {
            Element endpointElem = values.get(0);
            endpointElem.setText(endpoint);
            //String endpointElemString   = new XMLOutputter().outputString(endpointElem);
            //System.out.println("endpointElemString:[" + endpointElemString + "]");
        } else {
            Namespace parentNamespace = protocol.getRootElement().getNamespace();
            protocol.getRootElement().addContent(new Element("endpoint", parentNamespace ).
                    setContent( new Text(endpoint)));
            //String endpointElemString   = new XMLOutputter().outputString(docProtocol);
            //System.out.println("docProtocol XML:[" + endpointElemString + "]");
        }
    }

    /**
     * Set the URI of the protocol
     * @return The URI of the protocol
     */
    public void setURI(String uri) throws VOSpaceException {
        List<Element> values = (List<Element>) exprProtocalElem.evaluate(protocol).stream().
                map(o -> extractElement(o)).collect(Collectors.toList());
        Element protocol = values.get(0);
        protocol.setAttribute("uri", uri);
    }

    /**
     * Get a string representation of the protocol
     * @return a string representation of the protocol
     */
    public String toString() {
        String output = new XMLOutputter().outputString(protocol);
        // When JDOM2 serializes an object, it adds the xml version
        // to it in the first line. However the VTD chokes on that,
        // so we make sure that line is removed before we return.
        // This is used in the addProtocol method in Transfer.java
        // where we take the jdom2 xml and added it into a VTD XMLObject.
        return output.substring(output.indexOf('\n')+1);
    }
}