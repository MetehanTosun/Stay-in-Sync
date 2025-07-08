package de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.pollingnode.usercontrol.management.PollingJobManagement;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Awaits user actions related messages of the stay-in-sync-core. Supported Categories
 * are the starting, updating, and deleting of a supported pollingJob.
 */
@ApplicationScoped
public class ConsumerControlManagement {

    private final PollingJobManagement pollingNodeManagement;

    /**
     * Constructs the ConsumerControlManagement
     *
     * @param pollingNodeManagement is applicationScoped and the entryPoint for the message processing.
     */
    public ConsumerControlManagement(final PollingJobManagement pollingNodeManagement) {
        this.pollingNodeManagement = pollingNodeManagement;
    }


    /**
     * Awaits messages in syncJobCreated channel and passes them with the right consumable for the right method call on pollingNodeManagement along.
     *
     * @param apiRequestConfigurationMessageDTOMessageMessage is sent to handleSyncJobMessage to be used there
     * @return accepted message if syncJob was valid or not accepted message if syncJob was invalid.
     */
    @Incoming("syncJobCreated")
    CompletionStage<Void> startSyncJobSupport(final Message<SourceSystemApiRequestConfigurationMessageDTO> apiRequestConfigurationMessageDTOMessageMessage) {
        Log.info("Message to startSourceSystemSupport was received.");
        return handleSyncJobMessage(apiRequestConfigurationMessageDTOMessageMessage, pollingNodeManagement::beginSupportOfSyncJob);
    }


    /**
     * Awaits messages in syncJobCreated channel and passes them with the right consumable for the right method call on pollingNodeManagement along.
     *
     * @param apiRequestConfigurationMessageDTOMessageMessage is sent to handleSyncJobMessage to be used there
     * @return accepted message if syncJob was valid or not accepted message if syncJob was invalid.
     */
    @Incoming("syncJobDeleted")
    CompletionStage<Void> supportedSyncJobDeletionChannel(final Message<SourceSystemApiRequestConfigurationMessageDTO> apiRequestConfigurationMessageDTOMessageMessage) {
        Log.info("Message to startSourceSystemSupport was received.");
        return handleSyncJobMessage(apiRequestConfigurationMessageDTOMessageMessage, pollingNodeManagement::endSupportOfSyncJob);
    }


    /**
     * Calls pollingNodeManagement method by using Consumer parameter if SyncJob is valid.
     *
     * @param apiRequestConfigurationMessageDTOMessage message the channel received, that contains a unchecked syncJob.
     * @param apiRequestConfigurationMessageDTOHandler contains the method call of the calling channel handler.
     * @return accepted message if syncJob was valid or not accepted message if syncJob was invalid.
     */
    private CompletionStage<Void> handleSyncJobMessage(final Message<SourceSystemApiRequestConfigurationMessageDTO> apiRequestConfigurationMessageDTOMessage, final Consumer<SourceSystemApiRequestConfigurationMessageDTO> apiRequestConfigurationMessageDTOHandler) {
        final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessageDTOMessagePayload = apiRequestConfigurationMessageDTOMessage.getPayload();
        apiRequestConfigurationMessageDTOHandler.accept(apiRequestConfigurationMessageDTOMessagePayload);
        Log.info("Sent SyncJob was valid. Message accepted");
        return apiRequestConfigurationMessageDTOMessage.ack();
    }


}
