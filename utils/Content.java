package utils;
import java.io.IOException;
import java.nio.file.*;

public class Content {

    public static byte[] fileToByte(String filePath) throws IOException{
        Path path = Paths.get(filePath);
        
        return Files.readAllBytes(path);
    }
}