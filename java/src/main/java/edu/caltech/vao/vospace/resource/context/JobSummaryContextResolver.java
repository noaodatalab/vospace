
package edu.caltech.vao.vospace.resource.context;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import edu.caltech.vao.vospace.resource.JobSummary;

/**
 * Context provider for providing the JAXBContext for the Address JAXB object
 */
@Provider
public class JobSummaryContextResolver implements ContextResolver<JAXBContext> {

    private static JAXBContext context;
    static {
        try {
            context = JAXBContext.newInstance(JobSummary.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public JAXBContext getContext(Class<?> type) {
        if (JobSummary.class.equals(type)) {
            return context;
        }
        return null;
    }
}

