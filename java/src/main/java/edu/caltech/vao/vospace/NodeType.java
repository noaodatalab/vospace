
package edu.caltech.vao.vospace;

import java.util.HashMap;
import java.util.Map;


/**
 * The possible types of node
 */
public enum NodeType {
    NODE (0, "vos:Node"), 
	DATA_NODE (1, "vos:DataNode"), 
	LINK_NODE (2, "vos:LinkNode"), 
	CONTAINER_NODE (3, "vos:ContainerNode"), 
	UNSTRUCTURED_DATA_NODE (4, "vos:UnstructuredDataNode"), 
	STRUCTURED_DATA_NODE (5, "vos:StructuredDataNode");

    private final Integer id;
    private final String uri;

    private static final Map<String, Integer> MAP = new HashMap<String, Integer>();
    private static final Map<Integer, String> RevMAP = new HashMap<Integer, String>();
    static {
	for (NodeType t : NodeType.values()) {
	    MAP.put(t.uri, t.id);
	    RevMAP.put(t.id, t.uri);
	}
    }

    private NodeType(Integer id, String uri) {
	this.id = id;
	this.uri = uri;
    }

    private Integer getId() { return this.id; }

    private String getUri() { return this.uri; }

    public static Integer getIdByUri(String uri) {
	return MAP.get(uri);
    }

    public static String getUriById(int id) {
	return RevMAP.get(id);
    }
}
