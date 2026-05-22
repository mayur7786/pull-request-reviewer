package com.mayur.prreviewer.service;

import com.mayur.prreviewer.domain.ChangedFile;
import java.util.List;

public class JavaFileClassifierService {

    // Filters changed files down to Java sources.
    public List<ChangedFile> classify(List<ChangedFile> files) {
        return files.stream()
                .filter(file -> file.path().endsWith(".java"))
                .toList();
    }
}
