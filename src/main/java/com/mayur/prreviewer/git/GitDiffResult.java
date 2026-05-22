package com.mayur.prreviewer.git;

import com.mayur.prreviewer.domain.ChangedFile;
import java.util.List;

public record GitDiffResult(
        String currentBranch,
        List<String> commitMessages,
        List<ChangedFile> changedFiles,
        String diffSummary
) {
}
