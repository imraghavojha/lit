package commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import utils.CommandHandler;

@Command(
    name = "add",
    description = "Add file contents to the index."
)
public class AddCommand implements Callable<Integer> {

    @Parameters(
        index = "0", // The first parameter after the command name
        description = "The path to the file to add."
    )
    private String filePath; 

    @Override
    public Integer call() throws Exception {
        // calling the existing method with the filePath from user
        CommandHandler.handleAdd(filePath);
        return 0; 
    }
}