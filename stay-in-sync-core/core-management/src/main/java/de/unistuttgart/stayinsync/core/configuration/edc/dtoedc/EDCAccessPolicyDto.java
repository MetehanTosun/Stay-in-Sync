package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public class EDCAccessPolicyDto {
    private Long id;

    @NotNull
    private Long edcAssetId;

    @NotNull
    private Set<EDCAccessPolicyPermissionDto> permissions;

    public Long getId() {
        return id;
    }
    public EDCAccessPolicyDto setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getEdcAssetId() {
        return edcAssetId;
    }
    public EDCAccessPolicyDto setEdcAssetId(Long edcAssetId) {
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
