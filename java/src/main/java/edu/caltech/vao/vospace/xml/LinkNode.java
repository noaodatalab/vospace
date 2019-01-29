
package edu.caltech.vao.vospace.xml;

import edu.caltech.vao.vospace.VOSpaceException;

public class LinkNode extends Node {

    /**
     * Construct a Node from the byte array
     * @param req The byte array containing the Node
     */
    public LinkNode(byte[] bytes) throws VOSpaceException {
        super(bytes);
    }

    /**
     * Get the target of the node
     * @return The target of the node
     */
    public String getTarget() throws VOSpaceException {
        // System.err.println(Arrays.toString(get("/vos:node/vos:target")));
        String[] targetURIs = get("/vos:node/vos:target");
        if (targetURIs.length == 0) { return ""; } else { return targetURIs[0]; }
    }


    /**
     * Set the target for the node.
     * @param value The value of the <target> element
     */
    public void setTarget(String value) throws VOSpaceException {
        if (value != null)
            if (has("/vos:node/vos:target")) { remove("/vos:node/vos:target"); }
            addChild("/vos:node", PREFIX == null ? "<target>" + value + "</target>" : "<" + PREFIX + ":target>" + value + "</target>");
    }
}