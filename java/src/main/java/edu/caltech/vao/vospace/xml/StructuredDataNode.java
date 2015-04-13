
package edu.caltech.vao.vospace.xml;

import edu.caltech.vao.vospace.VOSpaceException;

public class StructuredDataNode extends DataNode {

    /**
     * Construct a Node from the byte array
     * @param req The byte array containing the Node
     */
    public StructuredDataNode(byte[] bytes) throws VOSpaceException {
        super(bytes);
    }

}