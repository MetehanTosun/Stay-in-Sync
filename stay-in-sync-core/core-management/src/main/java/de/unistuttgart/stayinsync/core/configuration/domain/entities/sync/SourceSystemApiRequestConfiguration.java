package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystemApiRequestConfiguration extends ApiRequestConfiguration {

    @ManyToMany(mappedBy = "sourceSystemApiRequestConfigrations")
    public Set<Transformation> transformations;

    @ManyToOne
    public SourceSystem sourceSystem;

    @ManyToOne
    public SourceSystemEndpoint sourceSystemEndpoint;

    @Lob
    public String responseDts;

    @Enumerated(EnumType.STRING)
    public JobDeploymentStatus deploymentStatus;

    public String workerPodName;

    public boolean responseIsArray;

    public int pollingIntervallTimeInMs;

    public static List<SourceSystemApiRequestConfiguration> listAllWherePollingIsActiveAndUnused() {
        String query = "SELECT sse FROM SourceSystemApiRequestConfiguration sse " +
                "WHERE sse.active = true " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM Transformation t " +
                "    WHERE sse MEMBER OF t.sourceSystemEndpoints " +
                "    AND t.syncJob.deployed = true" +
                ")";

        return list(query);
    }

    public static List<SourceSystemApiRequestConfiguration> findBySourceSystemId(Long sourceSystemId) {
        return find("sourceSystem.id", sourceSystemId).list();
    }

    public static List<SourceSystemApiRequestConfiguration> findByEndpointId(Long endpointId) {
        return find("sourceSystemEndpoint.id", endpointId).list();
    }

    /**
     * Finds all SourceSystemApiRequestConfigurations linked to SourceSystems by their names,
     * using the Panache Active Record pattern.
     * <p>
     * This method uses a single, efficient JOIN query and returns a list of object arrays,
     * where each array contains:
     * [0]: The name of the SourceSystem (String)
     * [1]: The associated SourceSystemApiRequestConfiguration entity
     *
     * @param names A set of source system names.
     * @return A list of [String, SourceSystemApiRequestConfiguration] pairs.
     */
    public static List<Object[]> findArcsGroupedBySourceSystemName(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }

        return getEntityManager().createQuery(
                        "SELECT arc.sourceSystem.name, arc " +
                                "FROM SourceSystemApiRequestConfiguration arc " +
                                "WHERE arc.sourceSystem.name IN :names", Object[].class)
                .setParameter("names", names)
                .getResultList();
    }

    public static Optional<SourceSystemApiRequestConfiguration> findBySourceSystemAndArcName(String sourceSystemName, String arcName) {
        return find("sourceSystem.name = ?1 and alias = ?2", sourceSystemName, arcName).firstResultOptional();
    }

}
