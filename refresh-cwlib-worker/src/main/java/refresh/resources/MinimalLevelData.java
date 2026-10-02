package refresh.resources;

import java.util.ArrayList;

import cwlib.enums.Part;
import cwlib.resources.RLevel;
import cwlib.types.data.NetworkPlayerID;

public class MinimalLevelData {
    public ArrayList<String> ContributorUsernames = new ArrayList<>();
    public boolean HasValidWorldThing;
    // TODO thing data, whether this has valid planets etc

    public MinimalLevelData(RLevel level) {
        for (NetworkPlayerID playerID : level.playerRecord.getPlayerIDs()) {
            String username = playerID.toString();

            // fake users which always appear in all user levels (latter one in all LBP2+ user levels).
            if (username.equals("OfflinePlayer") || username.equals("gumbahmoo")) {
                continue;
            }

            this.ContributorUsernames.add(username);
        }

        this.HasValidWorldThing = level.worldThing != null
            && level.worldThing.hasPart(Part.WORLD);
    }
}
