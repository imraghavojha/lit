import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        // this class creates a CommandLine instance with Lit
        int exitCode = new CommandLine(new Lit()).execute(args);
        System.exit(exitCode);
    }
}