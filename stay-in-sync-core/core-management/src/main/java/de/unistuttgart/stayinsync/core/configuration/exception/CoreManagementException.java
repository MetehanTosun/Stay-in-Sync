package de.unistuttgart.stayinsync.core.configuration.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * This Class is supposed to be used for handling errors within the core-management
 */
public class CoreManagementException extends RuntimeException {

    private final String title;

    private Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;

    private Object[] args = {};

    /**
     * Constructs an {@link CoreManagementException]
     *
     * @param status  {@link Response.Status} of current request
     * @param title   short and descriptive title of the occurred error
     * @param message preferably user-friendly message containing more detailed information on the error
     */
    public CoreManagementException(Response.Status status, String title, String message) {
        super(message);
        this.title = title;
        this.status = status;
    }

    /**
     * Constucts an {@link CoreManagementException}
     *
     * @param status  {@link Response.Status} of current request
     * @param title   short and descriptive title of the occurred error
     * @param message preferably user-friendly message containing more detailed information on the error
     * @param args    message params
     */
    public CoreManagementException(Response.Status status, String title, String message, Object... args) {
        super(String.format(message, args));
        this.title = title;
        this.status = status;
    }

    /**
     * Constucts an {@link CoreManagementException}
     *
     * @param title   short and descriptive title of the occurred error
     * @param message preferably user-friendly message containing more detailed information on the error
     */
    public CoreManagementException(String title, String message) {
        super(message);
        this.title = title;
        this.status = status;
    }

    /**
     * Constucts an {@link CoreManagementException}
     *
     * @param title   short and descriptive title of the occurred error
     * @param message preferably user-friendly message containing more detailed information on the error
     * @param args    message params
     */
    public CoreManagementException(String title, String message, Object... args) {
        super(String.format(message, args));
        this.title = title;
        this.status = status;
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
    public Response buildResponse() {
        return Response //
                .status(this.status) //
                .entity(new ErrorResponse(this.title, this.getMessage())) //
                .type(MediaType.APPLICATION_JSON) //
                .build();
    }

}
