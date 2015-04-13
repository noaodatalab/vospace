
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

}