package com.lit.utils;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections; 
import java.util.List;

/*
 * Takes two strings as input (the original and new file contents)
 * Then uses java-diff-utils library to do comparison
 * Library returns Patch object which is its own way of representing changes
 * For loop iterates through Patch object and translates it into DiffLines object (my own simplified obj)
 * Returns DiffResult which is basially bundled up object with bunch of DiffLines
 */

public class FileDiffer {

    public DiffResult calculateDiff(String originalContent, String revisedContent) {
        //handle empty strings explicitly to avoid the .split() issue.
        List<String> originalLines = originalContent.isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(originalContent.split("\n"));

        List<String> revisedLines = revisedContent.isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(revisedContent.split("\n"));

        Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);
        List<DiffLine> diffLines = new ArrayList<>();

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            switch (delta.getType()) {
                case INSERT:
                    delta.getTarget().getLines().forEach(line -> diffLines.add(new DiffLine(ChangeType.ADDED, line)));
                    break;
                case DELETE:
                    delta.getSource().getLines().forEach(line -> diffLines.add(new DiffLine(ChangeType.DELETED, line)));
                    break;
                case CHANGE:
                    delta.getSource().getLines().forEach(line -> diffLines.add(new DiffLine(ChangeType.DELETED, line)));
                    delta.getTarget().getLines().forEach(line -> diffLines.add(new DiffLine(ChangeType.ADDED, line)));
                    break;
                default:
                    break;
            }
        }
        return new DiffResult(diffLines);
    }
}