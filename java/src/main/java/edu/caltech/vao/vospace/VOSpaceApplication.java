
package edu.caltech.vao.vospace;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class VOSpaceApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
	Set<Class<?>> classes = new HashSet<Class<?>>();
	classes.add(NodeResource.class);
	return classes;
    }
}
