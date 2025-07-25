package de.unistuttgart.stayinsync.core.configuration.rest;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ArcTestRequestDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ArcTestResponseDTO;
import de.unistuttgart.stayinsync.core.configuration.service.ArcTestService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/config/arc-test-call")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArcTestResource {

    @Inject
    ArcTestService testCallService;

    @POST
    @Blocking
    public Uni<ArcTestResponseDTO> doTestCall(ArcTestRequestDTO request) {
        return testCallService.performTestCall(request);
    }
}
