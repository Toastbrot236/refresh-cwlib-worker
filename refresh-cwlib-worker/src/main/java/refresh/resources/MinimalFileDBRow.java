package refresh.resources;

import cwlib.types.data.GUID;
import cwlib.types.data.SHA1;
import cwlib.types.databases.FileDBRow;

public class MinimalFileDBRow {
    public String path;
    public long size;
    public GUID guid;
    public SHA1 hash;
    public long date;

    public MinimalFileDBRow(FileDBRow old) {
        this.path = old.getPath();
        this.size = old.getSize();
        this.guid = old.getGUID();
        this.hash = old.getSHA1();
        this.date = old.getDate();
    }
}
