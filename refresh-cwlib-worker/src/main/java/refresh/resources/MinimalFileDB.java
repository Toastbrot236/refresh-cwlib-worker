package refresh.resources;

import java.util.ArrayList;
import java.util.Iterator;

import cwlib.types.databases.FileDB;
import cwlib.types.databases.FileDBRow;

public class MinimalFileDB {
    public ArrayList<MinimalFileDBRow> rows = new ArrayList<>();

    public MinimalFileDB(FileDB old) {
        Iterator<FileDBRow> rowIterator = old.iterator();
        
        while (rowIterator.hasNext()) {
            this.rows.add(new MinimalFileDBRow(rowIterator.next()));
        }
    }
}
