package refresh.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import refresh.database.GameDatabaseContext;
import refresh.database.models.PersistentJobState;
import refresh.workers.cwlib.jobs.TestJob;
import refresh.workers.cwlib.jobs.CompleteAdventureDataJob;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WorkerManager {
    private static final Logger logger = LogManager.getLogger();

    private final int _workerId;

    private Thread _thread = null;
    private boolean _threadShouldRun = false;

    private long _lastContactUpdate = System.currentTimeMillis();

    private final List<WorkerJob> _jobs = new ArrayList<>();

    private final ObjectMapper _mapper = new ObjectMapper();

    public WorkerManager() throws SQLException {
        try(GameDatabaseContext context = new GameDatabaseContext()) {
            this._workerId = context.createWorker();
            logger.debug("Assigned worker id: {}", this._workerId);
        }

        this._jobs.add(new TestJob());
        this._jobs.add(new CompleteAdventureDataJob());
    }

    private void runWorkCycle() throws Exception {
        try(WorkContext context = new WorkContext()) {
            for (WorkerJob job : this._jobs) {
                PersistentJobState jobState = context.Database.getJobState(job.getJobId());

                if(jobState == null) {
                    logger.debug("Job {} has no state in the db, waiting for Refresh to give us stuff to work on...", job.getJobId());
                    continue;
                }

                job.jobState = this._mapper.readValue(jobState.State, job.getJobStateType());

                job.executeJob(context);
            }


            long now = System.currentTimeMillis();
            if(now - this._lastContactUpdate < 5000) return;

            this._lastContactUpdate = now;
            boolean updated = context.Database.markWorkerContacted(this._workerId);

            if(!updated) {
                logger.info("Worker is shutting down as we've been replaced.");
                this.stop(false);
            }
        }
    }

    public void start() {
        logger.debug("Starting the worker thread");
        this._threadShouldRun = true;

        Thread thread = new Thread(() -> {
            while(this._threadShouldRun) {
                try {
                    Thread.sleep(1000);
                    this.runWorkCycle();
                }
                catch(Exception e) {
                    logger.fatal("Critical exception while running work cycle: {}", String.valueOf(e));
                    logger.fatal("Waiting for 1 second before trying to run another cycle.");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        thread.start();

        this._thread = thread;
    }

    public void stop(boolean join) throws InterruptedException {
        if(this._thread == null) return;
        logger.debug("Stopping the worker thread");

        this._threadShouldRun = false;
        if(join)
            waitForExit();
    }

    public void waitForExit() throws InterruptedException {
        this._thread.join();
    }
}
