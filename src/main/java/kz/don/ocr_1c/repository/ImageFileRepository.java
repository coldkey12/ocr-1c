package kz.don.ocr_1c.repository;

import kz.don.ocr_1c.model.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageFileRepository extends JpaRepository<ImageFile, UUID> {
}
