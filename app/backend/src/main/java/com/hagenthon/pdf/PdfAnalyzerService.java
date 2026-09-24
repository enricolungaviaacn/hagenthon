package com.hagenthon.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class PdfAnalyzerService {

    public String extractText(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("File non trovato: {}", filePath);
            return "";
        }
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.debug("Estratti {} caratteri da {}", text.length(), filePath);
            return text;
        } catch (Exception e) {
            log.error("Errore estrazione testo PDF da {}: {}", filePath, e.getMessage());
            return "";
        }
    }
}
