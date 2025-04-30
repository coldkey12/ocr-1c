package kz.don.ocr_1c.chatgpt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ChatGptService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public ChatGptService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String analyzeText(String text) {
        try {
            // System message to guide ChatGPT's response
            String systemMessage = "You are an expert analyst. Extract key metrics from the following text and present them in a clear, concise format. " +
                    "Focus on important numbers, dates, names, and facts. Return only the extracted information in plain text format.";

            ChatMessage systemMsg = new ChatMessage("system", systemMessage);
            ChatMessage userMsg = new ChatMessage("user", text);

            ChatCompletionRequest request = new ChatCompletionRequest(
                    "gpt-3.5-turbo",  // Using 3.5-turbo as it's more cost-effective
                    List.of(systemMsg, userMsg)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    ChatCompletionResponse.class);

            return response.getBody().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze text with ChatGPT: " + e.getMessage(), e);
        }
    }
}