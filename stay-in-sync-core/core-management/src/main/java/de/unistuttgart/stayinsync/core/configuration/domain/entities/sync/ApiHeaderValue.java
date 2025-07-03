package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.util.List;

@Entity
public class ApiHeaderValue extends PanacheEntity {

    @ManyToOne
    public ApiRequestConfiguration requestConfiguration;

    @ManyToOne
    public ApiHeader apiHeader;

    public String selectedValue;


    public static List<ApiHeaderValue> findRequestHeadersByConfigurationId(Long requestConfigurationId) {
        return ApiHeaderValue.list("requestConfiguration.id", requestConfigurationId);
    }


}
