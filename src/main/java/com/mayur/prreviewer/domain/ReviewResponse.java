package com.mayur.prreviewer.domain;

import java.util.List;

public record ReviewResponse(
        String summary,
        List<ReviewFinding> findings
) {
}
