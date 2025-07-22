package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConflictHandlerTest {
    
    @BeforeEach
    public void setup() throws Exception {
        cleanup();
        CommandHandler.handleInit();
    }
    
    @AfterEach
    public void teardown() throws IOException {
        cleanup();
    }
    
    @Test
    @DisplayName("Test merge conflict when both branches modify the same file")
    public void testModifyModifyConflict() throws Exception {
        // Create base commit
        Files.writeString(Paths.get("conflict.txt"), "Original content\n");
        CommandHandler.handleAdd("conflict.txt");
        CommandHandler.handleCommit("Base commit");
        String baseCommitSha = new ReferenceManager().getHeadCommit();
        
        // Modify on main branch
        Files.writeString(Paths.get("conflict.txt"), "Modified by main\n");
        CommandHandler.handleAdd("conflict.txt");
        CommandHandler.handleCommit("Main modification");
        String mainCommitSha = new ReferenceManager().getHeadCommit();
        
        // Create feature branch from base and modify differently
        CommandHandler.handleSwitch(baseCommitSha);
        CommandHandler.handleBranch("feature");
        CommandHandler.handleSwitch("feature");
        Files.writeString(Paths.get("conflict.txt"), "Modified by feature\n");
        CommandHandler.handleAdd("conflict.txt");
        CommandHandler.handleCommit("Feature modification");
        String featureCommitSha = new ReferenceManager().getHeadCommit();
        
        // Switch back to main and merge
        CommandHandler.handleSwitch("main");
        MergeResult result = MergeUtils.merge(mainCommitSha, featureCommitSha, "feature");
        
        // Verify conflict detected
        assertFalse(result.isSuccess(), "Merge should have conflicts");
        assertEquals(1, result.getConflictedFiles().size(), "Should have 1 conflicted file");
        assertEquals("conflict.txt", result.getConflictedFiles().get(0));
        
        // Verify conflict markers in file
        String fileContent = Files.readString(Paths.get("conflict.txt"));
        assertTrue(fileContent.contains("<<<<<<< HEAD"), "Should have HEAD marker");
        assertTrue(fileContent.contains("======="), "Should have separator");
        assertTrue(fileContent.contains(">>>>>>> feature"), "Should have feature marker");
        assertTrue(fileContent.contains("Modified by main"), "Should have main content");
        assertTrue(fileContent.contains("Modified by feature"), "Should have feature content");
    }
    
    @Test
    @DisplayName("Test merge conflict when one branch modifies and other deletes")
    public void testModifyDeleteConflict() throws Exception {
        // Create base commit
        Files.writeString(Paths.get("delete-conflict.txt"), "File to be deleted\n");
        CommandHandler.handleAdd("delete-conflict.txt");
        CommandHandler.handleCommit("Base commit");
        String baseCommitSha = new ReferenceManager().getHeadCommit();
        
        // Modify on main branch
        Files.writeString(Paths.get("delete-conflict.txt"), "Modified content\n");
        CommandHandler.handleAdd("delete-conflict.txt");
        CommandHandler.handleCommit("Main modification");
        String mainCommitSha = new ReferenceManager().getHeadCommit();
        
        // Delete on feature branch
        CommandHandler.handleSwitch(baseCommitSha);
        CommandHandler.handleBranch("feature-delete");
        CommandHandler.handleSwitch("feature-delete");
        Files.delete(Paths.get("delete-conflict.txt"));
        // Need to remove from index when file is deleted
        IndexManager indexManager = new IndexManager();
        indexManager.removeEntry("delete-conflict.txt");
        indexManager.writeIndex();
        CommandHandler.handleCommit("Feature deletion");
        String featureCommitSha = new ReferenceManager().getHeadCommit();
        
        // Switch back to main and merge
        CommandHandler.handleSwitch("main");
        MergeResult result = MergeUtils.merge(mainCommitSha, featureCommitSha, "feature-delete");
        
        // Verify conflict detected
        assertFalse(result.isSuccess(), "Merge should have conflicts");
        assertEquals(1, result.getConflictedFiles().size());
        
        // File should exist with conflict markers
        assertTrue(Files.exists(Paths.get("delete-conflict.txt")), "File should exist");
        String fileContent = Files.readString(Paths.get("delete-conflict.txt"));
        assertTrue(fileContent.contains("<<<<<<< HEAD"), "Should have HEAD marker");
        assertTrue(fileContent.contains("Modified content"), "Should have main content");
        assertTrue(fileContent.contains(">>>>>>> feature-delete"), "Should have branch marker");
    }
    
    @Test
    @DisplayName("Test merge conflict when both branches add the same file differently")
    public void testAddAddConflict() throws Exception {
        // Create base commit
        Files.writeString(Paths.get("base.txt"), "Base file\n");
        CommandHandler.handleAdd("base.txt");
        CommandHandler.handleCommit("Base commit");
        String baseCommitSha = new ReferenceManager().getHeadCommit();
        
        // Add new file on main branch
        Files.writeString(Paths.get("new-file.txt"), "Added by main\n");
        CommandHandler.handleAdd("new-file.txt");
        CommandHandler.handleCommit("Main addition");
        String mainCommitSha = new ReferenceManager().getHeadCommit();
        
        // Add same file with different content on feature branch
        CommandHandler.handleSwitch(baseCommitSha);
        CommandHandler.handleBranch("feature-add");
        CommandHandler.handleSwitch("feature-add");
        Files.writeString(Paths.get("new-file.txt"), "Added by feature\n");
        CommandHandler.handleAdd("new-file.txt");
        CommandHandler.handleCommit("Feature addition");
        String featureCommitSha = new ReferenceManager().getHeadCommit();
        
        // Switch back to main and merge
        CommandHandler.handleSwitch("main");
        MergeResult result = MergeUtils.merge(mainCommitSha, featureCommitSha, "feature-add");
        
        // Verify conflict detected
        assertFalse(result.isSuccess(), "Merge should have conflicts");
        assertEquals(1, result.getConflictedFiles().size());
        assertEquals("new-file.txt", result.getConflictedFiles().get(0));
        
        // Verify conflict markers
        String fileContent = Files.readString(Paths.get("new-file.txt"));
        assertTrue(fileContent.contains("Added by main"), "Should have main content");
        assertTrue(fileContent.contains("Added by feature"), "Should have feature content");
    }
    
    @Test
    @DisplayName("Test merge with multiple conflicts")
    public void testMultipleConflicts() throws Exception {
        // Create base commit
        Files.writeString(Paths.get("file1.txt"), "Original 1\n");
        Files.writeString(Paths.get("file2.txt"), "Original 2\n");
        CommandHandler.handleAdd("file1.txt");
        CommandHandler.handleAdd("file2.txt");
        CommandHandler.handleCommit("Base commit");
        String baseCommitSha = new ReferenceManager().getHeadCommit();
        
        // Modify both files on main
        Files.writeString(Paths.get("file1.txt"), "Main 1\n");
        Files.writeString(Paths.get("file2.txt"), "Main 2\n");
        CommandHandler.handleAdd("file1.txt");
        CommandHandler.handleAdd("file2.txt");
        CommandHandler.handleCommit("Main modifications");
        String mainCommitSha = new ReferenceManager().getHeadCommit();
        
        // Modify both files differently on feature
        CommandHandler.handleSwitch(baseCommitSha);
        CommandHandler.handleBranch("feature-multi");
        CommandHandler.handleSwitch("feature-multi");
        Files.writeString(Paths.get("file1.txt"), "Feature 1\n");
        Files.writeString(Paths.get("file2.txt"), "Feature 2\n");
        CommandHandler.handleAdd("file1.txt");
        CommandHandler.handleAdd("file2.txt");
        CommandHandler.handleCommit("Feature modifications");
        String featureCommitSha = new ReferenceManager().getHeadCommit();
        
        // Switch back to main and merge
        CommandHandler.handleSwitch("main");
        MergeResult result = MergeUtils.merge(mainCommitSha, featureCommitSha, "feature-multi");
        
        // Verify both conflicts detected
        assertFalse(result.isSuccess(), "Merge should have conflicts");
        assertEquals(2, result.getConflictedFiles().size(), "Should have 2 conflicted files");
        assertTrue(result.getConflictedFiles().contains("file1.txt"));
        assertTrue(result.getConflictedFiles().contains("file2.txt"));
        
        // Both files should have conflict markers
        String file1Content = Files.readString(Paths.get("file1.txt"));
        assertTrue(file1Content.contains("<<<<<<< HEAD"));
        assertTrue(file1Content.contains("Main 1"));
        assertTrue(file1Content.contains("Feature 1"));
        
        String file2Content = Files.readString(Paths.get("file2.txt"));
        assertTrue(file2Content.contains("<<<<<<< HEAD"));
        assertTrue(file2Content.contains("Main 2"));
        assertTrue(file2Content.contains("Feature 2"));
    }
    
    @Test
    @DisplayName("Test merge with conflicts and non-conflicts mixed")
    public void testMixedConflictAndCleanMerge() throws Exception {
        // Create base commit
        Files.writeString(Paths.get("conflict.txt"), "Original\n");
        Files.writeString(Paths.get("clean.txt"), "Clean original\n");
        CommandHandler.handleAdd("conflict.txt");
        CommandHandler.handleAdd("clean.txt");
        CommandHandler.handleCommit("Base commit");
        String baseCommitSha = new ReferenceManager().getHeadCommit();
        
        // Modify conflict.txt on main, leave clean.txt unchanged
        Files.writeString(Paths.get("conflict.txt"), "Main version\n");
        CommandHandler.handleAdd("conflict.txt");
        CommandHandler.handleCommit("Main modification");
        String mainCommitSha = new ReferenceManager().getHeadCommit();
        
        // On feature: modify conflict.txt differently, modify clean.txt
        CommandHandler.handleSwitch(baseCommitSha);
        CommandHandler.handleBranch("feature-mixed");
        CommandHandler.handleSwitch("feature-mixed");
        Files.writeString(Paths.get("conflict.txt"), "Feature version\n");
        Files.writeString(Paths.get("clean.txt"), "Clean modified by feature\n");
        Files.writeString(Paths.get("feature-only.txt"), "Feature only file\n");
        CommandHandler.handleAdd("conflict.txt");
        CommandHandler.handleAdd("clean.txt");
        CommandHandler.handleAdd("feature-only.txt");
        CommandHandler.handleCommit("Feature modifications");
        String featureCommitSha = new ReferenceManager().getHeadCommit();
        
        // Switch back to main and merge
        CommandHandler.handleSwitch("main");
        MergeResult result = MergeUtils.merge(mainCommitSha, featureCommitSha, "feature-mixed");
        
        // Verify only one conflict
        assertFalse(result.isSuccess(), "Merge should have conflicts");
        assertEquals(1, result.getConflictedFiles().size(), "Should have 1 conflict");
        assertEquals("conflict.txt", result.getConflictedFiles().get(0));
        
        // Verify conflict file has markers
        String conflictContent = Files.readString(Paths.get("conflict.txt"));
        assertTrue(conflictContent.contains("<<<<<<< HEAD"));
        
        // Verify clean merge worked
        assertTrue(Files.exists(Paths.get("clean.txt")));
        assertEquals("Clean modified by feature\n", Files.readString(Paths.get("clean.txt")));
        
        // Verify added file exists
        assertTrue(Files.exists(Paths.get("feature-only.txt")));
        assertEquals("Feature only file\n", Files.readString(Paths.get("feature-only.txt")));
    }
    
    private void cleanup() throws IOException {
        Path litDir = Paths.get(".lit");
        if (Files.exists(litDir)) {
            Files.walk(litDir)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
        
        // Clean up test files
        Files.deleteIfExists(Paths.get("conflict.txt"));
        Files.deleteIfExists(Paths.get("delete-conflict.txt"));
        Files.deleteIfExists(Paths.get("new-file.txt"));
        Files.deleteIfExists(Paths.get("base.txt"));
        Files.deleteIfExists(Paths.get("file1.txt"));
        Files.deleteIfExists(Paths.get("file2.txt"));
        Files.deleteIfExists(Paths.get("clean.txt"));
        Files.deleteIfExists(Paths.get("feature-only.txt"));
    }
}