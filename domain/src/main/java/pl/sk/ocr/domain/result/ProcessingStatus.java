package pl.sk.ocr.domain.result;

import java.util.Collection;

public enum ProcessingStatus {
    SUCCESS,
    WARNING,
    FAILED,
    FATAL;

    public static ProcessingStatus aggregate(Collection<ProcessingStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return SUCCESS;
        }
        if (statuses.contains(FATAL)) {
            return FATAL;
        }
        if (statuses.contains(FAILED)) {
            return FAILED;
        }
        if (statuses.contains(WARNING)) {
            return WARNING;
        }
        return SUCCESS;
    }
}
