package de.unistuttgart.stayinsync.core.syncnode.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TransformJob(
        String name,
        String jobId,
        String scriptId,
        String scriptCode,
        String scriptLanguage,
        String expectedHash,
        Object sourceData
) { }
