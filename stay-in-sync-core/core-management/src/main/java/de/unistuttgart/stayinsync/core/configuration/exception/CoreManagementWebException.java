package de.unistuttgart.stayinsync.core.configuration.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * This Class is supposed to be used for handling errors upon web requests
 */
public class CoreManagementWebException extends WebApplicationException {

    /**
     * Constructs an {@link CoreManagementWebException]
     *
     * @param status  {@link Response.Status} of current request
     * @param title   short and descriptive title of the occurred error
     * @param message preferably user-friendly message containing more detailed information on the error
     */
    public CoreManagementWebException(Response.Status status, String title, String message) {
        super(buildResponse(title, message, status));
    }

    /**
     * Constucts an {@link CoreManagementWebException]
     *
     * @param status  {@link Response.Status} of current request
     * @param title   short and descriptive title of the occurred error
     * @param message preferably user-friendly message containing more detailed information on the error
     * @param args    message params
     */
    public CoreManagementWebException(Response.Status status, String title, String message, Object... args) {
        super(buildResponse(title, message, status, args));
    }

    public CoreManagementWebException(int i, String string, String string2, Long endpointId) {
        //TODO Auto-generated constructor stub
    }

    /**
     * Used to build response body
     */
    private record ErrorResponse(String errorTitle, String errorMessage) {
    }

    /**
     * Builds a Response Object for {@link WebApplicationException}
     *
     * @return {@link Response}
     */
    private static Response buildResponse(String title, String message, Response.Status status, Object... args) {
        return Response //
                .status(status) //
                .entity(new ErrorResponse(title, String.format(message, args))) //
                .type(MediaType.APPLICATION_JSON) //
                .build();
    }

    /**
     * Builds a Response Object for {@link WebApplicationException}
     *
     * @return {@link Response}
     */
    private static Response buildResponse(String title, String message, Response.Status status) {
        return Response //
                .status(status) //
                .entity(new ErrorResponse(title, message)) //
                .type(MediaType.APPLICATION_JSON) //
                .build();
    }


}
