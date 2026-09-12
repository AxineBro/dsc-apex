package com.hackathon.agent.infrastructure.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.agent.api.dto.response.ChatAttachment;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.service.ApartmentSearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplexCatalog {

    public record Complex(String name, String imageUrl, String district) {}

    private final ApartmentSearchService searchService;
    private final ObjectMapper objectMapper;

    @Value("${app.data.complexes}")
    private String complexesPath;

    private Map<String, Complex> byName = Map.of();

    @PostConstruct
    public void load() {
        try (var in = new ClassPathResource(complexesPath).getInputStream()) {
            List<Complex> list = objectMapper.readValue(in, new TypeReference<>() {});
            Map<String, Complex> m = new HashMap<>();
            for (var c : list) m.put(norm(c.name()), c);
            byName = Map.copyOf(m);
            log.info("Loaded {} complexes from {}", byName.size(), complexesPath);
        } catch (Exception e) {
            log.error("Failed to load complexes from {}: {}", complexesPath, e.getMessage(), e);
            byName = Map.of();
        }
    }

    public List<ChatAttachment> attachmentsFor(Session session) {
        try {
            if (session == null || session.getRankedList() == null || session.getRankedList().isEmpty())
                return List.of();
            LinkedHashMap<String, Complex> uniq = new LinkedHashMap<>();
            for (Long id : session.getRankedList().stream().limit(10).toList()) {
                var a = searchService.getById(id);
                if (a == null || a.getComplexName() == null) continue;
                Complex c = byName.get(norm(a.getComplexName()));
                if (c != null) uniq.putIfAbsent(c.name(), c);
                if (uniq.size() >= 3) break;
            }
            return uniq.values().stream()
                    .map(c -> new ChatAttachment("complex_image", c.imageUrl(), c.name()))
                    .toList();
        } catch (Exception e) {
            log.warn("attachmentsFor failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.replaceAll("[«»\"]", "").trim().toLowerCase(Locale.ROOT);
    }
}