package com.hidoc.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hidoc.mcp.core.McpContext;
import com.hidoc.mcp.core.McpTool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class RagBuildContextTool implements McpTool {
    private final ObjectMapper mapper;

    public RagBuildContextTool(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String name() { return "rag.build_context_from_history"; }

    @Override
    public JsonNode schema() {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("message").put("type", "string");
        props.putObject("conversation_history").put("type", "array");
        props.putObject("top_k").put("type", "integer").put("default", 8);
        props.putObject("max_context_tokens").put("type", "integer").put("default", 2048);
        props.putObject("mmr_lambda").put("type", "number").put("default", 0.5);
        props.putObject("recency_half_life_minutes").put("type", "number").put("default", 1440);
        props.putObject("summarize_long_turns").put("type", "boolean").put("default", true);
        return s;
    }

    @Override
    public CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            String message = params.path("message").asText("");
            ArrayNode history = (ArrayNode) params.path("conversation_history");
            int topK = params.path("top_k").asInt(8);
            double halfLifeMin = params.path("recency_half_life_minutes").asDouble(1440);

            if (history == null || history.isEmpty()) {
                ObjectNode out = mapper.createObjectNode();
                out.put("context", "");
                out.putArray("chunks");
                out.put("tokens", 0);
                return out;
            }

            List<ObjectNode> chunks = new ArrayList<>();
            for (JsonNode n : history) {
                if (!n.has("content")) continue;
                String role = n.path("role").asText("user");
                String content = n.path("content").asText("");
                double baseScore = jaccardSim(message, content);
                double recencyFactor = recencyWeight(n.path("timestamp").asText(null), halfLifeMin);
                double score = baseScore * recencyFactor * roleWeight(role);
                ObjectNode c = mapper.createObjectNode();
                c.put("role", role);
                c.put("content", content);
                c.put("score", score);
                c.put("reason", "jaccard*recency*role");
                chunks.add(c);
            }
            chunks.sort(Comparator.comparingDouble(a -> -a.path("score").asDouble(0)));
            List<ObjectNode> top = chunks.stream().limit(topK).collect(Collectors.toList());
            StringBuilder sb = new StringBuilder();
            for (ObjectNode c : top) {
                String role = c.path("role").asText();
                String content = c.path("content").asText();
                sb.append("[" + role + "] ").append(content).append("\n");
            }
            ObjectNode out = mapper.createObjectNode();
            out.put("context", sb.toString().trim());
            ArrayNode arr = mapper.createArrayNode();
            top.forEach(arr::add);
            out.set("chunks", arr);
            out.put("tokens", sb.length() / 4); // rough estimate
            return out;
        });
    }

    private double roleWeight(String role) {
        return switch (role) {
            case "system" -> 1.2;
            case "assistant" -> 0.8;
            case "tool" -> 0.9;
            default -> 1.0;
        };
    }

    private double recencyWeight(String ts, double halfLifeMinutes) {
        if (ts == null) return 1.0;
        try {
            Instant t = OffsetDateTime.parse(ts).toInstant();
            long minutes = Math.max(0, (Instant.now().toEpochMilli() - t.toEpochMilli()) / (60_000));
            double lambda = Math.log(2) / halfLifeMinutes;
            return Math.exp(-lambda * minutes);
        } catch (DateTimeParseException ex) {
            return 1.0;
        }
    }

    private double jaccardSim(String a, String b) {
        Set<String> sa = tokenize(a);
        Set<String> sb = tokenize(b);
        if (sa.isEmpty() || sb.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        return inter.size() / (double) union.size();
    }

    private Set<String> tokenize(String s) {
        return Arrays.stream(s.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toSet());
    }
}
