# AI Pull Request Reviewer Agent

Local Java 21 pull request reviewer built with LangGraph4j. It compares the current branch against a target branch, infers change intent, reviews grounded Java diffs with OpenAI, Gemini, Claude, or OpenAI-compatible models, and writes a Markdown report to `.review/ai-review.md`.

## What it does

- Diffs `HEAD` against a target branch using local Git
- Includes uncommitted working tree Java changes
- Falls back to `HEAD~1..HEAD` when the requested target is not a separate base branch
- Reviews only `*.java` files
- Uses full file content for new files
- Uses changed hunks plus surrounding context for modified files
- Produces structured findings with severity, category, evidence, suggestion, and confidence
- Writes a Markdown report inside the reviewed repository and prints it to stdout

## Workflow

See [workflow.mmd](/C:/mayur/pull-request-reviewer-agent/docs/workflow.mmd).

## Requirements

- Java 21+
- Git available on PATH
- A provider API key in environment variables

Common environment variables:

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL` for OpenAI-compatible providers
- `GEMINI_API_KEY`
- `ANTHROPIC_API_KEY`

## Run

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd exec:java '-Dexec.args=--target-branch main --repo C:\path\to\repo'
```

Provider examples:

```powershell

$env:OPENAI_API_KEY="..."
.\.tools\apache-maven-3.9.9\bin\mvn.cmd exec:java '-Dexec.args=--provider OPENAI --model gpt-5.2 --target-branch main --repo C:\path\to\repo'

.\.tools\apache-maven-3.9.9\bin\mvn.cmd exec:java '-Dexec.args=--provider OPENAI --model gpt-5.2 --target-branch billing-sbscription-calc --repo C:\mayur\repo --file src/main/java/com/example/subscription/dto/PriceModel.java --file src/main/java/com/example/subscription/service/SubscriptionCalculator.java'

$env:GEMINI_API_KEY="..."
.\.tools\apache-maven-3.9.9\bin\mvn.cmd exec:java '-Dexec.args=--provider GEMINI --model gemini-2.5-flash --target-branch main --repo C:\path\to\repo'
$env:ANTHROPIC_API_KEY="..."
.\.tools\apache-maven-3.9.9\bin\mvn.cmd exec:java '-Dexec.args=--provider CLAUDE --model claude-sonnet-4-0 --target-branch main --repo C:\path\to\repo'
$env:OPENAI_API_KEY="..."
.\.tools\apache-maven-3.9.9\bin\mvn.cmd exec:java '-Dexec.args=--provider OPENAI_COMPATIBLE --model my-model --api-base-url https://example.com/v1 --target-branch main --repo C:\path\to\repo'
```

Optional flags:

```text
--provider OPENAI|OPENAI_COMPATIBLE|GEMINI|CLAUDE
--model gpt-5.2
--api-base-url https://example.com/v1
--api-key-env CUSTOM_API_KEY
--output .review/ai-review.md
--developer-note "Add timeout handling for payment gateway calls."
--context-lines 20
--file src/main/java/com/example/BillingService.java
```

Relative `--output` paths are resolved inside the repository passed with `--repo`.
Repeat `--file` to review multiple specific Java files. When omitted, the reviewer considers all changed Java files in scope.
`OPENAI_COMPATIBLE` is the forward-looking escape hatch for future providers that expose an OpenAI-style chat completions API. Providers with different wire formats still need a small adapter, but the client is now structured around that adapter boundary.

## Test

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd test
```
