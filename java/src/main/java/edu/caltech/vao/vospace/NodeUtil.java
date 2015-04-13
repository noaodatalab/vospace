
package edu.caltech.vao.vospace;

import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import edu.caltech.vao.vospace.meta.MetaStore;

public class NodeUtil {

    private final Pattern VOS_PATTERN;
    private MetaStore store;

    public NodeUtil(MetaStore store) {
	VOS_PATTERN = Pattern.compile("vos://[\\w\\d][\\w\\d\\-_\\.!~\\*'\\(\\)\\+=]{2,}(![\\w\\d\\-_\\.!~\\*'\\(\\)\\+=]+(/[\\w\\d\\-_\\.!~\\*'\\(\\)\\+=]+)*)+");
	this.store = store;
    }

    /*
     * Check whether the specified identifier is valid
     * @param id The identifier to check
     * @return whether the identifier is valid or not
     */
    public boolean validId(String id) {
	Matcher m = VOS_PATTERN.matcher(id);
	boolean reserved = true;
	if (id.endsWith(".auto") || id.endsWith(".null")) {
     	    if (!id.substring(id.length() - 6, id.length() - 5).matches("/")) reserved = false;
	}
	boolean result =  (m.matches() && reserved);
	return result;
    }

    /*
     * Check whether the parent node of the specified identifier is valid:
     *   - it exists
     *   - it is a container
     * @param id The identifier to check
     * @return whether the parent node is valid or not
     */
    public boolean validParent(String id) throws VOSpaceException {
	try {
	    String parent = id.substring(0, id.lastIndexOf("/"));
	    if (store.getType(parent) != NodeType.CONTAINER_NODE.ordinal()) return false;
	    return true;
	} catch (SQLException e) {
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }
}