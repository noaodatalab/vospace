package edu.noirlab.datalab.vos;

import ca.nrc.cadc.vos.NodeWriter;

import java.io.IOException;
import java.io.StringWriter;

public class Node {
    ca.nrc.cadc.vos.Node node;
    public Node(ca.nrc.cadc.vos.Node node) {
        super();
        this.node = node;
    }

    public String toString() {
        System.out.println("Running Node toString");
        NodeWriter nodeWriter = new NodeWriter();
        StringWriter sw = new StringWriter();
        try {
            nodeWriter.write(node, sw, true);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return sw.toString();
    }
}
