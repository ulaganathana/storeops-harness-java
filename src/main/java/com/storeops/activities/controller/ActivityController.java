package com.storeops.activities.controller;

import com.storeops.activities.model.Activity;
import com.storeops.activities.model.CreateActivityRequest;
import com.storeops.activities.model.TaskStatus;
import com.storeops.activities.model.UpdateActivityRequest;
import com.storeops.activities.service.ActivityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<Activity> list(@RequestParam(required = false) String programmeId,
                               @RequestParam(required = false) TaskStatus status) {
        return service.list(programmeId, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Activity create(@RequestHeader("X-Staff-Id") String staffId, @Valid @RequestBody CreateActivityRequest request) {
        return service.create(staffId, request);
    }

    @GetMapping("/{id}")
    public Activity get(@PathVariable String id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public Activity update(@RequestHeader("X-Staff-Id") String staffId, @PathVariable String id,
                           @RequestBody UpdateActivityRequest request) {
        return service.update(staffId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Staff-Id") String staffId, @PathVariable String id) {
        service.delete(staffId, id);
    }
}
