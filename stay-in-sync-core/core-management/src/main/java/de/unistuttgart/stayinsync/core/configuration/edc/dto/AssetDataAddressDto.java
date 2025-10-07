package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DataTransferObject for Edc communication that sets the DataAddress with which an asset is created.
 * The DataAddress will be used by the edc to communicate with the referenced RestApi and therefore needs
 * to contain all important information to write requests to RestApis.
 *
 * @param queryParams a list of queryParams is represented as "param1=value1&param2=value2&..."
 * @param headers contains all additional header fields with their values and is automatically initialised with standard headers.
 */
public record AssetDataAddressDto(
        @JsonProperty("@type")
        String atType,
        @NotBlank
        String type,
        @NotBlank
        String baseUrl,
        String path,
        String queryParams,
        @JsonAnyGetter
        Map<String, Object> headers,
        Boolean proxyPath,
        Boolean proxyQueryParams,
        Boolean proxyMethod,
        Boolean proxyBody
) {
    /**
     * Adds a header field and value to the data address.
     *
     * @param field the header field name (without "header:" prefix)
     * @param value the header value
     */
    public void addHeader(String field, String value) {
        if (headers != null) {
            headers.put("header:" + field, value);
        }
    }
}