/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 4 $
*
************************************************************************
*/

package ca.nrc.cadc.vos;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Data structure for the VOSpace transfer job description.
 *
 * @author pdowler
 */
public class Transfer
{
    private static Logger log = Logger.getLogger(Transfer.class);

    /**
     * Keep track of the vospace version since some things differ. This field
     * is here to facilitate reading and writing the same version of documents
     * on the server side in order to maintain support for older clients.
     */
    public int version = VOS.VOSPACE_20;

    private URI target;
    private Direction direction;
    private View view;
    private Long contentLength;
    private String remoteIP;
    private List<Protocol> protocols;
    private boolean keepBytes = true;
    private boolean quickTransfer = false;

    protected Transfer() { }

    /**
     * Constructor for copying a node.
     * @param target
     * @param direction
     * @param protocols
     */
    public Transfer(URI target, Direction direction, List<Protocol> protocols)
    {
        this.target = target;
        this.direction = direction;
        this.protocols = protocols;
        if (this.protocols == null)
            this.protocols = new ArrayList<Protocol>();
    }

    /**
     * Constructor for internal vospace move and copy operations.
     *
     * @param target
     * @param destination
     * @param keepBytes true for copy, false for move
     */
    public Transfer(URI target, URI destination, boolean keepBytes)
    {
        this.target = target;
        this.direction = new Direction(destination.toASCIIString());
        this.keepBytes = keepBytes;
        this.protocols = new ArrayList<Protocol>();
    }

    /**
     * Constructor for copying a node with a specific view.
     *
     * @param target
     * @param direction
     * @param view
     * @param protocols
     */
    public Transfer(URI target, Direction direction, View view, List<Protocol> protocols)
    {
        this.target = target;
        this.direction = direction;
        this.view = view;
        this.protocols = protocols;
        if (this.protocols == null)
            this.protocols = new ArrayList<Protocol>();
    }

    // complete ctor for use by TransferReader
    Transfer(URI target, Direction direction, View view,
            List<Protocol> protocols, boolean keepBytes)
    {
        this.target = target;
        this.direction = direction;
        this.view = view;
        this.protocols = protocols;
        this.keepBytes = keepBytes;
        if (this.protocols == null)
            this.protocols = new ArrayList<Protocol>();
    }

    /**
     * Transfer uses the CADC quick transfer feature
     * @return True if client uses CADC quick copy feature
     */
    public boolean isQuickTransfer()
    {
    	return this.quickTransfer;
    }

    /**
     * Enable/Disable CADC quick transfer feature
     * @param quickTransfer True if enable quick transfer, false otherwise
     */
    public void setQuickTransfer(boolean quickTransfer)
    {
    	this.quickTransfer = quickTransfer;
    }

    public String getEndpoint()
    {
        if (this.protocols != null)
        {
            for (Protocol p : this.protocols)
            {
                return p.getEndpoint();
            }
        }
        return null;
    }
    public String getEndpoint(String strProtocol)
    {
        String rtn = null;
        if (this.protocols != null)
        {
            for (Protocol p : this.protocols)
            {
                if (p.getUri().equalsIgnoreCase(strProtocol)) {
                    rtn = p.getEndpoint();
                    break;
                }
            }
        }
        return rtn;
    }

    public Long getContentLength()
    {
        return contentLength;
    }

    public void setContentLength(Long contentLength)
    {
        this.contentLength = contentLength;
    }
    
    public String getRemoteIP() {
        return remoteIP;
    }
    
    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    /**
     * Returns a list of endpoints matching the protocol. Order in the list is
     * as recommended by the server: clients should try the end points
     * in the given order.
     * @param strProtocol
     * @return
     */
    public List<String> getAllEndpoints(String strProtocol)
    {
        List<String> rtn = new ArrayList<String>();
        if (this.protocols != null)
        {
            for (Protocol p : this.protocols)
            {
                if (p.getUri().equalsIgnoreCase(strProtocol)) {
                    rtn.add(p.getEndpoint());
                }
            }
        }
        return rtn;
    }

    /**
     * Returns a list of endpoints matching the protocol. Order in the list is
     * as recommended by the server: clients should try the end points
     * in the given order.
     * @return
     */
    public List<String> getAllEndpoints()
    {
        List<String> rtn = new ArrayList<String>();
        if (this.protocols != null)
        {
            for (Protocol p : this.protocols)
            {
                rtn.add(p.getEndpoint());
            }
        }
        return rtn;
    }


    public Direction getDirection()
    {
        return direction;
    }

    public URI getTarget()
    {
        return target;
    }

    public View getView()
    {
        return view;
    }

    public List<Protocol> getProtocols()
    {
        return protocols;
    }

    public boolean isKeepBytes()
    {
        return keepBytes;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Transfer[");
        sb.append(target);
        sb.append(",");
        sb.append(direction);
        sb.append(",");
        sb.append(view);

        if (protocols != null)
        {
            for (Protocol p : protocols)
            {
                sb.append(",");
                sb.append(p);
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
