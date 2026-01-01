package refresh.workers.cwlib.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CompleteAdventureDataJobState {
    public List<String> AdventureRootHashes = new ArrayList<>();
    public HashMap<String, Boolean> LevelModdedRelations = new HashMap<>();
}
