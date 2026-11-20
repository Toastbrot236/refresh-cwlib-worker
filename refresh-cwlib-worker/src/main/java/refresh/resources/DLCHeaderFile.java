package refresh.resources;
import cwlib.enums.Revisions;
import cwlib.io.Serializable;
import cwlib.io.serializer.Serializer;
import cwlib.io.streams.MemoryOutputStream;
import cwlib.types.data.ResourceDescriptor;
import cwlib.types.data.SHA1;
import cwlib.types.data.Revision;

public class DLCHeaderFile implements Serializable {
    public long Revision; // what exactly is this?
    public long BranchDescription; // what exactly is this?
    public SHA1[] TranslationTables = new SHA1[20]; // indexed by GameLanguage, hashes of RTranslationTables used?
    public SHA1 DatabaseHash; // map hash?
    public ResourceDescriptor Resource; // the "main" resource (pin, pck or dlc)?

    public void serialize(Serializer serializer) {
        this.Revision = serializer.u32(this.Revision);
        this.BranchDescription = serializer.u32(this.BranchDescription);
        serializer.u32(0); // unused and unknown

        // TranslationTables is CRawVector<CHash>, so length-prefix, and then the hashes?
        // Serializer.array() only accepts classes implementing Serializable, and neither String not SHA1 do that,
        // so serialize it manually.
        long translationTablesLength = serializer.u32(this.TranslationTables.length);

        for (int i = 0; i < translationTablesLength; i++) {
            this.TranslationTables[i] = serializer.sha1(this.TranslationTables[i]);
        }
        
        this.DatabaseHash = serializer.sha1(this.DatabaseHash);
        this.Resource = serializer.resource(this.Resource, this.Resource.getType());
    }

    public byte[] build() {
        MemoryOutputStream output = new MemoryOutputStream(this.getAllocatedSize());
        Serializer serializer = new Serializer(output, new Revision(Revisions.LBP2_MAX));
        this.serialize(serializer);
        return output.getBuffer();
    }

    public int getAllocatedSize() {
        return 80 + (this.TranslationTables.length * 20);
    }
}
