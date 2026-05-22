package com.mayur.prreviewer.domain;

import java.io.Serializable;
import java.util.List;

public record ChangedFile(
        String path,
        ChangedFileType type,
        List<DiffHunk> hunks,
        int additions,
        int deletions
) implements Serializable {
}
