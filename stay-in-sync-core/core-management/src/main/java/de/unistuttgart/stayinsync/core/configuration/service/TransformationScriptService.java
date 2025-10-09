package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSourceApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasTargetApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemApiRequestConfiguration;
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
        script.status = dto.status();

        script.hash = generateSha256Hash(dto.javascriptCode());

        Set<SourceSystemApiRequestConfiguration> requiredRestArcs = new HashSet<>();
        Set<AasSourceApiRequestConfiguration> requiredAasArcs = new HashSet<>();

        if(dto.requiredArcAliases() != null){
            for (String combinedAlias : dto.requiredArcAliases()) {
                String[] parts = combinedAlias.split("\\.",2);
                if (parts.length == 2){
                    String systemName = parts[0];
                    String arcName = parts[1];

                    Log.infof("Resolving ARC: system='%s', alias='%s'", systemName, arcName);

                    Optional<SourceSystemApiRequestConfiguration> restArcOpt = SourceSystemApiRequestConfiguration
                            .findBySourceSystemAndArcName(systemName, arcName);

                    if (restArcOpt.isPresent()) {
                        requiredRestArcs.add(restArcOpt.get());
                        continue;
                    }

                    Optional<AasSourceApiRequestConfiguration> aasArcOpt = AasSourceApiRequestConfiguration
                            .findBySourceSystemAndArcName(systemName, arcName);

                    if (aasArcOpt.isPresent()) {
                        requiredAasArcs.add(aasArcOpt.get());
                        continue;
                    }

                    throw new CoreManagementException(Response.Status.BAD_REQUEST, "ARC Not Found",
                            "The ARC specified in the script '%s' could not be found as a REST or AAS ARC.", combinedAlias);
                }
            }
        }
        Log.infof("Found %d unique ARCs required by the script", requiredRestArcs.size());

        Set<SourceSystemApiRequestConfiguration> finalArcSet = new HashSet<>();

        finalArcSet.addAll(requiredRestArcs);

        // TODO: Handle Union for ARCs with Graph and Script, since they can have a symmetric difference
        // Start with fresh set, add present ARCs for graph and script respectively.

        // Bind ManyToMany
        transformation.sourceSystemApiRequestConfigurations = finalArcSet;
        finalArcSet.forEach(sourceSystemApiRequestConfiguration -> sourceSystemApiRequestConfiguration.transformations.add(transformation));
        Log.infof("Bound %d REST ARCs to transformation %d", requiredRestArcs.size(), transformationId);

        transformation.aasSourceApiRequestConfigurations = requiredAasArcs;
        requiredAasArcs.forEach(arc -> arc.transformations.add(transformation));
        Log.infof("Bound %d AAS ARCs to transformation %d", requiredAasArcs.size(), transformationId);


        transformation.targetSystemApiRequestConfigurations.clear();
        transformation.aasTargetApiRequestConfigurations.clear();

        // REST Target Arc Binding
        if (dto.restTargetArcIds() != null && !dto.restTargetArcIds().isEmpty()) {
            List<TargetSystemApiRequestConfiguration> foundRestArcs = TargetSystemApiRequestConfiguration.list("id in ?1", dto.restTargetArcIds());
            if (foundRestArcs.size() != dto.restTargetArcIds().size()) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "REST Target ARC Not Found",
                        "One or more specified REST Target ARC IDs could not be found.");
            }
            transformation.targetSystemApiRequestConfigurations.addAll(foundRestArcs);
            foundRestArcs.forEach(arc -> arc.transformations.add(transformation));
            Log.infof("Bound %d REST Target ARCs to transformation %d", foundRestArcs.size(), transformationId);
        } else {
            Log.info("No REST Target ARCs to bind.");
        }

        // AAS Target Arc Binding
        if (dto.aasTargetArcIds() != null && !dto.aasTargetArcIds().isEmpty()) {
            List<AasTargetApiRequestConfiguration> foundAasArcs = AasTargetApiRequestConfiguration.list("id in ?1", dto.aasTargetArcIds());
            if (foundAasArcs.size() != dto.aasTargetArcIds().size()) {
                throw new CoreManagementException(Response.Status.BAD_REQUEST, "AAS Target ARC Not Found",
                        "One or more specified AAS Target ARC IDs could not be found.");
            }
            transformation.aasTargetApiRequestConfigurations.addAll(foundAasArcs);
            foundAasArcs.forEach(arc -> arc.transformations.add(transformation));
            Log.infof("Bound %d AAS Target ARCs to transformation %d", foundAasArcs.size(), transformationId);
        } else {
            Log.info("No AAS Target ARCs to bind.");
        }

        // SDK Code Generation for graalJS Context inside SyncNode
        String generatedSdkCode = targetSdkGeneratorService.generateSdkForTransformation(transformation);
        script.generatedSdkCode = generatedSdkCode;
        script.generatedSdkHash = generateSha256Hash(generatedSdkCode);

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