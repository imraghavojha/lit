package utils;

import java.io.IOException;
import java.nio.file.*;


public class CommandHandler {
    
    public static void handleInit(String command){
        if (command.equals("init")){
            Path litPath = Paths.get ("sample/.lit");
            try {
                
                Files.createDirectory(litPath);
                System.out.println("Yay! Succesfully created .lit/!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
