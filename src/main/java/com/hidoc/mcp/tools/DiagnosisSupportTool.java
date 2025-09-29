package com.hidoc.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidoc.api.ai.service.AIProxyService;
import com.hidoc.mcp.util.PromptLoader;
import org.springframework.stereotype.Component;

@Component
public class DiagnosisSupportTool extends AbstractPromptTool {
    public DiagnosisSupportTool(ObjectMapper mapper, AIProxyService proxy, PromptLoader loader) {
        super(mapper, proxy, loader);
    }

    @Override
    public String name() { return "ai.tool.diagnosis_support"; }

    @Override
    protected String promptFile() {
        String candidate = "medical_query_elaboration_prompt.txt";
        return candidate;
    }
}
