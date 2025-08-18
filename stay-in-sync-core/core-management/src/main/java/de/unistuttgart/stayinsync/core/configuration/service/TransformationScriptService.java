package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationScript;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationScriptMapper;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

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

    @Transactional
    public TransformationScript saveOrUpdateForTransformation(Long transformationId, TransformationScriptDTO dto) {
        Log.infof("Receiving %s", dto.requiredArcAliases().toString());
        Transformation transformation = Transformation.<Transformation>findByIdOptional(transformationId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND,
                        "Transformation not found",
                        "Cannot save script for non-existent transformation with id %d", transformationId));

        TransformationScript script = transformation.transformationScript;

        if (script == null) {
            Log.infof("No existing script found for transformation %d. Creating a new one.", transformationId);
            script = new TransformationScript();
            script.transformation = transformation;
            transformation.transformationScript = script;
        } else {
            Log.infof("Existing script %d found for transformation %d. Updating.", script.id, transformationId);
        }

        script.name = dto.name();
        script.typescriptCode = dto.typescriptCode();
        script.javascriptCode = dto.javascriptCode();
        script.hash = dto.hash();

        Set<SourceSystemApiRequestConfiguration> scriptArcs = new HashSet<>();
        if(dto.requiredArcAliases() != null){
            for (String combinedAlias : dto.requiredArcAliases()) {
                String[] parts = combinedAlias.split("\\.",2);
                if (parts.length == 2){
                    String systemName = parts[0];
                    String arcName = parts[1];

                    Log.infof("Found systemName: %s and arcName: %s", systemName, arcName);

                    SourceSystemApiRequestConfiguration foundArc = SourceSystemApiRequestConfiguration.findBySourceSystemAndArcName(systemName, arcName)
                            .orElseThrow(() -> new CoreManagementException(Response.Status.BAD_REQUEST, "ARC Not Found",
                                    "The ARC specified in the script '%s' could not be found.", combinedAlias));
                    scriptArcs.add(foundArc);
                }
            }
        }
        Log.infof("Found %d unique ARCs required by the script", scriptArcs.size());

        Set<SourceSystemApiRequestConfiguration> finalArcSet = new HashSet<>();

        finalArcSet.addAll(scriptArcs);

        // TODO: Handle Union for ARCs with Graph and Script, since they can have a symmetric difference
        // Start with fresh set, add present ARCs for graph and script respectively.

        // Bind ManyToMany
        transformation.sourceSystemApiRequestConfigurations = finalArcSet;
        finalArcSet.forEach(sourceSystemApiRequestConfiguration -> sourceSystemApiRequestConfiguration.transformations.add(transformation));

        Log.infof("Final total bound ARCs for transformation %d: %d", transformationId, finalArcSet.size());

        script.persist();
        transformation.persist();

        return script;
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
}
