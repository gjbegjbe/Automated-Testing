package org.example.testCaseSelector.FileWriter;

import java.io.IOException;
import java.io.Writer;

public class FileWriter {
    public static void writeFile(String path, String content){
        try {
            Writer writer = new java.io.FileWriter(path);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
