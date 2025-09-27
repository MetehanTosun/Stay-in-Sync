package de.unistuttgart.stayinsync.core.configuration.domain.entities.aas;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.persistence.Index;

@Entity
@Table(
        name = "aas_element_lite",
        uniqueConstraints = @UniqueConstraint(name = "uk_element_submodel_idshortpath", columnNames = {"submodel_lite_id", "id_short_path"}),
        indexes = {
                @Index(name = "idx_element_submodel", columnList = "submodel_lite_id"),
                @Index(name = "idx_element_id_short_path", columnList = "id_short_path")
        }
)
public class AasElementLite extends PanacheEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "submodel_lite_id", nullable = false)
    public AasSubmodelLite submodelLite;

    @Column(name = "id_short", nullable = false)
    public String idShort;

    @Column(name = "model_type", nullable = false)
    public String modelType;

    @Column(name = "value_type")
    public String valueType;

    @Column(name = "id_short_path", nullable = false)
    public String idShortPath;

    @Column(name = "encoded_api_path")
    public String encodedApiPath;

    @Column(name = "parent_path")
    public String parentPath;

    @Column(name = "has_children", nullable = false)
    public boolean hasChildren;

    @Column(name = "semantic_id")
    public String semanticId;

    @Column(name = "is_reference")
    public Boolean isReference;

    @Column(name = "reference_target_type")
    public String referenceTargetType;

    @Lob
    @Column(name = "reference_keys")
    public String referenceKeys;

    @Column(name = "target_submodel_id")
    public String targetSubmodelId;

    @Column(name = "type_value_list_element")
    public String typeValueListElement;

    @Column(name = "order_relevant")
    public Boolean orderRelevant;

    @Lob
    @Column(name = "input_signature")
    public String inputSignature;

    @Lob
    @Column(name = "output_signature")
    public String outputSignature;
}


