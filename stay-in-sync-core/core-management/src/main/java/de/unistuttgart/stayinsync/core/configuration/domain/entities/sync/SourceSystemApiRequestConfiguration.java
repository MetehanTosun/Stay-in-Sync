package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import jakarta.persistence.*;

import java.util.*;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystemApiRequestConfiguration extends ApiRequestConfiguration {

    @ManyToMany(mappedBy = "sourceSystemApiRequestConfigurations")
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

    public static List<SourceSystemApiRequestConfiguration> listAllActiveAndUnused() {
        String query = "SELECT sse FROM SourceSystemApiRequestConfiguration sse " +
                "WHERE sse.deploymentStatus IN (:statuses) " +
                "AND SIZE(sse.transformations) > 0";

        return getEntityManager().createQuery(query, SourceSystemApiRequestConfiguration.class)
                .setParameter("statuses", List.of(JobDeploymentStatus.FAILING, JobDeploymentStatus.DEPLOYED))
                .getResultList();
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
