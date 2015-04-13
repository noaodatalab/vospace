
package edu.caltech.vao.vospace.xml;

import edu.caltech.vao.vospace.VOSpaceException;

public class ContainerNode extends DataNode {

    /**
     * Construct a Node from the byte array
     * @param req The byte array containing the Node
     */
    public ContainerNode(byte[] bytes) throws VOSpaceException {
        super(bytes);
    }

    /*
     * Add a child node to the container's list
     * @param uri The identifier of the child node
     * @param type The type of the child node
     */
    public void addNode(String identifier, String type) throws VOSpaceException {
	if (!has("/vos:node/vos:nodes")) {
	    add("/vos:node/vos:provides", PREFIX == null ? "<nodes></nodes>" : "<" + PREFIX + ":nodes></" + PREFIX + ":nodes>");
	} else if (!has("/vos:node/vos:nodes/vos:node")) {
	    remove("/vos:node/vos:nodes");
	    add("/vos:node/vos:provides", PREFIX == null ? "<nodes></nodes>" : "<" + PREFIX + ":nodes></" + PREFIX + ":nodes>");
	}
	if (identifier != null)
	    addChild("/vos:node/vos:nodes", PREFIX == null ? "<node uri=\"" + identifier + "\" xsi:type=\"" + type + "\"/>" : "<" + PREFIX + ":node uri=\"" + identifier + "\" xsi:type=\"" + type + "\"/>");
    }

    /*
     * Add a child node to the container's list
     * @param child The string representation of the child node
     */
    public void addNode(String child) throws VOSpaceException {
	if (!has("/vos:node/vos:nodes")) {
	    add("/vos:node/vos:provides", PREFIX == null ? "<nodes></nodes>" : "<" + PREFIX + ":nodes></" + PREFIX + ":nodes>");
	} else if (!has("/vos:node/vos:nodes/vos:node")) {
	    remove("/vos:node/vos:nodes");
	    add("/vos:node/vos:provides", PREFIX == null ? "<nodes></nodes>" : "<" + PREFIX + ":nodes></" + PREFIX + ":nodes>");
	}
	if (child != null)
	    addChild("/vos:node/vos:nodes", child);
    }
}
