package de.unistuttgart.stayinsync.core.configuration.edc.dto;

/**
 * Can be used to control which attributes are sent out on edc side and Ui side.
 */
public class VisibilitySidesForDto {
    /**
     * View for Ui communication
     */
    public static class Ui {}

    /**
     * View for Edc
     */
    public static class Edc {}
}
