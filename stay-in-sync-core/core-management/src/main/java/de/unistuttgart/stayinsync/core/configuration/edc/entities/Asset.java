package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.AssetDataAddressMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.mapping.AssetPropertiesMapper;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity-Klasse für EDC-Assets (Eclipse Dataspace Connector).
 * Repräsentiert ein Asset, das über den EDC bereitgestellt werden kann.
 */
@Getter
@Setter
@Entity
@Table(name = "edc_asset")
public class Asset extends PanacheEntity {

    @NotBlank
    @Column(name = "asset_id", nullable = false)
    private String assetId;

    @Column(name = "type")
    private String type;

    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "data_address_id", nullable = false)
    private AssetDataAddress dataAddress;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "properties_id")
    private AssetProperties properties;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_edc_id", nullable = false)
    private EDCInstance targetEDC;

    @Column(name = "third_party_changes")
    private boolean thirdPartyChanges = false;

    /**
     * Update asset with the contents of the given assetDto
     * @param assetDto contains data for the update.
     */
    public void updateValuesWithAssetDto(final AssetDto assetDto){
        this.setAssetId(assetDto.id());
        this.setType(assetDto.type());
        this.setProperties(AssetPropertiesMapper.mapper.dtoToEntity(assetDto.properties()));
        this.setDataAddress(AssetDataAddressMapper.mapper.dtoToEntity(assetDto.dataAddress()));
    }
}