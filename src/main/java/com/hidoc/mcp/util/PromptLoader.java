package com.hidoc.mcp.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

@Component
public class PromptLoader {

    public String get(String filename) {
        try {
            ClassPathResource res = new ClassPathResource("prompts/" + filename);
            if (!res.exists()) return null;
            try (Scanner scanner = new Scanner(res.getInputStream(), StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        } catch (IOException e) {
            return null;
        }
    }

    public List<String> list() {
        // ClassPathResource cannot list directories reliably in a jar; list known prompts by trying common names
        // As a minimal solution, return a static set that covers present files.
        List<String> names = new ArrayList<>();
        String[] candidates = new String[]{
                "master_prompt.txt",
                "drug_info.txt",
                "diagnosis_support.txt",
                "lifestyle_coach.txt",
                "health_data_entry_prompt.txt",
                "activity_data_entry_prompt.txt",
                "health_data_trend_prompt.txt",
                "medical_query_elaboration_prompt.txt",
                "medication_data_entry_prompt.txt",
                "message_classifier_prompt.txt",
                "reports_processing_prompt.txt"
        };
        for (String c : candidates) {
            if (get(c) != null) names.add(c);
        }
        return names;
    }
}
