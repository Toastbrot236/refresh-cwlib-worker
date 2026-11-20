package refresh.resources;

import java.util.ArrayList;
import java.util.Iterator;

import cwlib.types.archives.Fart;
import cwlib.types.archives.Fat;

public class MinimalFart {
    ArrayList<MinimalFat> fats = new ArrayList<>();

    public MinimalFart(Fart old) {
        Iterator<Fat> fatIterator = old.iterator();

        while (fatIterator.hasNext()) {
            this.fats.add(new MinimalFat(fatIterator.next()));
        }
    }
}
