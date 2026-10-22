package com.storeops.alerts.routes;

import com.storeops.alerts.model.Alert;
import com.storeops.alerts.service.AlertService;
import com.storeops.common.security.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertsController {

    private final AlertService service;

    public AlertsController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public List<Alert> list(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return service.listForUser(user.id());
    }
}
