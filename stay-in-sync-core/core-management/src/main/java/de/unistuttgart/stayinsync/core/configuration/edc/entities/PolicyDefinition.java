package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.PolicyDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.AssetDataAddressMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.AssetPropertiesMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.PolicyDefinitionMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.PolicyMapper;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entitätsklasse für EDC-Policies in der Datenbank.
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System
 * und speichert alle relevanten Informationen persistent in der Datenbank.
 * Eine Policy definiert Zugriffs- und Nutzungsbedingungen für Assets im EDC-System.
 * Die vollständige Policy-Definition wird als JSON-String gespeichert.
 */
@Setter
@Getter
@Entity
@Table(name = "edc_policydefinition")
public class PolicyDefinition extends PanacheEntity {

    @Column(nullable = false, unique = true)
    private String policyDefinitionId;

    @OneToOne
    private Policy policy;

    @Column
    private String displayName;

    @ManyToOne
    @JoinColumn(name = "edc_instance")
    private EDCInstance targetEDC;

    @Column(name = "entity_out_of_sync")
    private boolean entityOutOfSync = false;

    /**
     * Update PolicyDefinition with the contents of the given policyDefinitionDto
     * @param policyDefinitionDto contains data for the update.
     */
    public void updateValuesWithPolicyDefinitionDto(final PolicyDefinitionDto policyDefinitionDto){
        this.setPolicyDefinitionId(policyDefinitionDto.policyDefinitionId());
        this.setPolicy(PolicyMapper.mapper.dtoToEntity(policyDefinitionDto.policy()));
        this.setDisplayName(policyDefinitionDto.displayName());
        this.setEntityOutOfSync(false);
    }

}
