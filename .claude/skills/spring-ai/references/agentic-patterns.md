# Agentic Workflow Patterns

Spring AI documents five workflow patterns for building agentic applications. These are architectural patterns, not framework features — you implement them using `ChatClient`, advisors, and tools.

## When to Use Agentic Patterns

**Before building an agentic workflow, ask:**
- Can a single LLM call with good prompting solve this? Start simple
- Is the task decomposable into independent subtasks? → Parallelization
- Does the task require different expertise for different inputs? → Routing
- Does the task need iterative refinement? → Evaluator-Optimizer
- Does the task need dynamic decomposition? → Orchestrator-Workers

## 1. Chain Workflow

Sequential steps where each step's output feeds the next:

```
Input -> LLM Step 1 -> LLM Step 2 -> LLM Step 3 -> Output
```

```java
String draft = chatClient.prompt()
    .system("You are a technical writer")
    .user("Write a summary of: " + input)
    .call().content();

String reviewed = chatClient.prompt()
    .system("You are an editor. Fix grammar and clarity.")
    .user(draft)
    .call().content();

String translated = chatClient.prompt()
    .system("Translate to German")
    .user(reviewed)
    .call().content();
```

**Use when:** Each step is well-defined and the output of one naturally feeds the next. Trade-off: latency increases linearly with chain length.

## 2. Parallelization Workflow

Concurrent LLM calls on independent subtasks, results aggregated:

```
         ┌-> LLM Task A --┐
Input ---┤-> LLM Task B --├-> Aggregate -> Output
         └-> LLM Task C --┘
```

```java
var taskA = CompletableFuture.supplyAsync(() ->
    chatClient.prompt().system("Analyze sentiment").user(text).call().content());
var taskB = CompletableFuture.supplyAsync(() ->
    chatClient.prompt().system("Extract entities").user(text).call().content());
var taskC = CompletableFuture.supplyAsync(() ->
    chatClient.prompt().system("Summarize key points").user(text).call().content());

var results = CompletableFuture.allOf(taskA, taskB, taskC).thenApply(v ->
    Map.of("sentiment", taskA.join(), "entities", taskB.join(), "summary", taskC.join()));
```

**Use when:** Subtasks are independent and can run concurrently. Reduces wall-clock time proportionally to the number of parallel tasks.

## 3. Routing Workflow

Classify input first, then route to a specialized handler:

```
Input -> Classifier LLM -> Route A (specialized prompt)
                        -> Route B (specialized prompt)
                        -> Route C (specialized prompt)
```

```java
record Classification(String category) {}

Classification cls = chatClient.prompt()
    .user("Classify this support ticket: " + ticket)
    .call().entity(Classification.class);

String systemPrompt = switch (cls.category()) {
    case "billing" -> "You are a billing support specialist...";
    case "technical" -> "You are a technical support engineer...";
    default -> "You are a general support agent...";
};

String response = chatClient.prompt()
    .system(systemPrompt)
    .user(ticket)
    .call().content();
```

**Use when:** Different inputs need fundamentally different expertise or system prompts. Better than one generic prompt trying to handle everything.

## 4. Orchestrator-Workers

An orchestrator LLM dynamically decomposes the task; worker LLMs execute subtasks:

```
Input -> Orchestrator LLM -> [Task 1, Task 2, ...] -> Worker LLMs -> Synthesizer -> Output
```

```java
record TaskPlan(List<String> subtasks) {}

TaskPlan plan = chatClient.prompt()
    .system("Break this task into independent subtasks")
    .user(complexTask)
    .call().entity(TaskPlan.class);

List<String> results = plan.subtasks().parallelStream()
    .map(subtask -> chatClient.prompt()
        .system("Complete this subtask thoroughly")
        .user(subtask)
        .call().content())
    .toList();

String synthesis = chatClient.prompt()
    .system("Synthesize these results into a coherent response")
    .user(String.join("\n---\n", results))
    .call().content();
```

**Use when:** Tasks are too complex for a single prompt and the decomposition itself requires intelligence. More flexible than chains but more expensive.

## 5. Evaluator-Optimizer

Iterative refinement loop with evaluation criteria:

```
Input -> Generator LLM -> Evaluator LLM -> [Pass? -> Output]
                              |                [Fail? -> feedback -> Generator again]
```

```java
record Evaluation(boolean acceptable, String feedback) {}

String draft = chatClient.prompt().user(task).call().content();

for (int i = 0; i < maxIterations; i++) {
    Evaluation eval = chatClient.prompt()
        .system("Evaluate if this meets the criteria: " + criteria)
        .user(draft)
        .call().entity(Evaluation.class);

    if (eval.acceptable()) break;

    draft = chatClient.prompt()
        .system("Improve based on this feedback: " + eval.feedback())
        .user(draft)
        .call().content();
}
```

**Use when:** Quality matters more than latency, and clear evaluation criteria exist. Set a max iteration limit to prevent infinite loops.

## Key Guidelines

- **Start with a single ChatClient call** — only add agentic patterns when a single call genuinely can't solve the problem
- **Chains are the simplest pattern** — prefer them over orchestrator-workers when steps are predictable
- **Parallelization is free performance** — use it whenever subtasks are independent
- **Set iteration limits** on evaluator-optimizer loops — LLMs can get stuck in refinement cycles
- **Monitor costs** — agentic patterns multiply LLM calls. A 3-step chain with 5 evaluator iterations = 15+ calls per request
- **Use different models for different roles** — cheap/fast models for classification and evaluation, capable models for generation
