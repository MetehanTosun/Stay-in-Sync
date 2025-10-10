package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.unistuttgart.stayinsync.transport.ScriptStatus;
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

    @Lob
    public String generatedSdkCode;

    public String generatedSdkHash;

    @Enumerated(EnumType.STRING)
    public ScriptStatus status = ScriptStatus.DRAFT;

    @OneToOne(mappedBy = "transformationScript")
    @JsonBackReference("transformationScript-reference")
    public Transformation transformation;
}
