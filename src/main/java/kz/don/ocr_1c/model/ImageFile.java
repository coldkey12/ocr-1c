package kz.don.ocr_1c.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
public class ImageFile {

    @Id
    private UUID id;

    private String name;
    private String mimeType;

    @Lob
    private byte[] content;

    @Lob
    private String ocrText;

    @Lob
    private String analyzedText;

    private Instant uploadedAt = Instant.now();
}