package com.dclub.api.api;

import com.dclub.api.api.ApiDtos.*;
import com.dclub.api.service.EventApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class EventController {
    private final EventApplicationService service;

    public EventController(EventApplicationService service) {
        this.service = service;
    }

    @GetMapping("/events")
    PageResponse<EventListItem> getEvents() {
        return service.getEvents();
    }

    @GetMapping("/events/{eventId}")
    EventDetail getEvent(@PathVariable long eventId) {
        return service.getEvent(eventId);
    }

    @PostMapping("/events/{eventId}/participation")
    @ResponseStatus(HttpStatus.CREATED)
    JoinEventResponse joinEvent(@PathVariable long eventId) {
        return service.join(eventId);
    }

    @PostMapping("/events/{eventId}/participation/cancel")
    CancelParticipationResponse cancelEventParticipation(@PathVariable long eventId,
                                                         @Valid @RequestBody CancelParticipationRequest request) {
        return service.cancel(eventId, request);
    }
}
