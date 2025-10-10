package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.util.List;

@Entity
public class ApiEndpointQueryParamValue extends PanacheEntity {

    @ManyToOne
    public ApiRequestConfiguration requestConfiguration;

    @ManyToOne
    public ApiEndpointQueryParam queryParam;

    public String selectedValue;


    public static List<ApiEndpointQueryParamValue> findRequestHeadersByConfigurationId(Long requestConfigurationId) {
        return ApiEndpointQueryParamValue.list("requestConfiguration.id", requestConfigurationId);
    }

}
