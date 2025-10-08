package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.AssetDataAddress;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapstruct Mapper that maps AssetDataAddress entity to AssetDataAddressDto.
 */
public interface AssetDataAddressMapper {

    AssetDataAddressMapper mapper = Mappers.getMapper(AssetDataAddressMapper.class);

    @Mapping(target = "atType", constant = "DataAddress")
    @Mapping(target = "type", constant = "HttpData")
    @Mapping(source = "headers", target = "headers", qualifiedByName = "stringToHeadersMap")
    AssetDataAddressDto entityToDto(AssetDataAddress entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", constant = "HttpData")
    @Mapping(source = "headers", target = "headers", qualifiedByName = "headersMapToString")
    AssetDataAddress dtoToEntity(AssetDataAddressDto dto);

     /*@
    @ ensures String is parsed in a Map that is compatible with the asset format expected of the Edc.
    @ requires String in Format field1=value1;field2=value2;field3=value3;...
     */
    /**
     * Parses a semicolon-separated headers string into a map with "header:" prefix.
     * Standard Accept = application/json is automatically added.
     *
     * @param headersString the headers string to parse
     * @return a map with header keys prefixed with "header:"
     */
    @Named("stringToHeadersMap")
    default Map<String, Object> stringToHeadersMap(String headersString) {
        Map<String, Object> headers = new HashMap<>();

        headers.put("header:Accept", "application/json");

        if (headersString == null || headersString.trim().isEmpty()) {
            return headers;
        }

        final String[] splitHeaders = headersString.split(";");
        for (String singleHeader : splitHeaders) {
            final String trimmedSingleHeader = singleHeader.trim();
            if (trimmedSingleHeader.isEmpty()) {
                continue;
            }
            int equalCharacterIndex = trimmedSingleHeader.indexOf('=');
            if (equalCharacterIndex > 0 && equalCharacterIndex < trimmedSingleHeader.length() - 1) {
                final String field = trimmedSingleHeader.substring(0, equalCharacterIndex).trim();
                final String value = trimmedSingleHeader.substring(equalCharacterIndex + 1).trim();
                headers.put("header:" + field, value);
            }
        }
        return headers;
    }

    /**
     * Convert Headers Map back to semicolon seperated String, also removing the standard Accept=application/json header.
     *
     * @param headersMap Map in which all keys have "header:" prefix.
     * @return String in format "field1=value1;field2=value2;..."
     */
    @Named("headersMapToString")
    default String headersMapToString(Map<String, Object> headersMap) {
        if (headersMap == null || headersMap.isEmpty()) {
            return "";
        }
        final StringBuilder stringBuilder = new StringBuilder();
        headersMap.forEach((key, value) -> {
            if (key.startsWith("header:")) {
                String field = key.substring("header:".length());
                if (!"Accept".equals(field) || !"application/json".equals(value)) {
                    if (!stringBuilder.isEmpty()) {
                        stringBuilder.append(";");
                    }
                    stringBuilder.append(field).append("=").append(value);
                }
            }
        });

        return stringBuilder.toString();
    }
}

