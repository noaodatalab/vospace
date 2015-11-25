/**
 * CapRunner.java
 * Author: Matthew Graham (NOAO)
 * Version: Original (0.1) - 24 November 2015
 */

package edu.caltech.vao.vospace.capability;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import edu.caltech.vao.vospace.NodeType;
import edu.caltech.vao.vospace.VOSpaceException;
import edu.caltech.vao.vospace.xml.Param;

/**
 * This interface represents the implementation details of a capability 
 * on a container which runs an arbitrary command string 
 */
public class CapRunner implements Capability {

    private int port = 0;

    private static final NodeType[] domain = new NodeType[] {NodeType.CONTAINER_NODE};
    
    /*
     * Return the registered identifier for this capability
     */
    public String getUri() {
	return "ivo://datalab.noao.edu/vospace/capabilities#runner";
    }


    /*
     * Return nodal applicability of this capability
     */
    public List<NodeType> getApplicability() {
	return Arrays.asList(domain);
    }

    
    /*
     * Set the parameters for the capability
     */
    public void setParams(Map<String, String> params) throws VOSpaceException {
	for (String par: params.keySet()) {
	    if (par.equals("port")) {
		port = Integer.parseInt(params.get(par));
	    }
	}
    }

     
    /*
     * Invoke the capability of the parent container on the specified
     * location
     */
    public boolean invoke(String location) throws VOSpaceException {
	boolean success = false;
	try {
	    CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost post = new HttpPost("http://localhost:" + port + "/notify");
	    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	    nvps.add(new BasicNameValuePair("name", location));
	    post.setEntity(new UrlEncodedFormEntity(nvps));
	    CloseableHttpResponse response = client.execute(post);
	    System.err.println(response.getStatusLine() + " " + location + " " + port);
	    success = true;
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
	}
	return success;
    }

    
    /*
     * Parse the configuration file
     */
    private Properties parseConfig(String configFile) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(configFile.substring(7)));
	return props;
    }
     
}
