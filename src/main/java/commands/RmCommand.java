package commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import utils.CommandHandler;

@Command(name = "rm", description = "Remove files from the working tree and from the index.")
public class RmCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The file to remove.")
    private String filePath;

    @Override
    public Integer call() throws Exception {
        CommandHandler.handleRm(filePath);
        return 0;
    }
}