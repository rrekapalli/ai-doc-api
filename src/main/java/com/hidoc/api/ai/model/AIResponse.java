package com.hidoc.api.ai.model;

import com.hidoc.api.domain.HealthDataEntry;

import java.util.List;
import java.util.Map;

public class AIResponse {
    private String reply;
    private HealthDataEntry healthDataEntry;
    private String storedId;
    private boolean persisted;
    private String reasoning;
    private List<Map<String, Object>> matches;

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public HealthDataEntry getHealthDataEntry() { return healthDataEntry; }
    public void setHealthDataEntry(HealthDataEntry healthDataEntry) { this.healthDataEntry = healthDataEntry; }

    public String getStoredId() { return storedId; }
    public void setStoredId(String storedId) { this.storedId = storedId; }

    public boolean isPersisted() { return persisted; }
    public void setPersisted(boolean persisted) { this.persisted = persisted; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public List<Map<String, Object>> getMatches() { return matches; }
    public void setMatches(List<Map<String, Object>> matches) { this.matches = matches; }
}