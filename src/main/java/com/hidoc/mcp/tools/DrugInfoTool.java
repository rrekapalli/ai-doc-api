package com.hidoc.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidoc.api.ai.service.AIProxyService;
import com.hidoc.mcp.util.PromptLoader;
import org.springframework.stereotype.Component;

@Component
public class DrugInfoTool extends AbstractPromptTool {
    public DrugInfoTool(ObjectMapper mapper, AIProxyService proxy, PromptLoader loader) {
        super(mapper, proxy, loader);
    }

    @Override
    public String name() { return "ai.tool.drug_info"; }

    @Override
    protected String promptFile() {
        // Use existing prompt as a stand-in
        String candidate = "medication_data_entry_prompt.txt";
        String alt = "medical_query_elaboration_prompt.txt";
        return loader.get(candidate) != null ? candidate : alt;
    }
}
