package fr.sncf.osrd.api;

public enum CacheStatus {
    INITIALIZING(false),
    DOWNLOADING(false),
    PARSING_JSON(false),
    PARSING_MODEL(false),
    CACHED(true),
    // errors that are known to be temporary
    TRANSIENT_ERROR(false),
    ERROR(true);

    static {
        INITIALIZING.transitions = new CacheStatus[] { DOWNLOADING };
        DOWNLOADING.transitions = new CacheStatus[] { PARSING_JSON, ERROR, TRANSIENT_ERROR };
        PARSING_JSON.transitions = new CacheStatus[] { PARSING_MODEL, ERROR, TRANSIENT_ERROR };
        PARSING_MODEL.transitions = new CacheStatus[] { CACHED, ERROR, TRANSIENT_ERROR };
        // if a new version appears
        CACHED.transitions = new CacheStatus[] { DOWNLOADING };
        // at the next try
        TRANSIENT_ERROR.transitions = new CacheStatus[] { DOWNLOADING };
        // if a new version appears
        ERROR.transitions = new CacheStatus[] { DOWNLOADING };
    }

    private CacheStatus(boolean isStable) {
        this.isStable = isStable;
    }

    public final boolean isStable;
    private CacheStatus[] transitions;

    boolean canTransitionTo(CacheStatus newStatus) {
        for (var status : transitions)
            if (status == newStatus)
                return true;
        return false;
    }
}
