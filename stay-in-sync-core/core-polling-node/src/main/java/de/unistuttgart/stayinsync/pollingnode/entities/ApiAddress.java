package de.unistuttgart.stayinsync.pollingnode.entities;

public class ApiAddress {
    private final String apiAddress;

    public ApiAddress(String apiAddress) {
        super();
        this.apiAddress = apiAddress;
    }

    public String getString() {
        return apiAddress;
    }
}
