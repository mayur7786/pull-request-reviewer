package com.mayur.prreviewer.domain;

import java.io.Serializable;

public record ReviewFinding(
        Severity severity,
        ReviewCategory category,
        String file,
        int line,
        String issue,
        String evidence,
        String suggestion,
        double confidence
) implements Serializable {
}
