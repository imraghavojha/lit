package commands;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import utils.CommandHandler;

@Command(
    name = "diff",
    description = "Show changes between commits, commit and working tree, etc."
)
public class DiffCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..2", description = "The commit(s) or file to diff.")
    private String[] params = {};

    @Override
    public Integer call() throws Exception {
        if (params.length == 0) {
            CommandHandler.handleDiffIndexAndWorkingDir();
        } else if (params.length == 1) {
            CommandHandler.handleDiffCommitAndWorkingDir(params[0]);
        } else if (params.length == 2) {
            CommandHandler.handleDiffCommits(params[0], params[1]);
        } else {
            System.err.println("Error: Invalid number of arguments for 'diff' command.");
            return 1;
        }
        return 0;
    }
}
