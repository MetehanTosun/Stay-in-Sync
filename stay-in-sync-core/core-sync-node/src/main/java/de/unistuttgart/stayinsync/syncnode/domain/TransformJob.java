package de.unistuttgart.stayinsync.syncnode.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TransformJob(
                Long transformationId,
                String name,
                String jobId,
                String scriptId,
                String scriptCode,
                String scriptLanguage,
                String expectedHash,
                String generatedSdkCode,
                String generatedSdkHash,
                Object sourceData) {
}
