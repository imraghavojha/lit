package commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import utils.CommandHandler;

@Command(
    name = "log",
    description = "Show commit history."
)
public class LogCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        CommandHandler.handleLog();
        return 0;
    }
}