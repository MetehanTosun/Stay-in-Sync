package de.unistuttgart.stayinsync.syncnode.domain;

import lombok.Data;

@Data
public class UpsertDirective {
    private String __directiveType;
    private ApiCallConfiguration checkConfiguration;
    private ApiCallConfiguration createConfiguration;
    private ApiCallConfiguration updateConfiguration;
}
