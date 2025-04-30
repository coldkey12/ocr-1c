package kz.don.ocr_1c.controller;

import kz.don.ocr_1c.model.ImageFile;
import kz.don.ocr_1c.repository.ImageFileRepository;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        System.setProperty("jna.library.path", "/opt/homebrew/lib");
        System.setProperty("TESSDATA_PREFIX", "/opt/homebrew/share/tessdata");
    }

    private ImageFileRepository repository;

    public void ImageOcrWebController(ImageFileRepository repository) {
        this.repository = repository;
    }

    public OcrController(ImageFileRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("files", repository.findAll());
        return "index";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
        if (!file.getContentType().startsWith("image/")) {
            redirectAttributes.addFlashAttribute("error", "Only image files are allowed.");
            return "redirect:/";
        }

        ImageFile image = new ImageFile();
        image.setId(UUID.randomUUID());
        image.setName(file.getOriginalFilename());
        image.setMimeType(file.getContentType());
        image.setContent(file.getBytes());
        repository.save(image);

        return "redirect:/";
    }

    @PostMapping("/ocr/{id}")
    public String ocr(@PathVariable UUID id, RedirectAttributes redirectAttributes) throws IOException {
        ImageFile file = repository.findById(id).orElseThrow();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getContent()));

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/share/tessdata");
        tesseract.setLanguage("rus+eng");

        try {
            String result = tesseract.doOCR(image);
            file.setOcrText(result);
            repository.save(file);
        } catch (TesseractException e) {
            redirectAttributes.addFlashAttribute("error", "OCR failed: " + e.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> viewImage(@PathVariable UUID id) {
        ImageFile file = repository.findById(id).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .body(file.getContent());
    }

}
