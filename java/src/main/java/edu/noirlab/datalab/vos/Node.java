package edu.noirlab.datalab.vos;

import java.io.IOException;
import java.io.StringWriter;
import org.apache.log4j.Logger;

public class Node {
    private static Logger log = Logger.getLogger(edu.noirlab.datalab.vos.Node.class);
    ca.nrc.cadc.vos.Node node;
    public Node(ca.nrc.cadc.vos.Node node) {
        this.node = node;
    }

    public String toString() {
        // use the VTDXML version of the writer so the new XML
        // looks as similar as possible as the VOSpace 2.0 xml
        //NodeWriter nodeWriter = NodeWriter();
        NodeWriter nodeWriter = new VTDXMLNodeWriter();
        StringWriter sw = new StringWriter();
        try {
            nodeWriter.write(node, sw, true);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            e.printStackTrace();
            return "";
        }
        return sw.toString();
    }
}
