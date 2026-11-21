package refresh.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import cwlib.enums.ArchiveType;
import cwlib.io.streams.MemoryOutputStream;
import cwlib.types.archives.Fart;
import cwlib.types.archives.Fat;
import cwlib.types.data.SHA1;

public class RemoteFart extends Fart{
    public SHA1 headerHash;

    public RemoteFart() {
        super(null, ArchiveType.SAVE);
        this.entries = new Fat[0];
    }

    public boolean save() {
        return false; // don't actually use this, use build()
    }

    public byte[] build() {
        // length is header hash size + raw data total size + fat table size + magic and length sizes
        MemoryOutputStream stream = new MemoryOutputStream((int)(0x14 + this.getQueueSize() + (this.queue.size() * 0x1c) + 0x4 + 0x4));

        // write header hash
        //stream.sha1(this.headerHash);

        ArrayList<Fat> fats = new ArrayList<>();

        // write raw data, while also generating fats
        for (Entry<SHA1, byte[]> entry : this.queue.entrySet()) {
            fats.add(new Fat(this, entry.getKey(), stream.getOffset(), entry.getValue().length));
            stream.bytes(entry.getValue());
        }

        // write fats, fat count, and magic
        this.entries = fats.toArray(new Fat[fats.size()]);
        stream.bytes(generateFAT(this.entries));
        System.out.println("fat table length " + this.entries.length);

        stream.u32(this.entries.length);
        stream.bytes("FARC".getBytes());

        return stream.getBuffer();
    }
}
