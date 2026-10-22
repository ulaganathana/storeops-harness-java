package com.storeops.activities.routes;

import com.storeops.activities.model.Activity;
import com.storeops.activities.model.ActivityStatus;
import com.storeops.activities.model.BulkStatusRequest;
import com.storeops.activities.model.BulkStatusResult;
import com.storeops.activities.model.CreateActivityRequest;
import com.storeops.activities.model.UpdateActivityRequest;
import com.storeops.activities.service.ActivityService;
import com.storeops.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
public class ActivitiesController {

    private final ActivityService service;

    public ActivitiesController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<Activity> list(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @RequestParam(name = "programmeId", required = false) String programmeId,
            @RequestParam(name = "status", required = false) ActivityStatus status) {
        return service.list(user, programmeId, status);
    }

    @PatchMapping("/bulk-status")
    public ResponseEntity<BulkStatusResult> bulkUpdateStatus(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @Valid @RequestBody BulkStatusRequest request) {
        BulkStatusResult result = service.bulkUpdateStatus(user, request);
        long succeeded = result.results().stream()
                .filter(item -> item.outcome().equals("SUCCEEDED"))
                .count();
        long failed = result.results().size() - succeeded;
        if (failed == 0) {
            return ResponseEntity.ok(result);
        }
        if (succeeded > 0) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Activity create(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @Valid @RequestBody CreateActivityRequest request) {
        return service.create(user, request);
    }

    @GetMapping("/{id}")
    public Activity get(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @PathVariable("id") String id) {
        return service.get(user, id);
    }

    @PatchMapping("/{id}")
    public Activity update(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @PathVariable("id") String id,
            @RequestBody UpdateActivityRequest request) {
        return service.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @PathVariable("id") String id) {
        service.delete(user, id);
    }
}
