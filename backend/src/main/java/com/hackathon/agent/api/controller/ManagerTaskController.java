package com.hackathon.agent.api.controller;

import com.hackathon.agent.infrastructure.persistence.entity.ManagerTaskEntity;
import com.hackathon.agent.infrastructure.persistence.repository.ManagerTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/tasks")
@RequiredArgsConstructor
public class ManagerTaskController {

    private final ManagerTaskRepository repository;

    @GetMapping
    public List<ManagerTaskDto> list(@RequestParam(defaultValue = "50") int limit) {
        return repository.findAll(
                        PageRequest.of(0, Math.min(limit, 200), Sort.by("createdAt").descending()))
                .map(this::toDto)
                .toList();
    }

    @PatchMapping("/{id}")
    public ManagerTaskDto setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ManagerTaskEntity e = repository.findById(id).orElseThrow();
        e.setStatus(body.getOrDefault("status", e.getStatus()));
        return toDto(repository.save(e));
    }

    private ManagerTaskDto toDto(ManagerTaskEntity t) {
        return new ManagerTaskDto(t.getId(), t.getReason(), t.getClientPhone(),
                t.getStatus(), t.getCreatedAt(), t.getDialogHistory());
    }

    public record ManagerTaskDto(Long id, String reason, String clientPhone,
                                 String status, java.time.LocalDateTime createdAt,
                                 String dialogHistory) {}
}