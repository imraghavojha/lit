package commands; 

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import utils.CommandHandler;

@Command(
    name = "init",
    description = "Create an empty Lit repository or reinitialize an existing one."
)
public class InitCommand implements Callable<Integer> {

    // The 'call' method is executed when the 'init' command is run.
    @Override
    public Integer call() throws Exception {
        // calling the existing logic from CommandHandler.
        CommandHandler.handleInit();
        return 0; 
    }
}