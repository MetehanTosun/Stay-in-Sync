package de.unistuttgart.stayinsync.monitoring.core.configuration.rest;

final class Examples {
    private Examples() {

    }

    static final String VALID_EXAMPLE_SYNCJOB = """
            {
              "id": "1",
              "name": "Sync Produktion A"
            }
            """;

    static final String VALID_EXAMPLE_SYNCJOB_TO_CREATE = """
            {
              "name": "Sync Produktion A",
              "deployed": true
            }
            """;

}
