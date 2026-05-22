package com.mayur.prreviewer.domain;

import java.io.Serializable;
import java.util.List;

public record ReviewContext(
        String file,
        ChangedFileType fileType,
        String className,
        String methodName,
        List<String> imports,
        int startLine,
        int endLine,
        String snippet,
        boolean generatedCode
) implements Serializable {
}
