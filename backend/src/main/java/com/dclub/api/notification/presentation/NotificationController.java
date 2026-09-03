package com.dclub.api.notification.presentation;

import com.dclub.api.global.presentation.ApiDtos.NotificationItem;
import com.dclub.api.global.presentation.ApiDtos.NotificationResponse;
import com.dclub.api.notification.application.NotificationApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationApplicationService service;
    public NotificationController(NotificationApplicationService service) { this.service = service; }
    @GetMapping NotificationResponse list() { return service.list(); }
    @PostMapping("/{notificationId}/read") NotificationItem read(@PathVariable long notificationId) { return service.read(notificationId); }
    @PostMapping("/read-all") @ResponseStatus(HttpStatus.NO_CONTENT) void readAll() { service.readAll(); }
}
