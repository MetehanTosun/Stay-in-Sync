package de.unistuttgart.stayinsync.core.syncnode.domain;

public class UpsertDirective {
    private String __directiveType;
    private ApiCallConfiguration checkConfiguration;
    private ApiCallConfiguration createConfiguration;
    private ApiCallConfiguration updateConfiguration;

    public UpsertDirective() {
    }

    public UpsertDirective(String __directiveType,
                           ApiCallConfiguration checkConfiguration,
                           ApiCallConfiguration createConfiguration,
                           ApiCallConfiguration updateConfiguration) {
        this.__directiveType = __directiveType;
        this.checkConfiguration = checkConfiguration;
        this.createConfiguration = createConfiguration;
        this.updateConfiguration = updateConfiguration;
    }

    public String get__directiveType() {
        return __directiveType;
    }

    public void set__directiveType(String __directiveType) {
        this.__directiveType = __directiveType;
    }

    public ApiCallConfiguration getCheckConfiguration() {
        return checkConfiguration;
    }

    public void setCheckConfiguration(ApiCallConfiguration checkConfiguration) {
        this.checkConfiguration = checkConfiguration;
    }

    public ApiCallConfiguration getCreateConfiguration() {
        return createConfiguration;
    }

    public void setCreateConfiguration(ApiCallConfiguration createConfiguration) {
        this.createConfiguration = createConfiguration;
    }

    public ApiCallConfiguration getUpdateConfiguration() {
        return updateConfiguration;
    }

    public void setUpdateConfiguration(ApiCallConfiguration updateConfiguration) {
        this.updateConfiguration = updateConfiguration;
    }
}
