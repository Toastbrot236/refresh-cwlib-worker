package refresh.resources;

import cwlib.types.archives.Fat;
import cwlib.types.data.SHA1;

public class MinimalFat {
    public SHA1 hash;
    public long offset;
    public long size;

    public MinimalFat(Fat old) {
        this.hash = old.getSHA1();
        this.offset = old.getOffset();
        this.size = old.getSize();
    }
}
