/*******************************************************************************
 * Copyright (c) 2011, Johns Hopkins University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Johns Hopkins University nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Johns Hopkins University BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package edu.jhu.pha.vospace.api;

import java.util.StringTokenizer;
import java.util.Vector;

public class PathSeparator {
	/**
	 * Splits the path to container, path inside the container starting with no "/" and ending with "/", and optional filename.
	 * @param path The path to process
	 * @param extractFilename Include the filename to the path or return it separately
	 * @return
	 */
	public static NodePath splitPath(String path, boolean extractFilename) {
		NodePath res = new NodePath();
		if(null == path || path.length() == 0){
			//throw new BadRequestException("The path provided is incorrect. "+path);
			return res;
		}
		StringTokenizer tok = new StringTokenizer(path, "/");
		res.setContainerName(tok.nextToken()); // container
		
		if(!tok.hasMoreTokens()){
			//throw new BadRequestException("The path provided is incorrect. "+path);
			return res;
		}
		
		while(tok.hasMoreTokens()) {
			String nextTok = tok.nextToken();
			if(extractFilename){
				if(tok.hasMoreTokens() || path.endsWith("/"))
					res.setNodePath(((null == res.getNodePath())?nextTok:res.getNodePath()+nextTok)+"/"); //still dirs
				else
					res.setNodeName(nextTok); // filename
			} else {
				res.setNodePath((null == res.getNodePath())?nextTok:res.getNodePath()+nextTok);
				if(tok.hasMoreTokens())
					res.setNodePath(res.getNodePath() + "/");
			}
		}
		
		return res;
	}

	
	/**
	 * Stores the information about a node path recieved by the REST service.
	 * @author Dmitry Mishin
	 */
	public static class NodePath {
		private String containerName;
		private String nodePath;
		private String nodeName;
		public String getContainerName() {
			return containerName;
		}
		public void setContainerName(String containerName) {
			this.containerName = containerName;
		}
		public String getNodePath() {
			return nodePath;
		}
		public void setNodePath(String nodePath) {
			this.nodePath = nodePath;
		}
		public String getNodeName() {
			return nodeName;
		}
		public void setNodeName(String nodeName) {
			this.nodeName = nodeName;
		}
	}
	
	public static final void main(String[] s) {
		Vector<String> strs = new Vector();
		strs.add("");
		strs.add("/");
		strs.add("cont1");
		strs.add("/cont1");
		strs.add("/cont1/");
		strs.add("/cont1/file1");
		strs.add("cont1/file1");
		strs.add("cont1/dir1/");
		strs.add("cont1/dir1/file1");
		strs.add("cont1/dir1/dir2/file1");
		strs.add("cont1/dir1/dir2/dir3/");
		
		for(String sss: strs){
			try {
				NodePath sres1 = splitPath(sss,false);
				NodePath sres2 = splitPath(sss,true);
				System.out.println(sss+": ("+sres1.containerName+" "+sres1.nodePath+"); ("+sres2.containerName+" "+sres2.nodePath+" "+sres2.nodeName+")");
			} catch(Exception ex) {
				System.out.println(ex.getMessage());
				//ex.printStackTrace();
			}
		}
	}
}
