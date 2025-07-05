package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.ApiHeader;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-04T13:15:39+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.7 (Homebrew)"
)
@ApplicationScoped
public class SourceSystemFullUpdateMapperImpl implements SourceSystemFullUpdateMapper {

    @Inject
    private AuthConfigMapper authConfigMapper;

    @Override
    public void mapFullUpdate(SourceSystem input, SourceSystem target) {
        if ( input == null ) {
            return;
        }

        target.name = input.name;
        target.apiUrl = input.apiUrl;
        target.description = input.description;
        target.apiType = input.apiType;
        byte[] openApiSpec = input.openApiSpec;
        if ( openApiSpec != null ) {
            target.openApiSpec = Arrays.copyOf( openApiSpec, openApiSpec.length );
        }
        else {
            target.openApiSpec = null;
        }
        if ( target.syncSystemEndpoints != null ) {
            Set<SyncSystemEndpoint> set = input.syncSystemEndpoints;
            if ( set != null ) {
                target.syncSystemEndpoints.clear();
                target.syncSystemEndpoints.addAll( set );
            }
            else {
                target.syncSystemEndpoints = null;
            }
        }
        else {
            Set<SyncSystemEndpoint> set = input.syncSystemEndpoints;
            if ( set != null ) {
                target.syncSystemEndpoints = new LinkedHashSet<SyncSystemEndpoint>( set );
            }
        }
        if ( target.apiRequestHeaders != null ) {
            Set<ApiHeader> set1 = input.apiRequestHeaders;
            if ( set1 != null ) {
                target.apiRequestHeaders.clear();
                target.apiRequestHeaders.addAll( set1 );
            }
            else {
                target.apiRequestHeaders = null;
            }
        }
        else {
            Set<ApiHeader> set1 = input.apiRequestHeaders;
            if ( set1 != null ) {
                target.apiRequestHeaders = new LinkedHashSet<ApiHeader>( set1 );
            }
        }
        target.authConfig = input.authConfig;
        if ( target.sourceSystemEndpoints != null ) {
            Set<SourceSystemEndpoint> set2 = input.sourceSystemEndpoints;
            if ( set2 != null ) {
                target.sourceSystemEndpoints.clear();
                target.sourceSystemEndpoints.addAll( set2 );
            }
            else {
                target.sourceSystemEndpoints = null;
            }
        }
        else {
            Set<SourceSystemEndpoint> set2 = input.sourceSystemEndpoints;
            if ( set2 != null ) {
                target.sourceSystemEndpoints = new LinkedHashSet<SourceSystemEndpoint>( set2 );
            }
        }
        if ( target.sourceSystemApiRequestConfigurations != null ) {
            Set<SourceSystemApiRequestConfiguration> set3 = input.sourceSystemApiRequestConfigurations;
            if ( set3 != null ) {
                target.sourceSystemApiRequestConfigurations.clear();
                target.sourceSystemApiRequestConfigurations.addAll( set3 );
            }
            else {
                target.sourceSystemApiRequestConfigurations = null;
            }
        }
        else {
            Set<SourceSystemApiRequestConfiguration> set3 = input.sourceSystemApiRequestConfigurations;
            if ( set3 != null ) {
                target.sourceSystemApiRequestConfigurations = new LinkedHashSet<SourceSystemApiRequestConfiguration>( set3 );
            }
        }
    }

    @Override
    public SourceSystemDTO mapToDTO(SourceSystem input) {
        if ( input == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String apiUrl = null;
        String description = null;
        String apiType = null;
        byte[] openApiSpec = null;

        id = input.id;
        name = input.name;
        apiUrl = input.apiUrl;
        description = input.description;
        apiType = input.apiType;
        byte[] openApiSpec1 = input.openApiSpec;
        if ( openApiSpec1 != null ) {
            openApiSpec = Arrays.copyOf( openApiSpec1, openApiSpec1.length );
        }

        SourceSystemDTO sourceSystemDTO = new SourceSystemDTO( id, name, apiUrl, description, apiType, openApiSpec );

        return sourceSystemDTO;
    }

    @Override
    public SourceSystem mapToEntity(SourceSystemDTO input) {
        if ( input == null ) {
            return null;
        }

        SourceSystem sourceSystem = new SourceSystem();

        sourceSystem.name = input.name();
        sourceSystem.apiUrl = input.apiUrl();
        sourceSystem.description = input.description();
        sourceSystem.apiType = input.apiType();
        byte[] openApiSpec = input.openApiSpec();
        if ( openApiSpec != null ) {
            sourceSystem.openApiSpec = Arrays.copyOf( openApiSpec, openApiSpec.length );
        }

        return sourceSystem;
    }

    @Override
    public List<SourceSystemDTO> mapToDTOList(List<SourceSystem> input) {
        if ( input == null ) {
            return null;
        }

        List<SourceSystemDTO> list = new ArrayList<SourceSystemDTO>( input.size() );
        for ( SourceSystem sourceSystem : input ) {
            list.add( mapToDTO( sourceSystem ) );
        }

        return list;
    }

    @Override
    public SourceSystem mapToEntity(CreateSourceSystemDTO sourceSystemDTO) {
        if ( sourceSystemDTO == null ) {
            return null;
        }

        SourceSystem sourceSystem = new SourceSystem();

        sourceSystem.authConfig = authConfigMapper.mapToEntity( sourceSystemDTO.authConfig() );
        sourceSystem.name = sourceSystemDTO.name();
        sourceSystem.apiUrl = sourceSystemDTO.apiUrl();
        sourceSystem.description = sourceSystemDTO.description();
        sourceSystem.apiType = sourceSystemDTO.apiType();
        byte[] openApiSpec = sourceSystemDTO.openApiSpec();
        if ( openApiSpec != null ) {
            sourceSystem.openApiSpec = Arrays.copyOf( openApiSpec, openApiSpec.length );
        }

        return sourceSystem;
    }
}
