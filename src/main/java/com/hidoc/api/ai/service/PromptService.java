package com.hidoc.api.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for loading and managing prompts from resources/prompts directory
 * Supports both existing prompts and new Spring AI tool prompts
 */
@Service
public class PromptService {

    // Cache for loaded prompts to avoid repeated file I/O
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    // Spring AI specific prompts
    @Value("classpath:prompts/spring_ai_master_prompt.txt")
    private Resource springAiMasterPrompt;

    @Value("classpath:prompts/spring_ai_message_classifier_tool.txt")
    private Resource classifierPrompt;

    @Value("classpath:prompts/mood_data_entry_tool.txt")
    private Resource moodPrompt;

    @Value("classpath:prompts/food_intake_tool.txt")
    private Resource foodIntakePrompt;

    @Value("classpath:prompts/trend_analysis_tool.txt")
    private Resource trendAnalysisPrompt;

    @Value("classpath:prompts/prognosis_tool.txt")
    private Resource prognosisPrompt;

    @Value("classpath:prompts/non_health_rejection_tool.txt")
    private Resource rejectionPrompt;

    // Existing prompts
    @Value("classpath:prompts/master_prompt.txt")
    private Resource masterPrompt;

    @Value("classpath:prompts/message_classifier_prompt.txt")
    private Resource messageClassifierPrompt;

    @Value("classpath:prompts/medication_data_entry_prompt.txt")
    private Resource medicationPrompt;

    @Value("classpath:prompts/activity_data_entry_prompt.txt")
    private Resource activityPrompt;

    @Value("classpath:prompts/health_data_entry_prompt.txt")
    private Resource healthDataPrompt;

    @Value("classpath:prompts/medical_query_elaboration_prompt.txt")
    private Resource medicalQueryPrompt;

    @Value("classpath:prompts/reports_processing_prompt.txt")
    private Resource reportsPrompt;

    @Value("classpath:prompts/health_data_trend_prompt.txt")
    private Resource trendPrompt;

    @Value("classpath:prompts/diagnosis_support.txt")
    private Resource diagnosisPrompt;

    @Value("classpath:prompts/drug_info.txt")
    private Resource drugInfoPrompt;

    @Value("classpath:prompts/lifestyle_coach.txt")
    private Resource lifestylePrompt;

    /**
     * Get prompt content by name
     * @param promptName the name of the prompt (without .txt extension)
     * @return the prompt content as string
     */
    public String getPrompt(String promptName) {
        return promptCache.computeIfAbsent(promptName, this::loadPrompt);
    }

    /**
     * Get Spring AI master prompt for system configuration
     */
    public String getSpringAiMasterPrompt() {
        return getPromptContent(springAiMasterPrompt, "spring_ai_master_prompt");
    }

    /**
     * Get message classifier prompt for Spring AI tools
     */
    public String getMessageClassifierPrompt() {
        return getPromptContent(classifierPrompt, "spring_ai_message_classifier_tool");
    }

    /**
     * Get mood data entry tool prompt
     */
    public String getMoodPrompt() {
        return getPromptContent(moodPrompt, "mood_data_entry_tool");
    }

    /**
     * Get food intake tool prompt
     */
    public String getFoodIntakePrompt() {
        return getPromptContent(foodIntakePrompt, "food_intake_tool");
    }

    /**
     * Get trend analysis tool prompt
     */
    public String getTrendAnalysisPrompt() {
        return getPromptContent(trendAnalysisPrompt, "trend_analysis_tool");
    }

    /**
     * Get prognosis tool prompt
     */
    public String getPrognosisPrompt() {
        return getPromptContent(prognosisPrompt, "prognosis_tool");
    }

    /**
     * Get non-health rejection tool prompt
     */
    public String getRejectionPrompt() {
        return getPromptContent(rejectionPrompt, "non_health_rejection_tool");
    }

    /**
     * Get medication data entry prompt
     */
    public String getMedicationPrompt() {
        return getPromptContent(medicationPrompt, "medication_data_entry_prompt");
    }

    /**
     * Get activity data entry prompt
     */
    public String getActivityPrompt() {
        return getPromptContent(activityPrompt, "activity_data_entry_prompt");
    }

    /**
     * Get health data entry prompt
     */
    public String getHealthDataPrompt() {
        return getPromptContent(healthDataPrompt, "health_data_entry_prompt");
    }

    /**
     * Get medical query elaboration prompt
     */
    public String getMedicalQueryPrompt() {
        return getPromptContent(medicalQueryPrompt, "medical_query_elaboration_prompt");
    }

    /**
     * Get reports processing prompt
     */
    public String getReportsPrompt() {
        return getPromptContent(reportsPrompt, "reports_processing_prompt");
    }

    /**
     * Get all available prompt names
     */
    public Map<String, String> getAllPromptNames() {
        Map<String, String> prompts = new HashMap<>();
        
        // Spring AI prompts
        prompts.put("spring_ai_master_prompt", "Spring AI Master Prompt");
        prompts.put("spring_ai_message_classifier_tool", "Message Classifier Tool");
        prompts.put("mood_data_entry_tool", "Mood Data Entry Tool");
        prompts.put("food_intake_tool", "Food Intake Tool");
        prompts.put("trend_analysis_tool", "Trend Analysis Tool");
        prompts.put("prognosis_tool", "Prognosis Tool");
        prompts.put("non_health_rejection_tool", "Non-Health Rejection Tool");
        
        // Existing prompts
        prompts.put("master_prompt", "Master Prompt");
        prompts.put("message_classifier_prompt", "Message Classifier");
        prompts.put("medication_data_entry_prompt", "Medication Data Entry");
        prompts.put("activity_data_entry_prompt", "Activity Data Entry");
        prompts.put("health_data_entry_prompt", "Health Data Entry");
        prompts.put("medical_query_elaboration_prompt", "Medical Query Elaboration");
        prompts.put("reports_processing_prompt", "Reports Processing");
        prompts.put("health_data_trend_prompt", "Health Data Trend");
        prompts.put("diagnosis_support", "Diagnosis Support");
        prompts.put("drug_info", "Drug Information");
        prompts.put("lifestyle_coach", "Lifestyle Coach");
        
        return prompts;
    }

    /**
     * Clear the prompt cache (useful for development/testing)
     */
    public void clearCache() {
        promptCache.clear();
    }

    /**
     * Load prompt content from resource
     */
    private String loadPrompt(String promptName) {
        try {
            Resource resource = getResourceByName(promptName);
            if (resource != null && resource.exists()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("Prompt not found: " + promptName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt: " + promptName, e);
        }
    }

    /**
     * Get prompt content with caching
     */
    private String getPromptContent(Resource resource, String cacheName) {
        return promptCache.computeIfAbsent(cacheName, name -> {
            try {
                if (resource.exists()) {
                    return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                }
                throw new IllegalArgumentException("Prompt resource not found: " + cacheName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load prompt resource: " + cacheName, e);
            }
        });
    }

    /**
     * Map prompt names to resources
     */
    private Resource getResourceByName(String promptName) {
        return switch (promptName) {
            case "spring_ai_master_prompt" -> springAiMasterPrompt;
            case "spring_ai_message_classifier_tool" -> classifierPrompt;
            case "mood_data_entry_tool" -> moodPrompt;
            case "food_intake_tool" -> foodIntakePrompt;
            case "trend_analysis_tool" -> trendAnalysisPrompt;
            case "prognosis_tool" -> prognosisPrompt;
            case "non_health_rejection_tool" -> rejectionPrompt;
            case "master_prompt" -> masterPrompt;
            case "message_classifier_prompt" -> messageClassifierPrompt;
            case "medication_data_entry_prompt" -> medicationPrompt;
            case "activity_data_entry_prompt" -> activityPrompt;
            case "health_data_entry_prompt" -> healthDataPrompt;
            case "medical_query_elaboration_prompt" -> medicalQueryPrompt;
            case "reports_processing_prompt" -> reportsPrompt;
            case "health_data_trend_prompt" -> trendPrompt;
            case "diagnosis_support" -> diagnosisPrompt;
            case "drug_info" -> drugInfoPrompt;
            case "lifestyle_coach" -> lifestylePrompt;
            default -> null;
        };
    }
}