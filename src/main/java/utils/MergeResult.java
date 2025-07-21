package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A data class to hold the results of a three-way merge operation.
 * It indicates whether the merge was successful (no conflicts) and provides a list
 * of any files that are in a conflicted state.
 */
public class MergeResult {

    private final boolean successful;
    private final List<String> conflictedFiles;

    // list of file paths that have conflicts, successful merge if the list is empty.

    public MergeResult(List<String> conflictedFiles) {
        this.conflictedFiles = conflictedFiles;
        this.successful = conflictedFiles.isEmpty();
    }

    public boolean isSuccess() {
        return successful;
    }

    public List<String> getConflictedFiles() {
        return new ArrayList<>(conflictedFiles); // returns a copy
    }
}