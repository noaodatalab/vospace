
package edu.caltech.vao.vospace.xml;

import edu.caltech.vao.vospace.VOSpaceException;

public class UnstructuredDataNode extends DataNode {

    /**
     * Construct a Node from the byte array
     * @param req The byte array containing the Node
     */
    public UnstructuredDataNode(byte[] bytes) throws VOSpaceException {
        super(bytes);
    }

}