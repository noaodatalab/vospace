package edu.noirlab.datalab.vos;

import ca.nrc.cadc.util.StringBuilderWriter;
import ca.nrc.cadc.vos.VOS.NodeBusyState;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import ca.nrc.cadc.vos.XmlProcessor;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.DataNode;
import ca.nrc.cadc.vos.UnstructuredDataNode;
import ca.nrc.cadc.vos.StructuredDataNode;
import ca.nrc.cadc.vos.LinkNode;
import ca.nrc.cadc.vos.NodeProperty;
/**
 * Writes a Node as XML to an output.
 *
 * @author jburke
 */


public class NodeWriter implements XmlProcessor {
    protected static Namespace xsiNamespace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    protected static Namespace voaNamespace;
    private static Logger log = Logger.getLogger(ca.nrc.cadc.vos.NodeWriter.class);
    private String stylesheetURL;
    protected Namespace vosNamespace;

    public NodeWriter() {
        this("http://www.ivoa.net/xml/VOSpace/v2.0");
    }

    public NodeWriter(String vospaceNamespace) {
        this.stylesheetURL = null;
        this.vosNamespace = Namespace.getNamespace("vos", vospaceNamespace);
        voaNamespace = Namespace.getNamespace("voa", vospaceNamespace);
    }

    public void setStylesheetURL(String stylesheetURL) {
        this.stylesheetURL = stylesheetURL;
    }

    public String getStylesheetURL() {
        return this.stylesheetURL;
    }

    public void write(Node node, OutputStream out) throws IOException {
        OutputStreamWriter outWriter;
        try {
            outWriter = new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException var5) {
            throw new RuntimeException("UTF-8 encoding not supported", var5);
        }

        this.write((Node)node, (Writer)outWriter);
    }

    public void write(Node node, StringBuilder builder) throws IOException {
        this.write((Node)node, (Writer)(new StringBuilderWriter(builder)));
    }

    public void write(Node node, Writer writer) throws IOException {
        long start = System.currentTimeMillis();
        Element root = this.getRootElement(node);
        this.write(root, writer);
        long end = System.currentTimeMillis();
        log.debug("Write elapsed time: " + (end - start) + "ms");
    }

    public void write(Node node, Writer writer, boolean ommitDeclaration) throws IOException {
        long start = System.currentTimeMillis();
        Element root = this.getRootElement(node);
        this.write(root, writer, ommitDeclaration);
        long end = System.currentTimeMillis();
        log.debug("Write elapsed time: " + (end - start) + "ms");
    }

    protected Element getRootElement(Node node) {
        Element root = this.getNodeElement(node);
        root.addNamespaceDeclaration(xsiNamespace);
        return root;
    }

    protected Element getNodeElement(Node node) {
        Element ret = new Element("node", this.vosNamespace);
        ret.addNamespaceDeclaration(voaNamespace);
        ret.setAttribute("type", "vos:" + node.getClass().getSimpleName(), xsiNamespace);
        ret.setAttribute("uri", node.getUri().toString());
        Element props = this.getPropertiesElement(node);
        ret.addContent(props);
        if (node instanceof ContainerNode) {
            ContainerNode cn = (ContainerNode)node;
            ret.addContent(this.getNodesElement(cn));
        } else if (!(node instanceof DataNode) && !(node instanceof UnstructuredDataNode) && !(node instanceof StructuredDataNode)) {
            if (node instanceof LinkNode) {
                LinkNode ln = (LinkNode)node;
                Element targetEl = new Element("target", this.vosNamespace);
                targetEl.setText(ln.getTarget().toString());
                ret.addContent(targetEl);
            }
        } else {
            ret.addContent(this.getAcceptsElement(node));
            ret.addContent(this.getProvidesElement(node));
            ret.addContent(this.getCapabilitiesElement(node));
            DataNode dn = (DataNode)node;
            ret.setAttribute("busy", dn.getBusy().equals(NodeBusyState.notBusy) ? "false" : "true");
        }

        return ret;
    }

    protected Element getPropertiesElement(Node node)
    {
        Element ret = new Element("properties", vosNamespace);
        for (NodeProperty nodeProperty : node.getProperties())
        {
            Element property = new Element("property", vosNamespace);
            if (nodeProperty.isMarkedForDeletion())
                property.setAttribute(new Attribute("nil", "true", xsiNamespace));
            else
                property.setText(nodeProperty.getPropertyValue());
            property.setAttribute("uri", nodeProperty.getPropertyURI());
            property.setAttribute("readOnly", (nodeProperty.isReadOnly() ? "true" : "false"));
            ret.addContent(property);
        }
        return ret;
    }

    protected Element getAcceptsElement(Node node) {
        Element accepts = new Element("accepts", this.vosNamespace);
        Iterator var3 = node.accepts().iterator();

        while(var3.hasNext()) {
            URI viewURI = (URI)var3.next();
            Element viewElement = new Element("view", this.vosNamespace);
            viewElement.setAttribute("uri", viewURI.toString());
            accepts.addContent(viewElement);
        }

        return accepts;
    }

    protected Element getProvidesElement(Node node) {
        Element provides = new Element("provides", this.vosNamespace);
        Iterator var3 = node.provides().iterator();

        while(var3.hasNext()) {
            URI viewURI = (URI)var3.next();
            Element viewElement = new Element("view", this.vosNamespace);
            viewElement.setAttribute("uri", viewURI.toString());
            provides.addContent(viewElement);
        }

        return provides;
    }

    protected Element getCapabilitiesElement(Node node) {
        Element capabilities = new Element("capabilities", this.vosNamespace);
        Iterator var3 = node.capabilities().iterator();

        while(var3.hasNext()) {
            URI viewURI = (URI)var3.next();
            Element viewElement = new Element("capability", this.vosNamespace);
            viewElement.setAttribute("uri", viewURI.toString());
            capabilities.addContent(viewElement);
        }

        return capabilities;
    }

    protected Element getNodesElement(ContainerNode node) {
        Element nodes = new Element("nodes", this.vosNamespace);
        Iterator var3 = node.getNodes().iterator();

        while(var3.hasNext()) {
            Node childNode = (Node)var3.next();
            Element nodeElement = this.getNodeElement(childNode);
            nodes.addContent(nodeElement);
        }

        return nodes;
    }

    protected void write(Element root, Writer writer) throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        outputter.setFormat(Format.getCompactFormat().setOmitDeclaration(true));
        Document document = new Document(root);
        if (this.stylesheetURL != null) {
            Map<String, String> instructionMap = new HashMap(2);
            instructionMap.put("type", "text/xsl");
            instructionMap.put("href", this.stylesheetURL);
            ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", instructionMap);
            document.getContent().add(0, pi);
        }

        outputter.output(document, writer);
    }

    /**
     * Write to root Element to a writer.
     *
     * @param root Root Element to write.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    @SuppressWarnings("unchecked")
    public void write(Element root, Writer writer, boolean omitDeclaration) throws IOException
    {
        XMLOutputter outputter = new XMLOutputter();
        // Our VTD XML generation creates a compact XML,
        // meaning no spaces. So in this borrowed code from
        // cadc I get rid of the pretty format, as it adds
        // a lot of empty spaces.
        // If you want pretty printing for debugging purposes
        // below code will help.
        //Format fmt = Format.getPrettyFormat();
        //fmt.setOmitDeclaration(omitDeclaration);
        //outputter.setFormat(fmt);
        outputter.setFormat(Format.getCompactFormat().setOmitDeclaration(omitDeclaration));
        Document document = new Document(root);
        outputter.output(document, writer);
    }
}
