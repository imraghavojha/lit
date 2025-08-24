package commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import utils.CommandHandler;

@Command(name = "commit", description = "Record changes to the repository.")
public class CommitCommand implements Callable<Integer> {

    @Option(names = {"-m", "--message"}, required = true, description = "The commit message.")
    private String message;

    @Override
    public Integer call() throws Exception {
        CommandHandler.handleCommit(message);
        return 0;
    }
}