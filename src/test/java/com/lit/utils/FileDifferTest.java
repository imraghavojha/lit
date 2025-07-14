package com.lit.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("FileDiffer Tests")
class FileDifferTest {

    private FileDiffer differ;

    @BeforeEach
    void setUp() {
        differ = new FileDiffer();
    }

    @Test
    @DisplayName("Should detect no changes for identical files")
    void testNoChanges() {
        String contentA = "line 1\nline 2\nline 3";
        DiffResult result = differ.calculateDiff(contentA, contentA);
        assertFalse(result.hasChanges(), "Should report no changes for identical files.");
        assertEquals(0, result.getDiffLines().size(), "DiffLines list should be empty.");
    }

    @Test
    @DisplayName("Should handle one file being empty")
    void testOneFileEmpty() {
        String contentA = "line 1\nline 2";
        String contentB = "";
        DiffResult result = differ.calculateDiff(contentA, contentB);
        assertEquals(2, result.getDiffLines().size());
        assertEquals(ChangeType.DELETED, result.getDiffLines().get(0).type);
    }

    @Test
    @DisplayName("Should handle both files being empty")
    void testBothFilesEmpty() {
        DiffResult result = differ.calculateDiff("", "");
        assertFalse(result.hasChanges());
    }

    @Test
    @DisplayName("Should detect an addition at the end")
    void testAdditionAtEnd() {
        String contentA = "line 1\nline 2";
        String contentB = "line 1\nline 2\nline 3";
        DiffResult result = differ.calculateDiff(contentA, contentB);
        assertEquals(1, result.getDiffLines().size());
        assertEquals(ChangeType.ADDED, result.getDiffLines().get(0).type);
        assertEquals("line 3", result.getDiffLines().get(0).text);
    }

    @Test
    @DisplayName("Should detect a deletion from the beginning")
    void testDeletionAtStart() {
        String contentA = "line 1\nline 2\nline 3";
        String contentB = "line 2\nline 3";
        DiffResult result = differ.calculateDiff(contentA, contentB);
        assertEquals(1, result.getDiffLines().size());
        assertEquals(ChangeType.DELETED, result.getDiffLines().get(0).type);
        assertEquals("line 1", result.getDiffLines().get(0).text);
    }

    @Test
    @DisplayName("Should detect a simple line change")
    void testSimpleChange() {
        String contentA = "apple\nbanana\ncherry";
        String contentB = "apple\nblueberry\ncherry";
        DiffResult result = differ.calculateDiff(contentA, contentB);
        List<DiffLine> diffs = result.getDiffLines();

        assertEquals(2, diffs.size(), "A change should be one deletion and one addition.");
        assertEquals(ChangeType.DELETED, diffs.get(0).type);
        assertEquals("banana", diffs.get(0).text);
        assertEquals(ChangeType.ADDED, diffs.get(1).type);
        assertEquals("blueberry", diffs.get(1).text);
    }

    @Test
    @DisplayName("Should detect multiple, non-consecutive changes")
    void testMultipleChanges() {
        String contentA = "line 1 (original)\nline 2\nline 3 (to be deleted)\nline 4";
        String contentB = "line 1 (changed)\nline 2\nline 4\nline 5 (added)";
        DiffResult result = differ.calculateDiff(contentA, contentB);
        List<DiffLine> diffs = result.getDiffLines();

        assertEquals(4, diffs.size(), "Should detect all 4 changes.");

        // Change on line 1
        assertEquals(ChangeType.DELETED, diffs.get(0).type);
        assertEquals("line 1 (original)", diffs.get(0).text);
        assertEquals(ChangeType.ADDED, diffs.get(1).type);
        assertEquals("line 1 (changed)", diffs.get(1).text);

        // Deletion of line 3
        assertEquals(ChangeType.DELETED, diffs.get(2).type);
        assertEquals("line 3 (to be deleted)", diffs.get(2).text);

        // Addition of line 5
        assertEquals(ChangeType.ADDED, diffs.get(3).type);
        assertEquals("line 5 (added)", diffs.get(3).text);
    }
}