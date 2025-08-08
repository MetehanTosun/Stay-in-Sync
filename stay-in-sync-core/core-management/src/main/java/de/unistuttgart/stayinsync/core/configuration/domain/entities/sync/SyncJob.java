package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO Implementierung
@Entity
public class SyncJob extends PanacheEntity {

    @NotNull
    @Size(min = 2, max = 50, message = "size must be between {min} and {max}")
    public String name;

    public String syncNodeIdentifier;

    public String description;

    public boolean isSimulation;

    @OneToMany(mappedBy = "syncJob", cascade = CascadeType.ALL)
    @JsonManagedReference("syncJob-back-reference")
    public Set<Transformation> transformations = new HashSet<>();

    public static List<SyncJob> listAllWhereNameLike(String name) {
        return (name != null) ?
                list("LOWER(name) LIKE CONCAT('%', ?1, '%')", name.toLowerCase()) :
                List.of();
    }

}
