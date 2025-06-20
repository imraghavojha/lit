package utils;

import java.io.IOException;
import java.nio.file.*;


public class CommandHandler {
    
    public static void handleInit(String command){
        if (command.equals("init")){
            Path litPath = Paths.get ("sample/.lit");
            Path objectsPath = Paths.get("sample/.lit/objects");
            Path refPath = Paths.get("sample/.lit/refs/heads");
            Path HEADPath = Paths.get("sample/.lit/HEAD");
            Path mainPath = Paths.get("sample/.lit/refs/heads/main");
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
