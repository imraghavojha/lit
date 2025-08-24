package commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import utils.CommandHandler;

@Command(name = "switch", description = "Switch branches or restore working tree files.")
public class SwitchCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The branch or commit to switch to.")
    private String targetRef;

    @Override
    public Integer call() throws Exception {
        CommandHandler.handleSwitch(targetRef);
        return 0;
    }
}