package com.storeops.alerts.controller;

import com.storeops.alerts.model.Notification;
import com.storeops.alerts.service.AlertService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notification> mine(@RequestHeader("X-Staff-Id") String staffId) {
        return service.forUser(staffId);
    }
}
