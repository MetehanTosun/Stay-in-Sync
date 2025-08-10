package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public class EDCAccessPolicyDto {
    private UUID id;

    @NotNull
    private UUID edcAssetId;

    @NotNull
    private Set<EDCAccessPolicyPermissionDto> permissions;

    public UUID getId() {
        return id;
    }
    public EDCAccessPolicyDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getEdcAssetId() {
        return edcAssetId;
    }
    public EDCAccessPolicyDto setEdcAssetId(UUID edcAssetId) {
        this.edcAssetId = edcAssetId;
        return this;
    }

    public Set<EDCAccessPolicyPermissionDto> getPermissions() {
        return permissions;
    }
    public EDCAccessPolicyDto setPermissions(Set<EDCAccessPolicyPermissionDto> permissions) {
        this.permissions = permissions;
        return this;
    }
}
