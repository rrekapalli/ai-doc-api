package com.hidoc.api.web.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.ai.service.AIProxyService;
import com.hidoc.api.web.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GrokController.class)
class GrokControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AIProxyService proxyService;

    @MockBean
    private com.hidoc.api.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "user-2")
    void chat_shouldReturnOk() throws Exception {
        AIResponse response = new AIResponse();
        response.setReply("grok-reply");
        Mockito.when(proxyService.process(any())).thenReturn(response);

        ChatRequest req = new ChatRequest();
        req.setMessage("ask grok");

        mockMvc.perform(post("/api/ai/grok/chat")
                        .with(user("user-2"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
