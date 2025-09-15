package com.hidoc.api.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AIConfig.class})
@TestPropertySource(properties = {
        "ai.providers.openai.api-key=test-openai",
        "ai.providers.openai.model=gpt-test",
        "ai.providers.openai.base-url=https://api.openai.test/v1",
        "ai.providers.grok.api-key=test-grok",
        "ai.providers.grok.model=grok-test",
        "ai.providers.grok.base-url=https://api.x.ai.test/v1",
        "ai.providers.gemini.api-key=test-gemini",
        "ai.providers.gemini.model=gemini-test",
        "ai.providers.gemini.base-url=https://gemini.test/v1"
})
class AIProvidersPropertiesTest {

    @Autowired
    private AIProvidersProperties props;

    @Test
    void shouldBindAllProviders() {
        assertThat(props.getOpenai().getApiKey()).isEqualTo("test-openai");
        assertThat(props.getOpenai().getModel()).isEqualTo("gpt-test");
        assertThat(props.getOpenai().getBaseUrl()).isEqualTo("https://api.openai.test/v1");

        assertThat(props.getGrok().getApiKey()).isEqualTo("test-grok");
        assertThat(props.getGrok().getModel()).isEqualTo("grok-test");
        assertThat(props.getGrok().getBaseUrl()).isEqualTo("https://api.x.ai.test/v1");

        assertThat(props.getGemini().getApiKey()).isEqualTo("test-gemini");
        assertThat(props.getGemini().getModel()).isEqualTo("gemini-test");
        assertThat(props.getGemini().getBaseUrl()).isEqualTo("https://gemini.test/v1");
    }
}
