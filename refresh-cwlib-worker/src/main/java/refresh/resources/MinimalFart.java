package refresh.resources;

import java.util.ArrayList;
import java.util.Iterator;

import cwlib.enums.ResourceType;
import cwlib.types.archives.Fart;
import cwlib.types.archives.Fat;
import cwlib.types.archives.SaveKey;
import cwlib.types.data.SHA1;

public class MinimalFart {
    public ArrayList<MinimalFat> fats = new ArrayList<>();
    public SHA1 keyRootHash;
    public ResourceType keyRootType;

    public MinimalFart(Fart old, SaveKey key) {
        Iterator<Fat> fatIterator = old.iterator();

        while (fatIterator.hasNext()) {
            this.fats.add(new MinimalFat(fatIterator.next()));
        }

        if (key != null) {
            this.keyRootHash = key.getRootHash();
            this.keyRootType = key.getRootType();
        }
    }
}
