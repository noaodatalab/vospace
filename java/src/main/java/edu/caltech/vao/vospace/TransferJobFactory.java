 
package edu.caltech.vao.vospace;

import edu.caltech.vao.vospace.TransferJob;
import uws.UWSException;
import uws.job.UWSJob;
import uws.job.JobThread;
import uws.service.AbstractUWSFactory;

public class TransferJobFactory extends AbstractUWSFactory {

    /**
     * Creates the thread which will executes the task described by the given {@link UWSJob} instance.
     *
     * @param jobDescription        Description of the task to execute.
     *
     * @return                                      The task to execute.
     *
     * @throws UWSException         If there is an error while creating the job task.
     */
    public JobThread createJobThread(UWSJob job) throws UWSException {
	if (job.getJobList().getName().equals("transfers"))
	    return new TransferJob(job);
	else
	    throw new UWSException("Impossible to create a job inside the jobs list " + job.getJobList().getName());
    }

}