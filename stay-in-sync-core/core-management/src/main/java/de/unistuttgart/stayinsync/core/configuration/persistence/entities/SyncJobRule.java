package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
public class SyncJobRule extends PanacheEntity {

    public String name;

    @OneToOne
    public SyncJob syncJob;

    public int updateIntervall;


}
