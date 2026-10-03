package refresh.resources;

import java.util.ArrayList;

public class MinimalResourceList {
    public int SuccessfulDatabaseInsertionCount; // how many assets have succeeded insertion
    public int FailedDatabaseInsertionCount; // how many assets have failed insertion
    public ArrayList<MinimalResource<MinimalLevelData>> Levels = new ArrayList<>();
    public ArrayList<MinimalResource<MinimalPlanData>> Plans = new ArrayList<>();

    public MinimalResourceList() {
        
    }
}
