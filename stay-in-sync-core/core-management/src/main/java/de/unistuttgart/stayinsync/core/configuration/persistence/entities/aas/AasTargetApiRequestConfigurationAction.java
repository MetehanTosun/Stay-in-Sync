package de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas;

import de.unistuttgart.stayinsync.transport.domain.AasTargetActionRole;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
public class AasTargetApiRequestConfigurationAction extends PanacheEntity {

    @ManyToOne(optional = false)
    public AasTargetApiRequestConfiguration aasTargetApiRequestConfiguration;

    @ManyToOne(optional = false)
    public AasElementLite targetElement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AasTargetActionRole actionRole;
}
