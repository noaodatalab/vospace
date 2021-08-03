/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *  
 *******************************************************************************/

package org.apache.wink.common.internal.providers.multipart;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.model.multipart.OutMultiPart;

@Provider
@Produces("multipart/*")

public class OutMultiPartProvider implements MessageBodyWriter<OutMultiPart> {
	@Context
	Providers providers;
	
	public long getSize(OutMultiPart t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return OutMultiPart.class.isAssignableFrom(type);
	}

	public void writeTo(OutMultiPart multiPart, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {

		// Sync between the boundary of the MediaType (if exist) and the one 
		// in the MultiPart object. priority to the one on the MediaType  
		String boundary = mediaType.getParameters().get("boundary"); //$NON-NLS-1$
	    if (boundary != null){
	        multiPart.setBoundary(boundary);
	    }else{
			httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, mediaType.toString() + "; boundary=" + multiPart.getBoundary()); //$NON-NLS-1$
	    }
	    
		multiPart.write(entityStream,providers);		
	}

	



}
