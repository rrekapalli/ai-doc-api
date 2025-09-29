package com.hidoc.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hidoc.mcp.core.McpContext;
import com.hidoc.mcp.core.McpTool;
import com.hidoc.mcp.core.ToolRegistry;
import com.hidoc.mcp.util.PromptLoader;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Component
public class RoutingTool implements McpTool {
    private final ObjectMapper mapper;
    private final org.springframework.beans.factory.ObjectProvider<ToolRegistry> registryProvider;
    private final PromptLoader prompts;

    public RoutingTool(ObjectMapper mapper, org.springframework.beans.factory.ObjectProvider<ToolRegistry> registryProvider, PromptLoader prompts) {
        this.mapper = mapper;
        this.registryProvider = registryProvider;
        this.prompts = prompts;
    }

    @Override
    public String name() { return "ai.route_with_master_prompt"; }

    @Override
    public JsonNode schema() {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("user_id").put("type", "string");
        props.putObject("message").put("type", "string");
        props.putObject("conversation_history").put("type", "array");
        props.putObject("include_rag").put("type", "boolean").put("default", true);
        s.putArray("required").add("user_id").add("message");
        return s;
    }

    @Override
    public CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = params.path("user_id").asText(ctx.getUserId().orElse(""));
            String message = params.path("message").asText("");
            boolean includeRag = params.path("include_rag").asBoolean(true);
            JsonNode history = params.path("conversation_history");

            String context = null;
            if (includeRag && history != null && history.isArray() && history.size() > 0) {
                ObjectNode rp = mapper.createObjectNode();
                rp.put("message", message);
                rp.set("conversation_history", history);
                JsonNode rag = registryProvider.getObject().call("rag.build_context_from_history", rp, ctx).join();
                context = rag.path("context").asText(null);
            }

            // Step 1: Run classification using message_classifier_prompt with master prompt guidance
            JsonNode classification = runClassifier(userId, message, context, ctx);

            // Step 2: If classification failed, or says parsed=false => treat as QUERY per master_prompt lines 43-46
            if (classification == null || !classification.path("parsed").asBoolean(false)) {
                String reply = classification != null ? classification.path("reply").asText("") : "";
                if (reply == null || reply.isBlank()) {
                    reply = buildSafeQueryReply(message);
                } else {
                    reply = enforceReplyRules(reply);
                }
                ObjectNode out = mapper.createObjectNode();
                out.put("text", reply);
                out.put("model", "classifier");
                out.put("tokensUsed", 0);
                return out;
            }

            // Step 3: Route to specialized tool when parsed=true
            String routeTo = classification.path("route_to").asText("");
            String msgType = classification.path("message_type").asText("");
            String tool = mapRouteToTool(routeTo, msgType, message);

            ObjectNode callParams = mapper.createObjectNode();
            callParams.put("user_id", userId);
            callParams.put("message", message);
            if (context != null && !context.isBlank()) callParams.put("context", context);

            // For ai.chat we don't pass context
            if ("ai.chat".equals(tool)) {
                callParams.remove("context");
            }

            try {
                return registryProvider.getObject().call(tool, callParams, ctx).join();
            } catch (RuntimeException ex) {
                ObjectNode out = mapper.createObjectNode();
                out.put("text", buildProviderDownMessage(ex));
                out.put("model", "error");
                out.put("tokensUsed", 0);
                return out;
            }
        });
    }

    private JsonNode runClassifier(String userId, String message, String context, McpContext ctx) {
        String classifier = prompts.get("message_classifier_prompt.txt");
        if (classifier == null) classifier = "";
        String master = prompts.get("master_prompt.txt");
        StringBuilder sb = new StringBuilder();
        if (master != null && !master.isBlank()) {
            sb.append(master.trim()).append("\n\n");
        }
        sb.append(classifier.trim()).append("\n\n");
        if (context != null && !context.isBlank()) {
            sb.append("[Context]\n").append(context.trim()).append("\n\n");
        }
        sb.append("[Message]\n").append(message);

        ObjectNode chatParams = mapper.createObjectNode();
        chatParams.put("user_id", userId);
        chatParams.put("message", sb.toString());
        JsonNode result;
        try {
            result = registryProvider.getObject().call("ai.chat", chatParams, ctx).join();
        } catch (RuntimeException ex) {
            // If the provider is unavailable during classification, fall back to null to trigger safe reply path
            return null;
        }
        if (result == null) return null;
        String text = result.path("text").asText("");
        if (text == null || text.isBlank()) return null;
        try {
            return mapper.readTree(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private String mapRouteToTool(String routeTo, String msgType, String message) {
        String rt = routeTo == null ? "" : routeTo.toLowerCase(Locale.ROOT);
        String mt = msgType == null ? "" : msgType.toLowerCase(Locale.ROOT);
        // Support filenames or message_type values
        if (rt.contains("medication") || mt.equals("medication")) return "ai.tool.drug_info";
        if (rt.contains("report") || mt.equals("report")) return "ai.chat"; // no dedicated tool yet
        if (rt.contains("health_data") || mt.equals("health")) return "ai.chat"; // fallback
        if (rt.contains("activity") || mt.equals("activity")) return "ai.chat"; // fallback
        // Fallback: heuristic to diagnosis vs general chat
        return mapIntentToTool(message);
    }

    private String mapIntentToTool(String message) {
        String m = message.toLowerCase(Locale.ROOT);
        if (m.contains("drug") || m.contains("medication") || m.contains("dose") || m.contains("side effect")) {
            return "ai.tool.drug_info";
        }
        if (m.contains("diagnos") || m.contains("symptom") || m.contains("what could this be") || m.contains("condition")) {
            return "ai.tool.diagnosis_support";
        }
        return "ai.chat";
    }

    private String buildSafeQueryReply(String message) {
        String base = "I can help with general health information. For emergencies, seek professional care.";
        return base;
    }

    private String enforceReplyRules(String reply) {
        if (reply == null) return "";
        // Do not auto-append any disclaimers here; prompts already include a formal disclaimer section.
        // Also, do not truncate the reply; allow full-length responses as requested.
        return reply;
    }

    private String buildProviderDownMessage(Exception ex) {
        // Keep the message simple and user-friendly; avoid leaking internal error details
        return "The AI service is temporarily unavailable. Please try again in a minute. Your message has been saved.";
    }
}
