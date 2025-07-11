package de.unistuttgart.stayinsync.pollingnode.execution.executor;

import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;


/**
 * This class contains the information needed to poll Data from Apis.
 * The map of this class is updated whenever a job is created, updated, or deleted.
 */
@ApplicationScoped
public class ApiDetailsContainerForExecutor {

    final Map<Long, SourceSystemApiRequestConfigurationMessageDTO> detailsForJobs;

    public ApiDetailsContainerForExecutor() {
        this.detailsForJobs = new HashMap<>();
    }

    public SourceSystemApiRequestConfigurationMessageDTO getDetailsForSpecificJob(final long id) {
        return detailsForJobs.get(id);
    }

    public void putKeyAndElementInMap(final long key, final SourceSystemApiRequestConfigurationMessageDTO element){
        detailsForJobs.put(key,element);
    }

    public void removeKeyAndElementInMap(final long key){
        detailsForJobs.remove(key);
    }
}
