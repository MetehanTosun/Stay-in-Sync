package java.de.unistuttgart.stayinsync.pollingnode.usercontrol.configuration;

import java.de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import java.de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import java.de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySyncJobException;

import jakarta.enterprise.context.ApplicationScoped;


/**
 * Creates PollingJobs out of SyncJobs with needed fields.
 */
@ApplicationScoped
public class PollingJobConfigurator {

    public PollingJobConfigurator(){
        super();
    }

    /**
     * Creates PollingJob based on information of SyncJob
     * @param syncJob used to get important initialisation information
     */
    public PollingJob createPollingJob(final SyncJob syncJob) throws FaultySyncJobException{
        final String apiAddress = syncJob.getApiAddress();
         if(apiAddress == null || apiAddress.isEmpty()){
             throw new FaultySyncJobException("SyncJob did not have defined source systems.");
        }
        return new PollingJob(apiAddress);
    }

}
