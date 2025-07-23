import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import objects.BlobObject;
import objects.CommitObject;
import utils.CommandHandler;
import utils.ObjectLoader;
import utils.ReferenceManager;

public class MergeAndSaveTest {

    private final Path testRepoPath = Paths.get("test_repo_merge");
    private Path originalUserDir;

    @BeforeEach
    public void setup() throws IOException {
        originalUserDir = Paths.get(System.getProperty("user.dir"));
        Files.createDirectories(testRepoPath);
        System.setProperty("user.dir", testRepoPath.toAbsolutePath().toString());
        CommandHandler.handleInit();
    }

    @AfterEach
    public void teardown() throws IOException {
        System.setProperty("user.dir", originalUserDir.toAbsolutePath().toString());
        if (Files.exists(testRepoPath)) {
            Files.walk(testRepoPath)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }

    private void createFile(String path, String content) throws IOException {
        Files.write(Paths.get(path), content.getBytes());
    }

    @Test
    public void testNewObjectSavingMechanism() throws IOException {
        createFile("test_blob.txt", "This is a test blob.");
        BlobObject blob = new BlobObject("test_blob.txt");
        String blobSha = blob.getSha1();
        
        CommandHandler.handleAdd("test_blob.txt");
        CommandHandler.handleCommit("Test commit for saving");

        ReferenceManager refManager = new ReferenceManager();
        String commitSha = refManager.getHeadCommit();
        CommitObject commit = ObjectLoader.loadCommit(commitSha);
        String treeSha = commit.getTreeSha1();

        Path blobPath = Paths.get(".lit", "objects", blobSha.substring(0, 2), blobSha.substring(2));
        assertTrue(Files.exists(blobPath), "Blob object should be saved in its SHA-1 subdirectory.");

        Path treePath = Paths.get(".lit", "objects", treeSha.substring(0, 2), treeSha.substring(2));
        assertTrue(Files.exists(treePath), "Tree object should be saved in its SHA-1 subdirectory.");
        
        Path commitPath = Paths.get(".lit", "objects", commitSha.substring(0, 2), commitSha.substring(2));
        assertTrue(Files.exists(commitPath), "Commit object should be saved in its SHA-1 subdirectory.");
    }

    @Test
    public void testMergeCommitWithTwoParents() throws IOException {
        // 1. Make an initial commit on the main branch
        createFile("main.txt", "Content for main branch.");
        CommandHandler.handleAdd("main.txt");
        CommandHandler.handleCommit("Commit on main");
        ReferenceManager refManager = new ReferenceManager();
        String mainCommitSha = refManager.getHeadCommit();
        assertNotNull(mainCommitSha, "First commit on main should succeed.");

        // 2. Create and switch to a new feature branch
        String featureBranch = "feature-branch";
        CommandHandler.handleBranch(featureBranch);
        CommandHandler.handleSwitch(featureBranch);
        
        // 3. Make a new commit on the feature branch
        createFile("feature.txt", "Content for feature branch.");
        CommandHandler.handleAdd("feature.txt");
        CommandHandler.handleCommit("Commit on feature");
        String featureCommitSha = refManager.getHeadCommit();
        assertNotEquals(mainCommitSha, featureCommitSha, "Feature branch should have a new commit.");

        // 4. Switch back to main branch
        CommandHandler.handleSwitch("main");
        
        // 5. Perform the merge
        String mergeMessage = "Merging feature-branch into main";
        CommandHandler.handleMergeCommit(mergeMessage, featureBranch);

        // 6. Verify the new merge commit
        String mergeCommitSha = refManager.getHeadCommit();
        CommitObject mergeCommit = ObjectLoader.loadCommit(mergeCommitSha);
        
        assertEquals(2, mergeCommit.getParentSha1s().size(), "Merge commit must have exactly two parents.");
        assertTrue(mergeCommit.getParentSha1s().contains(mainCommitSha), "Parent list should include the main commit SHA.");
        assertTrue(mergeCommit.getParentSha1s().contains(featureCommitSha), "Parent list should include the feature commit SHA.");
    }
}
