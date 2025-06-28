package java.de.unistuttgart.stayinsync.pollingnode.entities;

public class PollingJob {

    private final String apiAddress;

    public PollingJob(final String apiAddress){
        this.apiAddress = apiAddress;
    }


    public String getApiAddress() {
        return apiAddress;
    }


}
