
package edu.caltech.vao.vospace.storage;

import edu.caltech.vao.vospace.VOSpaceException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.commons.io.FileUtils;

/**
 * Backend storage manager for local filesystem
 */
public class LocalFSStorageManager implements StorageManager {

    /**
     * Construct a basic LocalFSStorageManager
     */
    public LocalFSStorageManager(Properties props) {}


    /**
     * Authenticate the client to the current backend storage
     * @param endpoint The storage URL
     * @param credentials The client's security credentials
     */
    public void authenticate(String endpoint, HashMap<String, String> credentials) throws VOSpaceException {
        // Nothing needed here
    }

    /**
     * Create a container at the specified location in the current backend storage
     * @param location The location of the container
     */
    public void createContainer(String location) throws VOSpaceException {
        // Need to set permissions to 775 so that Tomcat and manager users
        // do not clash when creating/writing files
        Set<PosixFilePermission> perm = PosixFilePermissions.fromString("rwxrwxr-x");
        Path path = null;
        try {
            path = Paths.get(new URI(location));
            Files.createDirectory(path);
            Files.setPosixFilePermissions(path, perm);
        } catch (FileAlreadyExistsException fe) {
            try {
                if (!Files.isDirectory(path)) {
                    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, "Container cannot be created");
                } else {
                    Files.setPosixFilePermissions(path, perm);
                }
            } catch (Exception e) {
                throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Create a symbolic link at the specified location in the current backend storage
     * @param location The location of the link
     * @param target The target of the link
     */
    public void createLink(String location, String target) throws VOSpaceException {
        Path path = null;
        Path tgtPath = null;
        try {
            path = Paths.get(new URI(location));
            tgtPath = path.getParent().relativize(Paths.get(new URI(target)));
            // System.out.println(location + " " + target + " " + path + " " + tgtPath);
            Files.createSymbolicLink(path, tgtPath);
        } catch (FileAlreadyExistsException fe) {
            try {
                if (!Files.isSymbolicLink(path)) {
                    throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, "Link cannot be created");
                }
            } catch (Exception e) {
                throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Create a zero-byte file at the specified location in the current backend storage
     * @param location The location of the file
     */
    public void touch(String location) throws VOSpaceException {
        try {
            File file = new File(new URI(location));
            FileUtils.touch(file);
        } catch (Exception e) {
            if (!e.getMessage().contains("last modification date")) throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Move the bytes from the specified old location to the specified new location
     * in the current backend storage
     * @param oldLocation The old location of the bytes
     * @param newLocation The new location of the bytes
     */
    public void moveBytes(String oldLocation, String newLocation) throws VOSpaceException {
        try {
            File oldFile = new File(new URI(oldLocation));
            if (oldFile.isFile()) {
                FileUtils.moveFile(oldFile, new File(new URI(newLocation)));
            } else {
                FileUtils.moveDirectory(oldFile, new File(new URI(newLocation)));
            }
        } catch (Exception e) {
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Copy the bytes from the specified old location to the specified new location
     * in the current backend storage
     * @param oldLocation The old location of the bytes
     * @param newLocation The new location of the bytes
     */
    public void copyBytes(String oldLocation, String newLocation) throws VOSpaceException {
        try {
            File oldFile = new File(new URI(oldLocation));
            if (oldFile.isFile()) {
                FileUtils.copyFile(oldFile, new File(new URI(newLocation)));
            } else {
                FileUtils.copyDirectory(oldFile, new File(new URI(newLocation)));
            }
        } catch (Exception e) {
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Put the bytes from the specified input stream at the specified location in
     * the current backend storage
     * @param location The location for the bytes
     * @param stream The stream containing the bytes
     */
    public void putBytes(String location, InputStream stream) throws VOSpaceException {
        try {
            URI uri = new URI(location);
            BufferedInputStream bis = new BufferedInputStream(stream);
            FileOutputStream fos = new FileOutputStream(uri.getPath());
            byte[] buffer = new byte[8192];
            int count = bis.read(buffer);
            while (count != -1 && count <= 8192) {
                fos.write(buffer, 0, count);
                count = bis.read(buffer);
            }
            if (count != -1) {
                fos.write(buffer, 0, count);
            }
            fos.close();
            bis.close();
        } catch (Exception e) {
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Get the bytes from the specified location in the current backend storage
     * @param location The location of the bytes
     * @return a stream containing the requested bytes
     */
    public InputStream getBytes(String location) throws VOSpaceException {
        try {
            URI uri = new URI(location);
            InputStream in = new FileInputStream(uri.getPath());
            return in;
        } catch (Exception e) {
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Remove the bytes at the specified location in the current backend storage
     * @param location The location of the bytes
     */
    public void removeBytes(String location) throws VOSpaceException {
        try {
            boolean success = new File(new URI(location)).delete();
        } catch (Exception e) {
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Retrieve when the bytes at the specified location in the current backend storage
     * were last modified. A response of -1 indicates that the information is not
     * available.
     * @param location The location to check
     * @return when the location was last modified
     */
    public long lastModified(String location) throws VOSpaceException {
        try {
            long lastModified = new File(new URI(location)).lastModified();
            return lastModified;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Retrieve the size of the data object at the specified location.
     * @param location The location to check
     * @return how many bytes the location occupies
     */
    public long size(String location) throws VOSpaceException {
        try {
            long size = new File(new URI(location)).length();
            return size;
        } catch (Exception e) {
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Retrieve the md5 sum of the data object at the specified location.
     * @param location The location to check
     * @return the md5 sum
     */
    public String md5(String location) throws VOSpaceException {
        try {
            StringBuffer md5 = new StringBuffer();
            if (size(location) > 0) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                FileInputStream fis = new FileInputStream(new File(new URI(location)));
                byte[] dataBytes = new byte[1024];
                int nread = 0;
                while ((nread = fis.read(dataBytes)) != -1) {
                    md.update(dataBytes, 0, nread);
                }
                byte[] mdbytes = md.digest();
                for (int i = 0; i < mdbytes.length; i++) {
                    md5.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
                }
            } else {
                md5.append("d41d8cd98f00b204e9800998ecf8427e");
            }
            return md5.toString();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new VOSpaceException(VOSpaceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


}
