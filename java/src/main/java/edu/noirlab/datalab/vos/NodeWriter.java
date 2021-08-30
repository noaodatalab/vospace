package edu.noirlab.datalab.vos;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;

public class NodeWriter extends ca.nrc.cadc.vos.NodeWriter {

    public NodeWriter() {
       super(VOSPACE_NS_20);
    }
    public NodeWriter(String vospaceNamespace) {
        super(vospaceNamespace);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void write(Element root, Writer writer, boolean omitDeclaration) throws IOException
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
