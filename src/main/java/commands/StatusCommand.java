package commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import utils.CommandHandler;

@Command(
    name = "status",
    description = "Show the working tree status."
)
public class StatusCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        CommandHandler.handleStatus();
        return 0; 
    }
}