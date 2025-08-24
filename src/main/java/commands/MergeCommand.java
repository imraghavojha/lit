package commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import utils.CommandHandler;
import utils.MergeResult;
import utils.MergeUtils;
import utils.ReferenceManager;

@Command(
    name = "merge",
    description = "Join two or more development histories together."
)
public class MergeCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The branch to merge into the current branch.")
    private String branchName;

    @Option(names = {"-m", "--message"}, required = true, description = "Commit message for the merge commit.")
    private String message;

    @Override
    public Integer call() throws Exception {
        ReferenceManager refManager = new ReferenceManager();

        // Get the commit SHA for the branch we want to merge.
        String otherCommitSha = refManager.getBranchCommit(branchName);
        if (otherCommitSha == null) {
            System.err.println("Error: Branch '" + branchName + "' not found.");
            return 1; // non-zero exit code for failure
        }

        // Get the commit SHA of the current branch (HEAD).
        String headCommitSha = refManager.getHeadCommit();
        if (headCommitSha == null) {
            System.err.println("Error: Cannot merge into a branch with no commits.");
            return 1;
        }

        System.out.println("Attempting to merge branch '" + branchName + "' into HEAD...");
        MergeResult result = MergeUtils.merge(headCommitSha, otherCommitSha, branchName);

        if (!result.isSuccess()) {
            System.err.println("\nAutomatic merge failed; fix conflicts and then commit the result.");
            System.err.println("Conflicting files:");
            for (String conflictedFile : result.getConflictedFiles()) {
                System.err.println("  " + conflictedFile);
            }
            return 1; 
        }
        
        // Handle the case of a fast-forward merge where no new commit is needed.
        if (headCommitSha.equals(MergeUtils.findCommonAncestor(headCommitSha, otherCommitSha))) {
            System.out.println("Fast-forwarding...");
            refManager.updateHead(otherCommitSha);
            CommandHandler.handleSwitch(branchName);
            System.out.println("Updated current branch to commit " + otherCommitSha);
            return 0;
        }


        // create the merge commit if there are no conflicts (successful merge)
        System.out.println("Merge successful. Creating merge commit.");
        CommandHandler.handleMergeCommit(message, branchName);

        return 0; 
    }
}