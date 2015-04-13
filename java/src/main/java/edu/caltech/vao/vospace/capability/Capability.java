/**
 * Capability.java
 * Author: Matthew Graham (NOAO)
 * Version: Original (0.1) - 5 January 2015
 */

package edu.caltech.vao.vospace.capability;

import java.util.List;

import edu.caltech.vao.vospace.NodeType;
import edu.caltech.vao.vospace.VOSpaceException;
import edu.caltech.vao.vospace.xml.Param;

/**
 * This interface represents the implementation details of a capability 
 * on a container
 */
public interface Capability {

     /*
      * Return the registered identifier for this capability
      */
     public String getUri();

    /*
     * Return nodal applicability of this capability
     */
    public List<NodeType> getApplicability();

     /*
      * Set the parameters for the capability
      */
     public void setParams(Param[] params);

     
     /*
      * Invoke the capability on the specified container
      */
     public boolean invoke(String identifier) throws VOSpaceException;
     
}
