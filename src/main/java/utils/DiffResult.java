package utils; 

import java.util.List;

/**
 * class to hold the structured result of a file comparison.
 */
public class DiffResult {
    private final List<DiffLine> diffLines;

    public DiffResult(List<DiffLine> diffLines) {
        this.diffLines = diffLines;
    }

    public List<DiffLine> getDiffLines() {
        return diffLines;
    }

    public boolean hasChanges() {
        return !diffLines.isEmpty();
    }
}

/**
 * single line in a diff output, with its type (ADDED/DELETED) and text.
 */
class DiffLine {
    public final ChangeType type;
    public final String text;

    public DiffLine(ChangeType type, String text) {
        this.type = type;
        this.text = text;
    }
}