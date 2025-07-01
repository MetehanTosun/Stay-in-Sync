package de.unistuttgart.stayinsync.core.configuration.rest;

final class Examples {

    public static final String VALID_ENDPOINT_PARAM_POST = """
            {
              "paramName": "userId",
              "queryParamType": "QUERY",
              "values": ["123", "456", "789"]
            }
                """;
    public static final String VALID_API_HEADER_POST = """
            {
              "name": "getUserA", 
              "used": "false", 
              "pollingIntervallInMs": "3"
            }
            """;
    public static final String VALID_API_HEADER_VALUE = """
            """;
    public static final String VALID_ENDPOINT_PARAM_VALUE_POST = """
            """;
    public static final String VALID_EXAMPLE_REQUEST_CONFIGURATION_CREATE = """
             {
              "name": "getUserA", 
              "used": "false", 
              "pollingIntervallInMs": "3"
            }
            """;
    public static final String VALID_EXAMPLE_ENDPOINT_CREATE = """
            [{
              "endpointPath": "/test/user", 
              "httpRequestType": "GET"
            },
            {
              "endpointPath": "/test/car", 
              "httpRequestType": "POST"
            }]
            """;
    public static final String VALID_QUERY_PARAM_CREATE = """
            {
              "paramName": "userId",
              "queryParamType": "QUERY",
              "values": ["123", "456", "789"]
            }""";

    private Examples() {

    }

    static final String VALID_EXAMPLE_SYNCJOB = """
            {
              "id": "1",
              "name": "Basyx sync job", 
              "deployed": false
            }
            """;

    static final String VALID_EXAMPLE_SYNCJOB_TO_CREATE = """
            {
              "name": "Basic Userdata sync",
              "deployed": true
            }
            """;

    static final String VALID_SOURCE_SYSTEM_CREATE = """
            {"name": "Sync Produktion A","apiUrl": "http://localhost:4200", "description": "this is my simple api", "apiType": "REST"}
            """;

    static final String VALID_SOURCE_SYSTEM_ENDPOINT_POST = """
            {
              "endpointPath": "/test",
              "httpRequestType": "GET"
            }
            """;

}
