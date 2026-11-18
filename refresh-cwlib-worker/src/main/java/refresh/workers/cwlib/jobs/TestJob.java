package refresh.workers.cwlib.jobs;

import cwlib.types.SerializedResource;
import cwlib.types.data.Revision;
import refresh.workers.WorkContext;
import refresh.workers.WorkerJob;
import refresh.workers.cwlib.state.AssetListState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestJob extends WorkerJob {
    public TestJob() {
        this.isJobStateRequired = true;
    }

    @Override
    protected Class<?> getJobStateType() {
        return AssetListState.class;
    }

    @Override
    public void executeJob(WorkContext context) throws IOException {
        AssetListState state = (AssetListState)this.jobState;
        for (String asset : state.Assets) {
            File file = new File("X:\\Refresh\\Refresh.GameServer\\bin\\Debug\\net9.0\\dataStore\\" + asset);

            if(!file.exists())
                continue;

            byte[] data = Files.readAllBytes(file.toPath());
            SerializedResource resource = new SerializedResource(data);
            Revision revision = resource.getRevision();

            System.out.println("Revision: " + revision.getVersion());
        }
    }
}
