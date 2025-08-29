package de.unistuttgart.stayinsync.core.configuration.util;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@IfBuildProfile("dev")
@ApplicationScoped
@Path("/dev/seed")
public class DevelopmentSeederResource {

    @Inject
    DevelopmentDataSeeder seeder;

    @POST
    public Response runSeeder() {
        Log.info("====== TRIGGERING DATABASE SEEDING VIA REST ENDPOINT ======");
        try {
            seeder.seedDatabase();
            return Response.ok("Database seeding successful.").build();
        } catch (Exception e) {
            Log.error("Database seeding failed", e);
            return Response.serverError().entity("Database seeding failed: " + e.getMessage()).build();
        }
    }
}