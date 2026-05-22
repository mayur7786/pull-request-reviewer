package com.mayur.prreviewer.domain;

import java.io.Serializable;
import java.util.List;

public record DiffHunk(
        int oldStart,
        int oldCount,
        int newStart,
        int newCount,
        List<String> lines
) implements Serializable {
}
