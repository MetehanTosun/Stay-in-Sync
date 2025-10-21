package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSourceApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationScriptMapper;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TransformationScript;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
@Transactional(REQUIRED)
public class TransformationScriptService {

    @Inject
    TransformationScriptMapper mapper;

    @Inject
    TargetSdkGeneratorService targetSdkGeneratorService;

    public TransformationScript create(TransformationScriptDTO dto) {
        Log.debugf("Creating new transformation script with name: %s", dto.name());
        TransformationScript transformationScript = mapper.mapToEntity(dto);
        transformationScript.persist();
        return transformationScript;
    }

    public Optional<TransformationScript> update(Long id, TransformationScript scriptFromDTO) {
        Log.debugf("Updating transformation script with id: %s", id);
        return TransformationScript.<TransformationScript>findByIdOptional(id)
                .map(targetScript -> {
                    mapper.mapFullUpdate(scriptFromDTO, targetScript);
                    return targetScript;
                });
    }

    @Transactional
    public Optional<TransformationScript> findByTransformationId(Long transformationId) {
        return Transformation.<Transformation>findByIdOptional(transformationId)
                .map(transformation -> transformation.transformationScript);
    }

    /**
     * Saves a new TransformationScript or updates an existing one for a specific Transformation.
     * This is a transactional, orchestrator method that performs a sequence of operations:
     * <ol>
     *   <li>Finds the parent Transformation entity.</li>
     *   <li>Finds or creates the associated TransformationScript entity.</li>
     *   <li>Updates the script's properties (code, status, etc.) from the provided DTO.</li>
     *   <li>Resolves and binds all required Source ARCs (both REST and AAS) based on aliases found in the script.</li>
     *   <li>Clears and re-binds all required Target ARCs (both REST and AAS) based on IDs from the DTO.</li>
     *   <li>Generates the executable JavaScript SDK for the 'targets' object based on the new configuration.</li>
     *   <li>Persists all changes to the database.</li>
     * </ol>
     *
     * @param transformationId The ID of the parent Transformation to which the script belongs.
     * @param dto              The {@link TransformationScriptDTO} containing the new or updated script data.
     * @return The persisted {@link TransformationScript} entity.
     * @throws CoreManagementException if the Transformation or any specified ARC cannot be found.
     */
    @Transactional
    public TransformationScript saveOrUpdateForTransformation(Long transformationId, TransformationScriptDTO dto) {
        Transformation transformation = findTransformationOrThrow(transformationId);
        TransformationScript script = findOrCreateScriptForTransformation(transformation);

        updateScriptProperties(script, dto);
        resolveAndBindSourceArcs(transformation, dto);
        clearAndBindTargetArcs(transformation, dto);
        generateAndSetSdk(script, transformation);

        script.persist();
        transformation.persist();

        return script;
    }

    /**
     * Finds a Transformation by its ID or throws a NOT_FOUND exception.
     *
     * @param transformationId The ID of the transformation to find.
     * @return The found {@link Transformation} entity.
     * @throws CoreManagementException if the transformation does not exist.
     */
    private Transformation findTransformationOrThrow(Long transformationId) {
        return Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND,
                        "Transformation not found",
                        "Cannot save script for non-existent transformation with id %d", transformationId));
    }

    /**
     * Retrieves the existing TransformationScript for a Transformation, or creates a new one if it doesn't exist.
     *
     * @param transformation The parent Transformation.
     * @return The existing or newly created {@link TransformationScript}.
     */
    private TransformationScript findOrCreateScriptForTransformation(Transformation transformation) {
        if (transformation.transformationScript == null) {
            Log.infof("No existing script found for transformation %d. Creating a new one.", transformation.id);
            TransformationScript newScript = new TransformationScript();
            newScript.transformation = transformation;
            transformation.transformationScript = newScript;
            return newScript;
        } else {
            Log.infof("Existing script %d found for transformation %d. Updating.", transformation.transformationScript.id, transformation.id);
            return transformation.transformationScript;
        }
    }

    /**
     * Updates the properties of a TransformationScript entity from a DTO.
     *
     * @param script The {@link TransformationScript} entity to update.
     * @param dto    The {@link TransformationScriptDTO} containing the new data.
     */
    private void updateScriptProperties(TransformationScript script, TransformationScriptDTO dto) {
        script.name = dto.name();
        script.typescriptCode = dto.typescriptCode();
        script.javascriptCode = dto.javascriptCode();
        script.status = dto.status();
        script.hash = generateSha256Hash(dto.javascriptCode());
    }

    /**
     * Resolves Source ARC aliases from the DTO into entity objects and binds them to the Transformation.
     * It handles both REST and AAS source ARCs.
     *
     * @param transformation The {@link Transformation} to bind the ARCs to.
     * @param dto            The DTO containing the list of required ARC aliases.
     * @throws CoreManagementException if an alias cannot be resolved to a known ARC.
     */
    private void resolveAndBindSourceArcs(Transformation transformation, TransformationScriptDTO dto) {
        Set<SourceSystemApiRequestConfiguration> requiredRestArcs = new HashSet<>();
        Set<AasSourceApiRequestConfiguration> requiredAasArcs = new HashSet<>();

        if (dto.requiredArcAliases() != null) {
            for (String combinedAlias : dto.requiredArcAliases()) {
                String[] parts = combinedAlias.split("\\.", 2);
                if (parts.length == 2) {
                    String systemName = parts[0];
                    String arcName = parts[1];

                    Log.infof("Resolving Source ARC: system='%s', alias='%s'", systemName, arcName);

                    // Try to resolve as a REST arc first
                    Optional<SourceSystemApiRequestConfiguration> restArcOpt = SourceSystemApiRequestConfiguration
                            .findBySourceSystemAndArcName(systemName, arcName);
                    if (restArcOpt.isPresent()) {
                        requiredRestArcs.add(restArcOpt.get());
                        continue;
                    }

                    // If not found, try to resolve as an AAS arc
                    Optional<AasSourceApiRequestConfiguration> aasArcOpt = AasSourceApiRequestConfiguration
                            .findBySourceSystemAndArcName(systemName, arcName);
                    if (aasArcOpt.isPresent()) {
                        requiredAasArcs.add(aasArcOpt.get());
                        continue;
                    }

                    // If still not found, throw an error
                    throw new CoreManagementException(Response.Status.BAD_REQUEST, "ARC Not Found",
                            "The Source ARC specified in the script '%s' could not be found.", combinedAlias);
                }
            }
        }

        // Bind REST Source ARCs
        transformation.sourceSystemApiRequestConfigurations = requiredRestArcs;
        requiredRestArcs.forEach(arc -> arc.transformations.add(transformation));
        Log.infof("Bound %d REST Source ARCs to transformation %d", requiredRestArcs.size(), transformation.id);

        // Bind AAS Source ARCs
        transformation.aasSourceApiRequestConfigurations = requiredAasArcs;
        requiredAasArcs.forEach(arc -> arc.transformations.add(transformation));
        Log.infof("Bound %d AAS Source ARCs to transformation %d", requiredAasArcs.size(), transformation.id);
    }

    /**
     * Clears existing Target ARC associations and binds new ones based on IDs from the DTO.
     * This method handles both REST and AAS target ARCs.
     *
     * @param transformation The {@link Transformation} to bind the ARCs to.
     * @param dto            The DTO containing the lists of Target ARC IDs.
     * @throws CoreManagementException if any of the specified Target ARC IDs do not exist.
     */
    private void clearAndBindTargetArcs(Transformation transformation, TransformationScriptDTO dto) {
        // Clear existing associations
        transformation.targetSystemApiRequestConfigurations.clear();
        transformation.aasTargetApiRequestConfigurations.clear();

        // Bind REST Target Arcs
        if (dto.restTargetArcIds() != null && !dto.restTargetArcIds().isEmpty()) {
            List<TargetSystemApiRequestConfiguration> foundRestArcs = TargetSystemApiRequestConfiguration.list("id in ?1", dto.restTargetArcIds());
            if (foundRestArcs.size() != dto.restTargetArcIds().size()) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "REST Target ARC Not Found",
                        "One or more specified REST Target ARC IDs could not be found.");
            }
            transformation.targetSystemApiRequestConfigurations.addAll(foundRestArcs);
            foundRestArcs.forEach(arc -> arc.transformations.add(transformation));
            Log.infof("Bound %d REST Target ARCs to transformation %d", foundRestArcs.size(), transformation.id);
        }

        // Bind AAS Target Arcs
        if (dto.aasTargetArcIds() != null && !dto.aasTargetArcIds().isEmpty()) {
            List<AasTargetApiRequestConfiguration> foundAasArcs = AasTargetApiRequestConfiguration.list("id in ?1", dto.aasTargetArcIds());
            if (foundAasArcs.size() != dto.aasTargetArcIds().size()) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "AAS Target ARC Not Found",
                        "One or more specified AAS Target ARC IDs could not be found.");
            }
            transformation.aasTargetApiRequestConfigurations.addAll(foundAasArcs);
            foundAasArcs.forEach(arc -> arc.transformations.add(transformation));
            Log.infof("Bound %d AAS Target ARCs to transformation %d", foundAasArcs.size(), transformation.id);
        }
    }

    /**
     * Generates the executable JavaScript SDK for the 'targets' object and sets it on the script entity.
     *
     * @param script         The {@link TransformationScript} entity to update.
     * @param transformation The parent {@link Transformation}, now with its updated ARC bindings.
     */
    private void generateAndSetSdk(TransformationScript script, Transformation transformation) {
        String generatedSdkCode = targetSdkGeneratorService.generateSdkForTransformation(transformation);
        script.generatedSdkCode = generatedSdkCode;
        script.generatedSdkHash = generateSha256Hash(generatedSdkCode);
    }

    @Transactional(SUPPORTS)
    public Optional<TransformationScript> findById(Long id) {
        Log.debugf("Finding transformation script by id: %s", id);
        return TransformationScript.findByIdOptional(id);
    }

    @Transactional(SUPPORTS)
    public List<TransformationScript> findAll() {
        Log.debug("Getting all transformation scripts.");
        return TransformationScript.listAll();
    }

    @Transactional(SUPPORTS)
    public boolean delete(Long id) {
        Log.debugf("Deleting transformation script with id %d", id);
        return TransformationScript.deleteById(id);
    }

    private String generateSha256Hash(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hashBytes);
            StringBuilder hexString = new StringBuilder(number.toString(16));

            while (hexString.length() < 64) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Hashing algorithm not available", "SHA-256 is not supported on this system.");
        }
    }
}