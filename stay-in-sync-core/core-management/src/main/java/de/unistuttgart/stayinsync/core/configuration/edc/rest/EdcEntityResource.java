package de.unistuttgart.stayinsync.core.configuration.edc.rest;

import com.fasterxml.jackson.annotation.JsonView;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.EdcEntityDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.VisibilitySidesForDto;
import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * Abstract class that offers the base structure of base methods needed to
 * offer API endpoints the Frontend uses to communicate with the database and edc by using the business logic.
 *
 * @param <D> EdcEntityDto so the subclasses can use their own dtos that implement EdcEntityDto
 */
public abstract class EdcEntityResource <D extends EdcEntityDto> {


    /**
     * Gets an entity as dto found by the given entityId.
     * Also updates the outOfSync boolean for the entity if itself and its Edc counterpart are out of sync.
     *
     * @param edcId NOT USED SHOULD BE DELETED
     * @param entityId used to find entity to return in the database
     * @return Response with the found entity.
     */
    public abstract Response getEntity(final Long edcId, final Long entityId);

    /**
     * Gets a list of entities as dtos found by their EdcInstance found by the given edcId.
     * Also updates the outOfSync boolean for entities that are out of sync with their edc counterparts.
     *
     * @param edcId used to find the edcInstance in the database and get all of its entities
     *
     * @return Response with the List of EntityDtos
     */
    public abstract Response getListOfEntities(final Long edcId);

    /**
     * Creates entity on both Edc and the database with the given EntityDto information
     *
     * @param edcId used to add reference to the found EdcInstance to the created entity.
     * @param dto contains the data to create a new entity on the Edc and the database.
     *
     * @return Response containing the data of the created entity.
     */
    public abstract Response createEntity(final Long edcId, final D dto);

    /**
     * Updates entity found with the given entityId on both the Edc and the database with the given EntityDto information.
     *
     * @param edcId NOT USED SHOULD BE REMOVED
     * @param entityId to find the entity to update in the Edc and the database
     * @param updatedDto contains data to update the entity
     * @return Response containing the data of the updated entity
     */
    public abstract Response updateEntity(final Long edcId, final Long entityId, final D updatedDto);

    /**
     * Removes Entity from its edc and the database.
     *
     * @param edcId NOT USED AND SHOULD BE DELETED
     * @param entityId to find the entity in the database
     *
     * @return Response with DELETED status if the entity was deleted from both layers.
     */
    public abstract Response deleteEntity(final Long edcId, final Long entityId);

    protected Response handleNotFoundException(final Long id){
        final String exceptionMessage = "The Entity was not found with the id in the database.";
        Log.warnf(exceptionMessage, id);
        return Response.status(Response.Status.NOT_FOUND)
                .entity(exceptionMessage)
                .build();
    }

    protected Response handleNullArgument(){
        final String exceptionMessage = "Invalid Id, can not be null. No Entity found.";
        Log.warnf(exceptionMessage);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(exceptionMessage)
                .build();
    }

}
