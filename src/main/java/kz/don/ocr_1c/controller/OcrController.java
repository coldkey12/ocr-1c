package kz.don.ocr_1c.controller;

import kz.don.ocr_1c.model.ImageFile;
import kz.don.ocr_1c.repository.ImageFileRepository;
import kz.don.ocr_1c.chatgpt.ChatGptService;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Controller
public class OcrController {

    static {
        // Configure native library paths for Tesseract on macOS
        System.setProperty("jna.library.path", "/opt/homebrew/lib");
        System.setProperty("TESSDATA_PREFIX", "/opt/homebrew/share/tessdata");
    }

    private final ImageFileRepository repository;
    private final ChatGptService chatGptService;

    public OcrController(ImageFileRepository repository, ChatGptService chatGptService) {
        this.repository = repository;
        this.chatGptService = chatGptService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("files", repository.findAll());
        return "index";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         RedirectAttributes redirectAttributes) throws IOException {
        // Validate file type
        if (!file.getContentType().startsWith("image/")) {
            redirectAttributes.addFlashAttribute("error", "Only image files are allowed.");
            return "redirect:/";
        }

        // Create and save new image file
        ImageFile image = new ImageFile();
        image.setId(UUID.randomUUID());
        image.setName(file.getOriginalFilename());
        image.setMimeType(file.getContentType());
        image.setContent(file.getBytes());
        repository.save(image);

        redirectAttributes.addFlashAttribute("success", "File uploaded successfully!");
        return "redirect:/";
    }

    @PostMapping("/ocr/{id}")
    public String performOcr(@PathVariable UUID id,
                             RedirectAttributes redirectAttributes) throws IOException {
        ImageFile file = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        try {
            // Step 1: Perform OCR on the image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getContent()));
            String ocrText = extractTextFromImage(image);
            file.setOcrText(ocrText);

            // Step 2: Analyze text with ChatGPT
            String analyzedText = chatGptService.analyzeText(ocrText);
            file.setAnalyzedText(analyzedText);

            repository.save(file);
            redirectAttributes.addFlashAttribute("success", "OCR and analysis completed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Processing failed: " + e.getMessage());
        }

        return "redirect:/";
    }

    private String extractTextFromImage(BufferedImage image) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/share/tessdata");
        tesseract.setLanguage("rus+eng"); // Support both Russian and English
        tesseract.setPageSegMode(1); // Automatic page segmentation
        tesseract.setOcrEngineMode(1); // LSTM OCR engine

        return tesseract.doOCR(image);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> viewImage(@PathVariable UUID id) {
        ImageFile file = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .body(file.getContent());
    }

    @GetMapping("/text/{id}")
    @ResponseBody
    public String viewOcrText(@PathVariable UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"))
                .getOcrText();
    }

    @GetMapping("/analyze/{id}")
    @ResponseBody
    public String viewAnalysis(@PathVariable UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found"))
                .getAnalyzedText();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleNotFound(IllegalArgumentException ex,
                                 RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }
}