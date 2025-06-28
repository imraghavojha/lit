package utils;

import java.io.IOException;
import java.nio.file.*;


public class CommandHandler {
    
    public static void handleInit(String command){
        if (command.equals("init")){
            Path currentDirectory = Paths.get("");
            Path litPath = currentDirectory.resolve(".lit");
            Path objectsPath = litPath.resolve("objects");
            Path refPath = litPath.resolve("refs/heads");
            Path HEADPath = litPath.resolve("HEAD");
            Path mainPath = refPath.resolve("main");
            try {
                // creating .lit directory and subdirectories
                Files.createDirectory(litPath);
                
                Files.createDirectory(objectsPath);

                Files.createDirectories(refPath);

                Files.createFile(HEADPath);

                Files.createFile(mainPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
