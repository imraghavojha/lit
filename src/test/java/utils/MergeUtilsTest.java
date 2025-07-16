package utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import objects.CommitObject;

public class MergeUtilsTest {
    @BeforeEach
    public void setup() throws Exception {
        cleanup(); // Ensure a clean state before every test
        CommandHandler.handleInit();
    }

    // This method runs AFTER each @Test method.
    @AfterEach
    public void teardown() throws IOException {
        cleanup();
    }

    @Test
    public void testFindCommonAncestor() throws Exception {
        // C1
        Files.writeString(Paths.get("file1.txt"), "C1");
        CommandHandler.handleAdd("file1.txt");
        CommandHandler.handleCommit("C1");

        // C2 (The expected ancestor)
        Files.writeString(Paths.get("file1.txt"), "C2");
        CommandHandler.handleAdd("file1.txt");
        CommandHandler.handleCommit("C2");
        String expectedAncestorSha = new ReferenceManager().getHeadCommit();

        // C3 (on main)
        Files.writeString(Paths.get("file1.txt"), "C3");
        CommandHandler.handleAdd("file1.txt");
        CommandHandler.handleCommit("C3");
        String mainBranchSha = new ReferenceManager().getHeadCommit();

        // C4 (on new 'feature' branch)
        CommandHandler.handleSwitch(expectedAncestorSha); // Go back to C2
        CommandHandler.handleBranch("feature");
        CommandHandler.handleSwitch("feature");
        Files.writeString(Paths.get("file2.txt"), "C4");
        CommandHandler.handleAdd("file2.txt");
        CommandHandler.handleCommit("C4");
        String featureBranchSha = new ReferenceManager().getHeadCommit();

        // execute the method
        String actualAncestorSha = MergeUtils.findCommonAncestor(mainBranchSha, featureBranchSha);

        // verify if it finds the expected common ancestor
        assertEquals(expectedAncestorSha, actualAncestorSha, "The common ancestor should be C2.");
    }

    @Test
    public void testDiffTrees() throws Exception {
        // Base Commit
        Files.writeString(Paths.get("original.txt"), "original");
        Files.writeString(Paths.get("to_be_deleted.txt"), "delete me");
        CommandHandler.handleAdd("original.txt");
        CommandHandler.handleAdd("to_be_deleted.txt");
        CommandHandler.handleCommit("Base commit");
        String baseCommitSha = new ReferenceManager().getHeadCommit();
        CommitObject baseCommit = ObjectLoader.loadCommit(baseCommitSha);
        String baseTreeSha = baseCommit.getTreeSha1();

        // New Commit
        Files.writeString(Paths.get("original.txt"), "modified"); // Modified
        Files.delete(Paths.get("to_be_deleted.txt")); // Deleted
        Files.writeString(Paths.get("added.txt"), "new file"); // Added
        CommandHandler.handleAdd("original.txt");
        CommandHandler.handleAdd("added.txt");
        CommandHandler.handleCommit("New commit");
        String newCommitSha = new ReferenceManager().getHeadCommit();
        CommitObject newCommit = ObjectLoader.loadCommit(newCommitSha);
        String newTreeSha = newCommit.getTreeSha1();

        // execute the method
        TreeDiffResult result = MergeUtils.diffTrees(baseTreeSha, newTreeSha);

        // verify for all different functionalities
        assertEquals(1, result.getAddedFiles().size(), "Should be 1 added file.");
        assertEquals("added.txt", result.getAddedFiles().get(0).getName(), "The added file should be 'added.txt'.");

        assertEquals(1, result.getDeletedFiles().size(), "Should be 1 deleted file.");
        assertEquals("to_be_deleted.txt", result.getDeletedFiles().get(0).getName(), "The deleted file should be 'to_be_deleted.txt'.");

        assertEquals(1, result.getModifiedFiles().size(), "Should be 1 modified file.");
        assertEquals("original.txt", result.getModifiedFiles().get(0).getName(), "The modified file should be 'original.txt'.");
    }

    private void cleanup() throws IOException {
        Path litDir = Paths.get(".lit");
        if (Files.exists(litDir)) {
            Files.walk(litDir)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
        Files.deleteIfExists(Paths.get("file1.txt"));
        Files.deleteIfExists(Paths.get("file2.txt"));
        Files.deleteIfExists(Paths.get("original.txt"));
        Files.deleteIfExists(Paths.get("to_be_deleted.txt"));
        Files.deleteIfExists(Paths.get("added.txt"));
    }
}