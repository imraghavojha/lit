package commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import utils.CommandHandler;

@Command(name = "branch", description = "Create a new branch.")
public class BranchCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The name of the branch to create.")
    private String branchName;

    @Override
    public Integer call() throws Exception {
        CommandHandler.handleBranch(branchName);
        return 0;
    }
}