package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.unistuttgart.stayinsync.core.transport.ScriptStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
public class TransformationScript extends PanacheEntity {

    public String name;

    @Lob
    public String typescriptCode;

    @Lob
    public String javascriptCode;

    public String hash;

    @Enumerated(EnumType.STRING)
    public ScriptStatus status = ScriptStatus.DRAFT;

    @OneToOne(mappedBy = "transformationScript")
    @JsonBackReference("transformationScript-reference")
    public Transformation transformation;
}
