package refresh.server.responses;

import java.util.ArrayList;

import refresh.resources.MinimalLevelData;
import refresh.resources.MinimalPlanData;
import refresh.resources.MinimalResource;

public class ResourceDataResponse {
    public int SuccessfulDatabaseInsertionCount; // how many assets have succeeded insertion
    public int FailedDatabaseInsertionCount; // how many assets have failed insertion
    public ArrayList<MinimalResource<MinimalLevelData>> Levels = new ArrayList<>();
    public ArrayList<MinimalResource<MinimalPlanData>> Plans = new ArrayList<>();

    public ResourceDataResponse() {
        
    }
}
