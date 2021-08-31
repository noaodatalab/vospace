package edu.noirlab.datalab.vos;

import ca.nrc.cadc.vos.*;
import ca.nrc.cadc.vos.Node;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

public class VTDXMLNodeWriter extends edu.noirlab.datalab.vos.NodeWriter {
    protected static Namespace voaNamespace;
    //protected static Namespace vosNamespace;

    public VTDXMLNodeWriter() {
        this(VOSPACE_NS_20);
    }
    public VTDXMLNodeWriter(String vospaceNamespace) {
        //super(vospaceNamespace);
        this.vosNamespace = Namespace.getNamespace(null, vospaceNamespace);
        //this.vosNamespace = Namespace.getNamespace("vos", vospaceNamespace);
        this.voaNamespace = Namespace.getNamespace("voa", vospaceNamespace);
    }


    /**
     *  Build the root Element of a Node.
     *
     * @param node Node.
     * @return root Element.
     */
    protected Element getRootElement(Node node)
    {
        // Create the root element (node).
        Element root = getNodeElement(node);
        //root.addNamespaceDeclaration(vosNamespace);
        root.addNamespaceDeclaration(xsiNamespace);
        return root;
    }

    @Override
    protected Element getNodeElement(Node node) {
        Element ret = new Element("node", this.vosNamespace);
        //ret.addNamespaceDeclaration(voaNamespace);
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
        }

        if (node.accepts() != null && node.accepts().size() > 0) {
            ret.addContent(this.getAcceptsElement(node));
        }
        if (node.provides() != null && node.provides().size() > 0) {
            ret.addContent(this.getProvidesElement(node));
        }
        if (node.capabilities() != null && node.capabilities().size() > 0) {
            ret.addContent(this.getCapabilitiesElement(node));
        }

        ret.setAttribute("busy", "false");

        return ret;
    }

    @Override
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
            // Our VTD doesn't like the "readOnly" attribute
            //property.setAttribute("readOnly", (nodeProperty.isReadOnly() ? "true" : "false"));
            ret.addContent(property);
        }
        return ret;
    }
}
