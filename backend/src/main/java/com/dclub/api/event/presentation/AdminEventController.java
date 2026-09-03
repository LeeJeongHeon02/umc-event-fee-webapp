package com.dclub.api.event.presentation;

import com.dclub.api.event.application.AdminEventApplicationService;
import com.dclub.api.global.presentation.ApiDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
public class AdminEventController {
    private final AdminEventApplicationService service;

    public AdminEventController(AdminEventApplicationService service) {
        this.service = service;
    }

    @GetMapping
    List<AdminEventResponse> getAll() {
        return service.getAll();
    }

    @PostMapping
    ResponseEntity<AdminEventResponse> create(@Valid @RequestBody AdminEventCreateRequest request) {
        AdminEventResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/events/" + response.id())).body(response);
    }

    @GetMapping("/{eventId}")
    AdminEventResponse get(@PathVariable long eventId) {
        return service.get(eventId);
    }

    @PatchMapping("/{eventId}")
    AdminEventResponse update(@PathVariable long eventId,
                              @Valid @RequestBody AdminEventUpdateRequest request) {
        return service.update(eventId, request);
    }

    @PostMapping("/{eventId}/publish")
    AdminEventResponse publish(@PathVariable long eventId,
                               @Valid @RequestBody AdminEventVersionRequest request) {
        return service.publish(eventId, request);
    }

    @PostMapping("/{eventId}/close")
    AdminEventResponse close(@PathVariable long eventId,
                             @Valid @RequestBody AdminEventTransitionRequest request) {
        return service.close(eventId, request);
    }

    @PostMapping("/{eventId}/cancel")
    AdminEventCancelResponse cancel(@PathVariable long eventId,
                                    @Valid @RequestBody AdminEventTransitionRequest request) {
        return service.cancel(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    ResponseEntity<Void> delete(@PathVariable long eventId, @RequestParam long version) {
        service.delete(eventId, version);
        return ResponseEntity.noContent().build();
    }
}
