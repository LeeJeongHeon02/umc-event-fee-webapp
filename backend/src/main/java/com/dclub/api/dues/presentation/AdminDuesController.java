package com.dclub.api.dues.presentation;

import com.dclub.api.dues.application.AdminDuesApplicationService;
import com.dclub.api.global.presentation.ApiDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dues-rounds")
public class AdminDuesController {
    private final AdminDuesApplicationService service;

    public AdminDuesController(AdminDuesApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminDuesRoundResponse> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDuesRoundResponse create(@Valid @RequestBody AdminDuesRoundRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{duesRoundId}")
    public AdminDuesRoundResponse update(@PathVariable long duesRoundId,
                                         @Valid @RequestBody AdminDuesRoundRequest request) {
        return service.update(duesRoundId, request);
    }

    @PostMapping("/{duesRoundId}/publish")
    public AdminDuesPublishResponse publish(@PathVariable long duesRoundId,
                                             @Valid @RequestBody AdminEventVersionRequest request) {
        return service.publish(duesRoundId, request);
    }

    @DeleteMapping("/{duesRoundId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long duesRoundId, @RequestParam long version) {
        service.delete(duesRoundId, version);
    }
}
