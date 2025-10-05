package de.unistuttgart.stayinsync.core.configuration.service;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.rest.client.SyncedAssetsClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;

@ApplicationScoped
public class SyncedAssetsService {


    @Inject
    PodResolverService podResolverService;

    @Inject
    TransformationService transformationService;

    public void retrieveSyncedAssets(Long id) {
        Transformation transformation = transformationService.findByIdDirect(id);

        String workerHostName = transformation.workerHostName;
        String workerAddress = podResolverService.resolvePodName(workerHostName);

        RestClientBuilder.newBuilder()
                .baseUri(URI.create(workerAddress))
                .build(SyncedAssetsClient.class);
    }
}
