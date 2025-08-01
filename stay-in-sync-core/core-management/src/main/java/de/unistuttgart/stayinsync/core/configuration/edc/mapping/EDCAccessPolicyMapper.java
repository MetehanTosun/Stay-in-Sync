package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAccessPolicy;
import de.unistuttgart.stayinsync.core.configuration.edc.EDCAccessPolicyPermission;
import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAccessPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAccessPolicyPermissionDto;

import java.util.Set;
import java.util.stream.Collectors;

public class EDCAccessPolicyMapper {

    public static EDCAccessPolicyDto toDto(EDCAccessPolicy e) {
        if (e == null) {
            return null;
        }
        return new EDCAccessPolicyDto()
            .setId(e.id)
            .setEdcAssetId(e.getEdcAsset().id)
            .setPermissions(e.getAccessPolicyPermissions().stream()
                .map(EDCAccessPolicyMapper::permissionToDto)
                .collect(Collectors.toSet()));
    }

    private static EDCAccessPolicyPermissionDto permissionToDto(EDCAccessPolicyPermission p) {
        return new EDCAccessPolicyPermissionDto()
            .setId(p.id)
            .setAction(p.getAction())
            .setConstraintLeftOperand(p.getConstraintLeftOperand())
            .setConstraintOperator(p.getConstraintOperator())
            .setConstraintRightOperand(p.getConstraintRightOperand());
    }

    public static EDCAccessPolicy fromDto(EDCAccessPolicyDto dto) {
        if (dto == null) {
            return null;
        }
        // Asset muss existieren
        EDCAsset asset = EDCAsset.findById(dto.getEdcAssetId());
        if (asset == null) {
            throw new IllegalArgumentException("EDCAsset " + dto.getEdcAssetId() + " nicht gefunden");
        }

        EDCAccessPolicy entity = new EDCAccessPolicy();
        entity.setEdcAsset(asset);

        // Permissions anlegen
        Set<EDCAccessPolicyPermission> perms = dto.getPermissions().stream()
            .map(permDto -> {
                EDCAccessPolicyPermission p = new EDCAccessPolicyPermission();
                p.setAction(permDto.getAction());
                p.setConstraintLeftOperand(permDto.getConstraintLeftOperand());
                p.setConstraintOperator(permDto.getConstraintOperator());
                p.setConstraintRightOperand(permDto.getConstraintRightOperand());
                return p;
            })
            .collect(Collectors.toSet());
        entity.setAccessPolicyPermissions(perms);

        return entity;
    }
}

