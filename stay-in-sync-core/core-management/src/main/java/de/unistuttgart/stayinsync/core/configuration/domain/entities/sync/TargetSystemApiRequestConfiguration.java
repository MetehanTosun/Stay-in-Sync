package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.core.transport.domain.TargetApiRequestConfigurationPatternType;
import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
public class TargetSystemApiRequestConfiguration extends ApiRequestConfiguration {

    @ManyToMany(mappedBy = "targetSystemApiRequestConfigurations")
    public Set<Transformation> transformations;

    @ManyToOne
    public TargetSystem targetSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TargetApiRequestConfigurationPatternType arcPatternType;

    /** The List of actions allows for 'any' constellation of sequential calls to be defined by the user.
     * Execution Order assigns a specified sequential ordering, that allows for well-defined processing.
     */
    @OneToMany(
            mappedBy = "targetSystemApiRequestConfiguration",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("executionOrder ASC")
    public List<TargetSystemApiRequestConfigurationAction> actions;
}
