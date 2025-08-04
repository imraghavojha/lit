import commands.AddCommand;
import commands.InitCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command; 

@Command(
    name = "lit",
    mixinStandardHelpOptions = true,
    version = "Lit 1.0",
    description = "A custom version control system built in Java.",
    // commands
    subcommands = {
        InitCommand.class,
        AddCommand.class
        // more to be added here
    }
)
public class Lit implements Runnable {
    // ... rest of the class is unchanged ...
    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }
}