package java.de.unistuttgart.stayinsync.pollingnode.entities;

public class SyncJob {
    final private String apiAddress;

    public SyncJob(final String apiAddress){
        super();
        this.apiAddress = apiAddress;
    }

    public String getApiAddress(){
        return apiAddress;
    }
}
