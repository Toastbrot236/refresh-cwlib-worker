package refresh.resources;

import java.util.ArrayList;

import cwlib.resources.RPlan;
import refresh.helpers.ResourceHelper;

public class MinimalPlanData {
    public String Name;
    public String Description;
    public String Icon;
    public String CreatorUsername;
    public ArrayList<String> ContributorUsernames = new ArrayList<>();
    // TODO other metadata, like photo and thing data.

    public MinimalPlanData(RPlan plan) {
        this.Name = plan.inventoryData.userCreatedDetails.name;
        this.Description = plan.inventoryData.userCreatedDetails.description;
        this.Icon = ResourceHelper.GetAssetReference(plan.inventoryData.icon);
        this.CreatorUsername = plan.inventoryData.creator.toString();

        for (String username : plan.inventoryData.creationHistory.creators) {
            this.ContributorUsernames.add(username);
        }
    }
}
