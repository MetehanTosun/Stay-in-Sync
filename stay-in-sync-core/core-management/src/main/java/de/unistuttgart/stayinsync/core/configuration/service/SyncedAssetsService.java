package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.rest.client.SyncedAssetsClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SyncedAssetsService {


    @Inject
    PodResolverService podResolverService;

    @Inject
    TransformationService transformationService;

    public Map<Long, List<String>> retrieveSyncedAssets(Long id) {
        Transformation transformation = transformationService.findByIdDirect(id);

        String workerHostName = transformation.workerHostName;
        String workerAddress = podResolverService.resolvePodName(workerHostName);

        SyncedAssetsClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(workerAddress))
                .build(SyncedAssetsClient.class);

        return client.getById(id);
    }
}
