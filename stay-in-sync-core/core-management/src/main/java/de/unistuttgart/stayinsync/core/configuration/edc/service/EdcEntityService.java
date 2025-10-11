package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.EdcEntityDto;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.*;
import io.quarkus.logging.Log;

import java.util.List;

/**
 * Abstract class that offers the base structure of base methods needed to manage the core entities of the edc: Assets, Policies and Contract Definitions.
 *
 * @param <D> EdcEntityDto so the subclasses can use their own dtos that implement EdcEntityDto
 */
public abstract class EdcEntityService <D extends EdcEntityDto> {

    /**
     * Returns entity that is found by its id in the database Before that it fetches the entity
     * from its referenced edc and compares its dto to the persisted entity as its dto.
     * Boolean 'entityOutOfSync' is set to true if there is a mismatch. Else it is set to false.
     *
     * @return an entity as dto.
     *
     * @throws EntityNotFoundException if the entity was not found in the database.
     * @throws EntityFetchingException if the entity could not be fetched from the edc.
     */
    public abstract D getEntityWithSyncCheck(final Long entityId) throws EntityNotFoundException, EntityFetchingException;

    /**
     * Returns all entities that are referenced to the Edc found by the edcId. Before that it fetches all
     * entities from the found edc and compares their dtos to the persisted entities as dtos.
     * Boolean 'entityOutOfSync' is set to true if there are mismatches. Else it is set to false.
     *
     * @return a list of entities of found edcInstance as dtos.
     *
     * @throws EntityNotFoundException if the edcInstance was not found in the database.
     * @throws EntityFetchingException if the entities could not be fetched from the edc.
     */
    public abstract List<D> getEntitiesAsListWithSyncCheck(final Long edcId) throws EntityNotFoundException, EntityFetchingException;

    /**
     * Tries to create an entity in the edc, found by the edcId in the database. If it was successful
     * is also created in the database and returned ad an EntityDto.
     *
     * @param edcId to find an existing edcInstance in the database and link it to the newly created Entity.
     * @param dto contains the information to create the entity in the database and edc found by edcId.
     * @return the created asset as a dto.
     *
     * @throws EntityNotFoundException if the edcInstance was not found in the database.
     * @throws EntityCreationFailedException if it was not possible to create the entity in the edc and therefore in the database.
     */
    public abstract D createEntityInDatabaseAndEdc(final Long edcId, final D dto) throws EntityNotFoundException, EntityCreationFailedException;

    /**
     * Tries to update an entity found by id in the referenced Edc. If that was successful it also
     * is updated in the database and then returned as an EntityDto.
     *
     * @param entityId to find the existing entity in the database.
     * @param updatedDto contains the data to update the persisted entity in the database and the edc.
     * @return entityDto with updated fields.
     *
     * @throws EntityNotFoundException if the entity was not found in the database.
     * @throws EntityUpdateFailedException if it was not possible to update the entity in the edc and therefore in the database.
     */
    public abstract D updateEntityInDatabaseAndEdc(final Long entityId, final D updatedDto) throws EntityNotFoundException, EntityUpdateFailedException;

    /**
     * Tries to remove an entity found by id in database from the referenced Edc. If that
     * was successful it also is removed from the database.
     *
     * @param entityId to find the existing entity in the database.
     * @throws EntityNotFoundException if the entity was not found in the database.
     * @throws EntityDeletionFailedException if it was not possible to delete the entity from the edc and therefore from the database.
     */
    public abstract void deleteEntityFromDatabaseAndEdc(final Long entityId) throws EntityNotFoundException, EntityDeletionFailedException;

    /**
     *
     * @param status of the response
     * @throws DatabaseEntityOutOfSyncException if the entity was deleted on the edc without the systems knowledge
     * @throws AuthorizationFailedException if it was not possible to access the Edc Controlplane with the saved auth-key
     * @throws ConnectionToEdcFailedException if it was not possible to connect to the edc properly
     */
    protected void handleNegativeResponseCodes(int status) throws DatabaseEntityOutOfSyncException, AuthorizationFailedException, ConnectionToEdcFailedException {
        this.handleEntityNotFoundResponse(status);
        this.handleAuthorizationErrorResponse(status);
        this.handleTimeOutErrorResponse(status);
        this.handleBadRequestResponse(status);
        this.handleNoApiEndpointResponse(status);
    }

    /**
     * Handles case where entity was not found on EDC side.
     */
    protected void handleEntityNotFoundResponse(final int responseStatus) throws DatabaseEntityOutOfSyncException {
        final boolean entityNotFound = responseStatus == 404;
        if (entityNotFound) {
            final String exceptionMessage = "Entity not found on EDC (status 404). " +
                    "This usually means the policy definition was deleted externally — database and EDC are out of sync.";
            Log.errorf(exceptionMessage + " [status=%d]", responseStatus);
            throw new DatabaseEntityOutOfSyncException(exceptionMessage);
        }
    }

    /**
     * Handles EDC authorization errors.
     */
    protected void handleAuthorizationErrorResponse(final int responseStatus) throws AuthorizationFailedException {
        final boolean authorizationError = responseStatus == 401 || responseStatus == 403;
        if (authorizationError) {
            final String exceptionMessage = "Authorization failed (status " + responseStatus +
                    "). API key or credentials for the EDC instance are likely invalid.";
            Log.errorf(exceptionMessage);
            throw new AuthorizationFailedException(exceptionMessage);
        }
    }

    /**
     * Handles connection timeout or gateway timeout errors when contacting the EDC.
     */
    protected void handleTimeOutErrorResponse(final int responseStatus) throws ConnectionToEdcFailedException {
        final boolean timeoutError = responseStatus == 408 || responseStatus == 504;
        if (timeoutError) {
            final String exceptionMessage = "Connection to EDC timed out (status " + responseStatus +
                    "). The EDC instance may be temporarily unreachable or overloaded.";
            Log.errorf(exceptionMessage);
            throw new ConnectionToEdcFailedException(exceptionMessage);
        }
    }

    /**
     * Handles bad request errors (e.g. malformed payload, invalid structure).
     */
    protected void handleBadRequestResponse(final int responseStatus) throws ConnectionToEdcFailedException {
        final boolean badRequest = responseStatus == 400;
        if (badRequest) {
            final String exceptionMessage = "Bad request sent to EDC (status 400). " +
                    "Check payload formatting and required fields.";
            Log.errorf(exceptionMessage);
            throw new ConnectionToEdcFailedException(exceptionMessage);
        }
    }

    /**
     * Handles cases where the EDC API endpoint is not available.
     */
    protected void handleNoApiEndpointResponse(final int responseStatus) throws ConnectionToEdcFailedException {
        final boolean noApiEndpoint = responseStatus == 502 || responseStatus == 503;
        if (noApiEndpoint) {
            final String exceptionMessage = "EDC API endpoint not reachable (status " + responseStatus +
                    "). The connector service may be down or misconfigured.";
            Log.errorf(exceptionMessage);
            throw new ConnectionToEdcFailedException(exceptionMessage);
        }
    }

}
