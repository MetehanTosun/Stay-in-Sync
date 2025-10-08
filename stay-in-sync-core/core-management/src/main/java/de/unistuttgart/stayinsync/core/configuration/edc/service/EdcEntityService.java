package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.EdcEntityDto;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.*;

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

}
