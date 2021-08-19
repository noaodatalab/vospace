package edu.noirlab.datalab.vos;

import ca.nrc.cadc.vos.*;
import ca.nrc.cadc.vos.Node;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

public class VTDXMLNodeWriter extends edu.noirlab.datalab.vos.NodeWriter {
    protected static Namespace voaNamespace;
    protected static Namespace vosNamespace;

    public VTDXMLNodeWriter() {
        this(VOSPACE_NS_20);
    }
    public VTDXMLNodeWriter(String vospaceNamespace) {
        super(vospaceNamespace);
        this.vosNamespace = Namespace.getNamespace("vos", vospaceNamespace);
        this.voaNamespace = Namespace.getNamespace("voa", vospaceNamespace);
    }

    @Override
    protected Element getNodeElement(Node node)
    {
        Element ret = new Element("node", vosNamespace);
        ret.addNamespaceDeclaration(voaNamespace);
        ret.setAttribute("type", "vos" + ":" + node.getClass().getSimpleName(), xsiNamespace);
        ret.setAttribute("uri", node.getUri().toString());
        //ret.setAttribute("type", vosNamespace.getPrefix() + ":" + node.getClass().getSimpleName(), xsiNamespace);

        Element props = getPropertiesElement(node);
        ret.addContent(props);

        if (node instanceof ContainerNode)
        {
            ContainerNode cn = (ContainerNode) node;
            ret.addContent(getNodesElement(cn));
        }
        else if ((node instanceof DataNode) ||
                (node instanceof UnstructuredDataNode) ||
                (node instanceof StructuredDataNode))
        {
            ret.addContent(getAcceptsElement(node));
            ret.addContent(getProvidesElement(node));
            ret.addContent(getCapabilitiesElement(node));
            //DataNode dn = (DataNode) node;
            //ret.setAttribute("busy", (dn.getBusy().equals(NodeBusyState.notBusy) ? "false" : "true"));
        }
        else if (node instanceof LinkNode)
        {
            LinkNode ln = (LinkNode) node;
            Element targetEl = new Element("target", vosNamespace);
            targetEl.setText(ln.getTarget().toString());
            ret.addContent(targetEl);
        }
        // always set to false at datalab vospace
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
