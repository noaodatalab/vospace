package edu.caltech.vao.vospace.xml;

import edu.caltech.vao.vospace.VOSpaceException;

public class Param  {

    private XMLObject param;

    /**
     * Construct a Param from the byte array
     * @param bytes The byte array containing the Param
     */
    public Param(byte[] bytes) throws VOSpaceException {
	param = new XMLObject(bytes);
    }

    /**
     * Construct a Param from the string representation
     * @param bytes The string containing the param
     */
    public Param(String bytes) throws VOSpaceException {
	param = new XMLObject(bytes.getBytes());
    }

   /**
     * Get the URI of the param
     * @return The URI of the param
     */
    public String getURI() throws VOSpaceException {
	return param.xpath("/vos:param/@uri")[0];
    }

   /**
     * Get the value of the param
     * @return The value of the param
     */
    public String getValue() throws VOSpaceException {
	return param.xpath("/vos:param")[0];
    }

}