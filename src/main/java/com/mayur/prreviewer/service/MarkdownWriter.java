package com.mayur.prreviewer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarkdownWriter {

    // Writes the markdown report to the target file.
    public void write(Path outputPath, String markdown) {
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, markdown);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write markdown report to " + outputPath, exception);
        }
    }
}
