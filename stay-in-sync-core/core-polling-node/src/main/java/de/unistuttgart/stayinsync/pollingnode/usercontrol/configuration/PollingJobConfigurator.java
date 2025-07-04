package de.unistuttgart.stayinsync.pollingnode.usercontrol.configuration;

import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;

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
    public PollingJob createPollingJob(final SyncJob syncJob) throws FaultySourceSystemApiRequestMessageDtoException {
        final String apiAddress = syncJob.getApiAddress();
         if(apiAddress == null || apiAddress.isEmpty()){
             throw new FaultySourceSystemApiRequestMessageDtoException("SyncJob did not have defined source systems.");
        }
        return new PollingJob(apiAddress);
    }

}
