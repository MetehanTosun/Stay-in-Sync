package de.unistuttgart.stayinsync.syncnode.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.TransformationResultDTO;

public final class TransformationResultMapper {
    private TransformationResultMapper() {
    }

    public static TransformationResultDTO toDTO(TransformationResult src, ObjectMapper om) {
        var dto = new TransformationResultDTO();
        dto.setTransformationId(src.getTransofrmationId());
        dto.setJobId(src.getJobId());
        dto.setScriptId(src.getScriptId());
        dto.setValidExecution(src.isValidExecution());

        // null-safe conversion of arbitrary objects to JSON
        dto.setSourceData(om.valueToTree(src.getSourceData()));
        dto.setOutputData(om.valueToTree(src.getOutputData()));

        dto.setErrorInfo(src.getErrorInfo());

        return dto;
    }
}
