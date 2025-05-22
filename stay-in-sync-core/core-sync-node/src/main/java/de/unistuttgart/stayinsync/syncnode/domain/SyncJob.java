package de.unistuttgart.stayinsync.syncnode.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SyncJob(String name) {

}
