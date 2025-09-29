### Goal
Convert the existing REST API that handles AI chat into an MCP (Model Context Protocol) Server, while keeping the current `/api/ai/chat` entry point functional. The MCP Server should:
- Use the `master_prompt` (and other prompt files in `./resources/prompts/`) to route/compose calls across specialized tools/agents.
- Support inclusion of prior message history via on-the-fly RAG computed from the request’s `conversation_history` JSON (no centralized vector DB).
- Wrap/extend existing services (notably `AIProxyService`) so we avoid rewriting core logic.

Below is a step-by-step implementation plan tailored to the current codebase and constraints.

---

### Current Relevant Code and How It Maps to MCP
- `ChatGPTController` (path: `src\main\java\com\hidoc\api\web\ai\ChatGPTController.java`)
    - Endpoint: `POST /api/ai/openai/chat` currently receives a message and optional `conversation_history`, builds an `AIRequest`, and calls `AIProxyService.process()`.
- `AIProxyService` (path: `src\main\java\com\hidoc\api\ai\service\AIProxyService.java`)
    - Central abstraction to talk to the LLM provider(s). Ideal to reuse from MCP server tools.
- Prompts folder: `src\main\resources\prompts\master_prompt.txt` (and other prompts)
    - Will seed an Orchestrator tool that routes to specialized tools/agents.
- `OAuthValidationService` and `UserInfo`
    - Useful to bridge auth/identity signals when exposing an MCP connection.

---

### High-Level Architecture
1. MCP Server process (new module):
    - Exposes MCP-compliant JSON-RPC 2.0 over stdio or WebSocket (depending on client).
    - Registers tools:
        - `ai.chat` – core chat tool wrapping `AIProxyService`.
        - `ai.route_with_master_prompt` – reads `master_prompt.txt` and routes to specialized tools.
        - One tool per specialized prompt (e.g., `ai.tool.drug_info`, `ai.tool.diagnosis_support`, etc.).
        - `rag.build_context_from_history` – builds a compact, relevance-ranked context from the provided `conversation_history` JSON.
        - `memory.get_history` / `memory.append_history` – optional message history operations (not required for RAG).
        - `prompts.list` / `prompts.get` – to enumerate and fetch prompt content/resources.
    - Optional: `usage.track` – forwards usage events to `UsageTracking` domain object.

2. REST Gateway (existing Spring Boot app):
    - Keep `/api/ai/openai/chat` (or add `/api/ai/chat`) as the REST facade.
    - The controller becomes a thin proxy to the MCP Server by making a local MCP client call to `ai.route_with_master_prompt` (or `ai.chat` depending on design).
    - Backward compatibility: The REST path returns the same `AIResponse` shape as today.

3. Data Layer:
    - No centralized vector store is required for RAG. RAG operates on the request’s `conversation_history` only.

---

### Protocol and Tool Design (MCP)
MCP uses JSON-RPC 2.0 over a transport (stdio/websocket) with concepts of tools, resources, and prompts. You will:

- Define Tools (methods) with JSON schemas for params/results.
- Optionally define Resources (e.g., prompt files, retrieved context) that the client can open/stream.

#### Shared schemas
```json
{
  "components": {
    "schemas": {
      "ConversationHistory": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "id": {"type": "string"},
            "role": {"type": "string", "enum": ["user", "assistant", "system", "tool"]},
            "content": {"type": "string"},
            "timestamp": {"type": "string", "format": "date-time"},
            "tool_name": {"type": "string"},
            "metadata": {"type": "object"}
          },
          "required": ["role", "content"]
        }
      }
    }
  }
}
```

#### 1) Tool: `ai.chat`
- Purpose: Direct call to the LLM via `AIProxyService`.
- Params:
```json
{
  "type": "object",
  "properties": {
    "user_id": {"type": "string"},
    "message": {"type": "string"},
    "conversation_history": {"$ref": "#/components/schemas/ConversationHistory"},
    "provider": {"type": "string", "enum": ["OPENAI", "ANTHROPIC", "GEMINI"], "default": "OPENAI"},
    "metadata": {"type": "object"}
  },
  "required": ["user_id", "message"]
}
```
- Result:
```json
{
  "type": "object",
  "properties": {
    "text": {"type": "string"},
    "model": {"type": "string"},
    "tokensUsed": {"type": "number"},
    "finishReason": {"type": "string"}
  },
  "required": ["text"]
}
```
- Implementation: Map to `AIProxyService.process()` using existing `AIRequest` and return `AIResponse`-like payload.

#### 2) Tool: `prompts.get` and `prompts.list`
- `prompts.list` returns all filenames under `resources/prompts/`.
- `prompts.get` loads a specific prompt file content (e.g., `master_prompt.txt`).

#### 3) Tool: `ai.route_with_master_prompt`
- Purpose: Orchestrator. Reads `master_prompt.txt`, interprets routing logic (e.g., classify intent), then calls a specialized tool accordingly, combines outputs back to the user.
- Params:
```json
{
  "type": "object",
  "properties": {
    "user_id": {"type": "string"},
    "message": {"type": "string"},
    "conversation_history": {"$ref": "#/components/schemas/ConversationHistory"},
    "include_rag": {"type": "boolean", "default": true}
  },
  "required": ["user_id", "message"]
}
```
- Behavior:
    - Load `master_prompt.txt` and possibly chain-of-thought free instructions for intent classification (no CoT in output unless policy allows).
    - If `include_rag` is true AND `conversation_history` is non-empty: call `rag.build_context_from_history` to generate a compact context block; inject it into the downstream tool prompt.
    - Based on intent, invoke the relevant specialized tool; fall back to `ai.chat` for general Q&A.

#### 4) Tools per Specialized Prompt (examples)
- `ai.tool.drug_info` – uses `prompts/drug_info.txt` with the message and optional RAG `context`.
- `ai.tool.diagnosis_support` – uses `prompts/diagnosis_support.txt`.
- `ai.tool.lifestyle_coach`, `ai.tool.lab_report_explainer`, etc.
- All should accept a common param schema: `user_id`, `message`, `context` (optional RAG results), `conversation_history`.

#### 5) Tools for Memory and RAG
- `memory.get_history` / `memory.append_history` – optional server-side memory of message turns.
- `rag.build_context_from_history` – params and result as defined below.

#### New tool: `rag.build_context_from_history`
- Purpose: Build a relevance-ranked context block from the provided `conversation_history` for the current `message`. No external database reads.
- Params:
```json
{
  "type": "object",
  "properties": {
    "message": {"type": "string"},
    "conversation_history": {"$ref": "#/components/schemas/ConversationHistory"},
    "top_k": {"type": "integer", "minimum": 1, "maximum": 32, "default": 8},
    "max_context_tokens": {"type": "integer", "minimum": 512, "maximum": 8192, "default": 2048},
    "mmr_lambda": {"type": "number", "minimum": 0, "maximum": 1, "default": 0.5},
    "recency_half_life_minutes": {"type": "number", "minimum": 1, "default": 1440},
    "role_weights": {
      "type": "object",
      "properties": {"user": {"type": "number", "default": 1.0}, "assistant": {"type": "number", "default": 0.8}, "system": {"type": "number", "default": 1.2}, "tool": {"type": "number", "default": 0.9}}
    },
    "summarize_long_turns": {"type": "boolean", "default": true}
  },
  "required": ["message", "conversation_history"]
}
```
- Result:
```json
{
  "type": "object",
  "properties": {
    "context": {"type": "string"},
    "chunks": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "role": {"type": "string"},
          "content": {"type": "string"},
          "score": {"type": "number"},
          "reason": {"type": "string"}
        }
      }
    },
    "tokens": {"type": "integer"}
  },
  "required": ["context"]
}
```

---

### RAG Design (On-the-fly from `conversation_history`)
1. Inputs: relies solely on the request’s `conversation_history` JSON array and the current `message`.
2. Algorithm:
    - Preprocess (normalize, deduplicate, optional summarization for long turns > ~512 tokens).
    - Chunk long turns into ~256–512 token chunks with 32–64 token overlap, preserving role and timestamp.
    - Embed current `message` and each chunk using an embedding model (e.g., `text-embedding-3-small` or family used by `AIProxyService`). Cache within the request to avoid recompute.
    - Rank with cosine similarity and MMR (`mmr_lambda`), weight by role, decay by recency (half-life minutes).
    - Select top-k and pack into a compact `context` string within `max_context_tokens`.
3. Edge behavior: empty/missing history → return empty context; orchestrator omits infusion.
4. Privacy & compliance: no central storage; redact PHI in logs; respect user consent flags if provided.

---

### Memory and Conversation History
- Primary source of history is the client-provided `conversation_history` JSON.
- Server-side memory can be offered via `memory.*` tools, but it is not required for RAG.
- If implemented, ensure opt-in consent and proper scoping per `user_id`/`conversation_id`.

---

### Changes in the Spring Boot App (REST Gateway)
1. Add a new controller path that maps to MCP orchestrator (optional rename):
    - `POST /api/ai/chat` → Calls MCP tool `ai.route_with_master_prompt` using an internal MCP client.
    - Keep `/api/ai/openai/chat` for backward compatibility or deprecate later.

2. Introduce a lightweight MCP client in Java:
    - If using WebSocket MCP server: use a Java WebSocket client and a small JSON-RPC helper.
    - Create an adapter `McpClient` with methods: `callTool(String name, JsonNode params)`.

3. Update `ChatGPTController` to route through MCP:
    - Extract `user_id` as today; build params: `message`, `conversation_history`.
    - `McpClient.callTool("ai.route_with_master_prompt", params)`; translate result into `AIResponse` for the REST response.

---

### Building the MCP Server
You have two practical options:

1) JVM-native (single language, reuses Spring skillset)
- Implement a small JSON-RPC 2.0 server over WebSocket or stdio.
- Build a `ToolRegistry` and wire tools to service classes:
    - `AiChatTool` (wraps `AIProxyService`)
    - `RoutingTool` (loads `master_prompt.txt` and calls specialized tools)
    - `RagOnTheFlyTool` (`rag.build_context_from_history`), `MemoryTools`, `PromptTools`
- Pros: Single deployment, easier reuse of current beans via Spring. Cons: No off-the-shelf Java MCP SDK yet; you’ll build a thin MCP layer.

2) Node/TypeScript MCP (fastest path with existing SDKs) + Java bridge
- Use official/community MCP server SDK (TypeScript) to expose tools.
- Each tool calls Java REST endpoints or gRPC to reach `AIProxyService` logic, or you re-implement the minimal parts in Node.
- Pros: Mature SDKs and clients; Cons: Two runtimes.

Recommendation: Start with JVM-native WebSocket MCP to keep dependencies minimal and reuse `AIProxyService` directly.

---

### Draft Interfaces and Pseudocode

#### MCP Tool interface (Java)
```java
public interface McpTool {
  String name();
  JsonNode schema(); // JSON Schema for params
  CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx);
}
```

#### WebSocket JSON-RPC handler (simplified)
```java
@ServerEndpoint("/mcp")
public class McpEndpoint {
  private final ToolRegistry registry = ...;

  @OnMessage
  public void onMessage(Session session, String text) {
    JsonNode req = mapper.readTree(text);
    String method = req.get("method").asText();
    JsonNode params = req.get("params");
    String id = req.get("id").asText();

    registry.call(method, params)
      .thenApply(result -> jsonRpcResult(id, result))
      .exceptionally(ex -> jsonRpcError(id, ex))
      .thenAccept(resp -> session.getAsyncRemote().sendText(resp));
  }
}
```

#### Routing tool sketch (updated)
```java
public class RoutingTool implements McpTool {
  private final PromptLoader prompts; // reads resources/prompts
  private final McpClient internal;   // to call other tools (or ToolRegistry)
  private final RagOnTheFlyService rag;

  public String name() { return "ai.route_with_master_prompt"; }

  public CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx) {
    String userId = params.get("user_id").asText();
    String message = params.get("message").asText();
    boolean includeRag = params.path("include_rag").asBoolean(true);

    String master = prompts.get("master_prompt.txt");
    List<JsonNode> history = readHistory(params);

    // 1) Classify intent using LLM (call ai.chat with a classifier prompt built from master)
    JsonNode classification = internal.callTool("ai.chat", buildClassificationParams(master, message, history));

    // 2) Optionally build context from provided history
    ObjectNode ragResult = null;
    if (includeRag && history != null && !history.isEmpty()) {
      ObjectNode rp = mapper.createObjectNode();
      rp.put("message", message);
      rp.set("conversation_history", (JsonNode) mapper.valueToTree(history));
      rp.put("top_k", 8);
      rp.put("max_context_tokens", 2048);
      ragResult = (ObjectNode) internal.callTool("rag.build_context_from_history", rp);
    }

    String context = ragResult == null ? null : ragResult.path("context").asText(null);

    // 3) Route
    String toolName = mapIntentToTool(classification);
    JsonNode result = internal.callTool(toolName, buildToolParams(userId, message, history, context));

    // 4) Persist memory (optional)
    internal.callTool("memory.append_history", buildAppendParams(userId, message, result));

    return CompletableFuture.completedFuture(result);
  }
}
```

---

### Prompt Management
- Store all specialized prompts in `src\main\resources\prompts\`. Examples:
    - `master_prompt.txt`
    - `drug_info.txt`
    - `diagnosis_support.txt`
    - `lifestyle_coach.txt`
- `PromptLoader` utility:
    - Loads by file name;
    - Optionally supports parameterization (e.g., `{{patient_age}}`).
- In each specialized tool prompt, include an optional `[Context]` block that is omitted if `context` is empty.

---

### Security and Auth Mapping
- MCP itself doesn’t mandate auth; clients usually connect locally. Add a session token requirement:
    - During WebSocket handshake, validate a bearer token (reuse `OAuthValidationService`).
    - Propagate `user_id` from the REST Gateway or from the token claims.
- Enforce tenant/consent when accessing any optional `memory.*` tools.
- Do not persist `conversation_history` or embeddings unless explicitly requested with consent.

---

### Streaming and Timeouts
- Support partial outputs using JSON-RPC notifications (e.g., `mcp.tool.progress`), or chunked results in the REST gateway if you need SSE.
- Apply timeouts per tool call; expose `finishReason` in results.

---

### Observability
- Log tool invocations with duration and token usage (wrap `AIProxyService` results).
- Integrate with `UsageTracking` for audit and billing.
- Redact PHI in logs; use structured logging; log sizes/counts, not content.

---

### Database Changes
- None required for RAG.
- Optional tables for server-side memory if desired:
    - `conversation` and `message` (for memory only)

---

### Migration Plan
1. Week 1: Foundations
    - Decide MCP transport (WebSocket) and language (Java).
    - Create `mcp-server` module with JSON-RPC handler, `ToolRegistry`, and a basic `ai.chat` tool wrapping `AIProxyService`.
    - Add `prompts.list/get` tools.

2. Week 2: Orchestration and RAG
    - Implement `ai.route_with_master_prompt` and 1–2 specialized tools using existing prompts.
    - Implement `RagOnTheFlyService` and MCP tool `rag.build_context_from_history`.
    - Add `memory.get_history` / `memory.append_history` only if needed (optional).

3. Week 3: REST Gateway integration
    - Add internal `McpClient` and update `/api/ai/chat` to call the MCP orchestrator.
    - Feature flag to enable/disable MCP path. Preserve `/api/ai/openai/chat`.

4. Week 4: Security, Observability, Hardening
    - WebSocket auth and per-tool authorization checks.
    - Rate limiting per `user_id`.
    - Add tests, load tests, and monitoring dashboards.

---

### Testing Strategy
- Unit tests for each tool (schema validation, happy/edge cases).
- Contract tests for JSON-RPC requests/responses.
- E2E tests: REST → MCP → tools → `AIProxyService`.
- Privacy tests: verify no persistence of history/embeddings unless consented; verify memory scoping per `user_id` if enabled.
- Specific unit tests for `rag.build_context_from_history`: small, large, empty histories; confirms MMR, recency decay, and token budgets.

---

### Example: REST Gateway calling MCP
```java
@PostMapping(value = "/api/ai/chat", produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<AIResponse> chat(@Valid @RequestBody ChatPayload req, Authentication auth) {
  String userId = resolveUserId(auth, req);
  ObjectNode params = mapper.createObjectNode();
  params.put("user_id", userId);
  params.put("message", req.getMessage());
  params.set("conversation_history", mapper.valueToTree(req.getConversation_history()));
  params.put("include_rag", true);

  JsonNode result = mcpClient.callTool("ai.route_with_master_prompt", params);
  AIResponse out = translateToAIResponse(result); // map fields back to your DTO
  return ResponseEntity.ok(out);
}
```

---

### Operational Considerations
- Deployment: Co-locate MCP server with the REST app (same JVM) to avoid network hops, or run as a sidecar with localhost WebSocket.
- Backward compatibility: Keep the old endpoint working as long as needed. Gradually switch internal callers to `POST /api/ai/chat`.
- Documentation: Publish the MCP tool schemas for client integrators (e.g., IDEs, agent frameworks).

---

### Deliverables Checklist
- MCP server module with:
    - JSON-RPC handler, `ToolRegistry`, auth middleware
    - Tools: `ai.chat`, `ai.route_with_master_prompt`, `prompts.list/get`, `rag.build_context_from_history`, `memory.get_history/append_history` (optional), and 2–3 specialized prompt tools
- No vector DB required
- REST gateway client and updated controller path
- Observability + security + tests

---

### Summary
By introducing an MCP server that wraps your existing `AIProxyService` and moving to an on-the-fly RAG approach based solely on the request’s `conversation_history`, you avoid centralized vector storage and simplify privacy/compliance. The REST endpoint `/api/ai/chat` remains a thin adapter to MCP. The new `rag.build_context_from_history` tool produces a compact context for better responses when history is present, and the system gracefully degrades (no RAG) when it isn’t.