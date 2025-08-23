import commands.AddCommand;
import commands.BranchCommand;
import commands.CommitCommand;
import commands.InitCommand;
import commands.LogCommand;
import commands.MergeCommand;
import commands.RmCommand;
import commands.StatusCommand;
import commands.SwitchCommand;
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
        AddCommand.class,
        CommitCommand.class,
        BranchCommand.class,
        SwitchCommand.class,
        RmCommand.class,
        MergeCommand.class,
        StatusCommand.class,
        LogCommand.class
    }
)
public class Lit implements Runnable {
    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }
}