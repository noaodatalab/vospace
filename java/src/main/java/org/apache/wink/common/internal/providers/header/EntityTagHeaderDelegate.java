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

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.wink.common.internal.i18n.Messages;

public class EntityTagHeaderDelegate implements HeaderDelegate<EntityTag> {

    private static String WEAK = "W/"; //$NON-NLS-1$

    public EntityTag fromString(String eTag) throws IllegalArgumentException {
        if (eTag == null) {
            throw new IllegalArgumentException(Messages.getMessage("variableIsNull", "eTag")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Remove leading and trailing white spaces
        eTag = eTag.trim();

        boolean weak = false;
        if (eTag.startsWith(WEAK)) {
            eTag = eTag.substring(WEAK.length());
            weak = true;
        }

        // Check that e-tag is quoted-string
        if (!eTag.startsWith("\"") || !eTag.endsWith("\"")) { //$NON-NLS-1$ //$NON-NLS-2$
            if ("*".equals(eTag)) { //$NON-NLS-1$
                return new EntityTag("*"); //$NON-NLS-1$
            }
            throw new IllegalArgumentException(Messages.getMessage("entityTagNotQuoted", eTag)); //$NON-NLS-1$
        }

        // Remove quotes
        eTag = eTag.substring(1, eTag.length() - 1);

        // Un-escape the e-tag
        StringBuilder builder = null;

        for (int i = 0; i < eTag.length(); i++) {

            if (eTag.charAt(i) == '\\' && i + 1 < eTag.length()) {

                // each '\' (which is not the last one) escapes the next
                // character
                if (builder == null) {
                    builder = new StringBuilder(eTag.length());
                    builder.append(eTag, 0, i);
                }
                // don't append the '\'

            } else {
                // append the character

                if (builder != null) {
                    builder.append(eTag.charAt(i));
                }
            }
        }

        if (builder != null) {
            eTag = builder.toString();
        }

        return new EntityTag(eTag, weak);
    }

    public String toString(EntityTag eTag) {

        if (eTag == null) {
            throw new IllegalArgumentException(Messages.getMessage("variableIsNull", "eTag")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        StringBuilder builder = new StringBuilder();
        if (eTag.isWeak()) {
            builder.append("W/"); //$NON-NLS-1$
        }
        builder.append('"');
        String value = eTag.getValue();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"')
                builder.append('\\');
            builder.append(c);
        }
        builder.append('"');
        return builder.toString();
    }
}
