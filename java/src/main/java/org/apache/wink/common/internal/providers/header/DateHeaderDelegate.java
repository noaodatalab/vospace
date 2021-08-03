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
package org.apache.wink.common.internal.providers.header;

import java.util.Date;

import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.wink.common.internal.i18n.Messages;
import org.apache.wink.common.internal.utils.HttpDateParser;

/**
 * Date HeaderDelegate, responsible to Serialize and De-serialize Date headers. <br>
 */
public class DateHeaderDelegate implements HeaderDelegate<Date> {

    public Date fromString(String date) throws IllegalArgumentException {

        if (date == null) {
            throw new IllegalArgumentException(Messages.getMessage("variableIsNull", "date")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return HttpDateParser.parseHttpDate(date);
    }

    /**
     * Format input date using HTTP preferred Date Format (RFC1123)
     */
    public String toString(Date date) {

        if (date == null) {
            throw new IllegalArgumentException(Messages.getMessage("variableIsNull", "date")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return HttpDateParser.toHttpDate(date);
    }
}
