package de.unistuttgart.stayinsync.core.configuration.edc;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "edc_property")
public class EDCProperty extends UuidEntity {

    @Column(name = "description", nullable = false, length = 1024)
    public String description;

    // Default-Konstruktor für JPA
    public EDCProperty() {}

    // Convenience-Konstruktor
    public EDCProperty(String description) {
        this.description = description;
    }

    // Getter/Setter falls benötigt
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
