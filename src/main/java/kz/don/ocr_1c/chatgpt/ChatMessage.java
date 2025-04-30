package kz.don.ocr_1c.chatgpt;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessage {
    private String role;
    private String content;
}
