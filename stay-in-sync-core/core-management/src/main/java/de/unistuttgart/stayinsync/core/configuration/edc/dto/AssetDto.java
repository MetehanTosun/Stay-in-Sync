package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * DataTransferObject for EDC Asset communication.
 * Represents an asset in the Eclipse Dataspace Connector format.
 *
 * @param context JSON-LD context, always set to default EDC context
 * @param type Type identifier, always "Asset"
 * @param id Unique identifier for the asset (corresponds to asset_id in database)
 * @param properties Asset metadata (name, description, etc.)
 * @param dataAddress Configuration for accessing the actual data
 */
public record AssetDto(
        @JsonProperty("@context")
        ContextDto context,
        @JsonProperty("@type")
        String type,
        @JsonProperty("@id")
        String id,
        AssetPropertiesDto properties,
        AssetDataAddressDto dataAddress,
        @JsonView(VisibilitySidesForDto.Ui.class)
        Boolean thirdPartyChanges
) {
}
