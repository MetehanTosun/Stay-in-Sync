package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSourceApiRequestConfiguration;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO Implementierung
@Entity
public class Transformation extends PanacheEntity {

    public String name;

    public String description;

    @ManyToOne
    @JsonBackReference("syncJob-back-reference")
    public SyncJob syncJob;

    @OneToOne(cascade = CascadeType.ALL)
    @JsonManagedReference("transformationScript-reference")
    public TransformationScript transformationScript;

    @Enumerated(EnumType.STRING)
    public JobDeploymentStatus deploymentStatus = JobDeploymentStatus.UNDEPLOYED;

    public String workerHostName;

    @OneToOne(cascade = CascadeType.ALL)
    public TransformationRule transformationRule;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "transformation_sourceApiRequestConfiguration",
            joinColumns = @JoinColumn(name = "transformation_id"),
            inverseJoinColumns = @JoinColumn(name = "source_system_api_request_configuration_id")
    )
    public Set<SourceSystemApiRequestConfiguration> sourceSystemApiRequestConfigurations = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "transformation_aas_arc_config",
            joinColumns = @JoinColumn(name = "transformation_id"),
            inverseJoinColumns = @JoinColumn(name = "aas_arc_config_id")
    )
    public Set<AasSourceApiRequestConfiguration> aasSourceApiRequestConfigurations = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "transformation_sourceVariable",
            joinColumns = @JoinColumn(name = "transformation_id"),
            inverseJoinColumns = @JoinColumn(name = "source_variable_id")
    )
    public Set<SourceSystemVariable> sourceSystemVariables = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "transformation_targetApiRequestConfiguration",
            joinColumns = @JoinColumn(name = "transformation_id"),
            inverseJoinColumns = @JoinColumn(name = "target_system_api_request_configuration_id")
    )
    public Set<TargetSystemApiRequestConfiguration> targetSystemApiRequestConfigurations = new HashSet<>();

    public static List<Transformation> listAllWithoutSyncJob() {
        return find("syncJob is null").list();
    }

    public static List<Transformation> listAllWithSyncJob() {
        return find("syncJob is not null").list();
    }

    public static List<Transformation> findBySyncJobId(Long syncJobId) {
        return find("syncJob.id", syncJobId).list();
    }


}
