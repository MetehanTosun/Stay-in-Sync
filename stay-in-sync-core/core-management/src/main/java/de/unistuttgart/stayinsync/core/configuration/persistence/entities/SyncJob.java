package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Entity
public class SyncJob extends PanacheEntity {

    @NotNull
    @Size(min = 2, max = 50)
    public String name;


    public static List<SyncJob> listAllWhereNameLike(String name) {
        return (name != null) ?
                list("LOWER(name) LIKE CONCAT('%', ?1, '%')", name.toLowerCase()) :
                List.of();
    }

}
