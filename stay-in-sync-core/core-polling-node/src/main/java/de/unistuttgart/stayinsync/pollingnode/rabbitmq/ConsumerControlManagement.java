package de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.pollingnode.usercontrol.management.PollingJobManagement;
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
     * @param pollingNodeManagement is applicationScoped and the entryPoint for the message processing.
     */
    public ConsumerControlManagement(final PollingJobManagement pollingNodeManagement){
        this.pollingNodeManagement = pollingNodeManagement;
    }


    /**
     * Awaits messages in syncJobCreated channel and passes them with the right consumable for the right method call on pollingNodeManagement along.
     *
     * @param syncJobMessage is sent to handleSyncJobMessage to be used there
     * @return accepted message if syncJob was valid or not accepted message if syncJob was invalid.
     */
    @Incoming("syncJobCreated")
    CompletionStage<Void> startSyncJobSupport(final Message<SyncJob> syncJobMessage) {
        return handleSyncJobMessage(syncJobMessage, pollingNodeManagement::beginSupportOfSyncJob);
    }


    /**
     * Awaits messages in syncJobCreated channel and passes them with the right consumable for the right method call on pollingNodeManagement along.
     *
     * @param syncJobMessage is sent to handleSyncJobMessage to be used there
     * @return accepted message if syncJob was valid or not accepted message if syncJob was invalid.
     */
    @Incoming("syncJobDeleted")
    CompletionStage<Void> supportedSyncJobDeletionChannel(final Message<SyncJob> syncJobMessage) {
        return handleSyncJobMessage(syncJobMessage, pollingNodeManagement::endSupportOfSyncJob);
    }




    /**
     * Calls pollingNodeManagement method by using Consumer parameter if SyncJob is valid.
     *
     * @param syncJobMessage message the channel received, that contains a unchecked syncJob.
     * @param syncJobHandler contains the method call of the calling channel handler.
     * @return accepted message if syncJob was valid or not accepted message if syncJob was invalid.
     */
    private CompletionStage<Void> handleSyncJobMessage(final Message<SyncJob> syncJobMessage,
                                                       final Consumer<SyncJob> syncJobHandler) {
        final SyncJob syncJob = syncJobMessage.getPayload();
        try {
            throwFaultySyncJobExceptionIfInvalid(syncJob);
            syncJobHandler.accept(syncJob);
            Log.info("Sent SyncJob was valid. Message accepted");
            return syncJobMessage.ack();
        } catch (FaultySourceSystemApiRequestMessageDtoException e) {
            Log.error(e.getMessage());
            return syncJobMessage.nack(new FaultySourceSystemApiRequestMessageDtoException(e.getMessage()));
        }
    }

    /**
     * Throws a FaultySyncJobException if the SyncJob has any invalid fields of obvious reason.
     * @param syncJob is the checked object.
     * @throws FaultySourceSystemApiRequestMessageDtoException if any field is null or empty.
     */
    private void throwFaultySyncJobExceptionIfInvalid(final SyncJob syncJob) throws FaultySourceSystemApiRequestMessageDtoException {
        if(syncJob.getApiAddress() == null || syncJob.getApiAddress().isEmpty()){
            final String errorMessage = "ApiAddress of sent SyncJob was empty";
            Log.error(errorMessage);
            throw new FaultySourceSystemApiRequestMessageDtoException(errorMessage);
        }
    }
}
