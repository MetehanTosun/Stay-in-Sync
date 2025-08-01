package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCProperty;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;

public class EDCPropertyMapper {

    public static EDCPropertyDto toDto(EDCProperty e) {
        if (e == null) return null;
        return new EDCPropertyDto()
            .setId(e.id)
            .setDescription(e.description);
    }

    public static EDCProperty fromDto(EDCPropertyDto dto) {
        if (dto == null) return null;
        var e = new EDCProperty();
        // nur setzen, wenn dto.getId() nicht null: für Updates
        if (dto.getId() != null) {
            e = EDCProperty.findById(dto.getId());
            if (e == null) {
                throw new IllegalArgumentException("EDCProperty " + dto.getId() + " nicht gefunden");
            }
        }
        e.description = dto.getDescription();
        return e;
    }
}
