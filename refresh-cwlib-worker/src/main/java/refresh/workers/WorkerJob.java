package refresh.workers;

public abstract class WorkerJob {
    public final String getJobId() {
        return this.getClass().getSimpleName();
    }

    protected abstract Class<?> getJobStateType();

    protected boolean isJobStateRequired = false;
    protected Object jobState;

    public void setJobState(Object jobState) {
        if(getJobStateType() != jobState.getClass())
            throw new RuntimeException("Instantiated invalid type");
        this.jobState = jobState;
    }

    public abstract void executeJob(WorkContext context) throws Exception;
}
