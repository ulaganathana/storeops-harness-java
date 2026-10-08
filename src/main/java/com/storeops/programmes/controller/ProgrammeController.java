package com.storeops.programmes.controller;

import com.storeops.programmes.model.AddMemberRequest;
import com.storeops.programmes.model.CreateProgrammeRequest;
import com.storeops.programmes.model.Project;
import com.storeops.programmes.service.ProgrammeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/programmes")
public class ProgrammeController {
    private final ProgrammeService service;

    public ProgrammeController(ProgrammeService service) {
        this.service = service;
    }

    @GetMapping
    public List<Project> list(@RequestHeader("X-Staff-Id") String staffId) {
        return service.listForStore(staffId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@RequestHeader("X-Staff-Id") String staffId, @Valid @RequestBody CreateProgrammeRequest request) {
        return service.create(staffId, request);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public Project addMember(@RequestHeader("X-Staff-Id") String staffId, @PathVariable String id,
                             @Valid @RequestBody AddMemberRequest request) {
        return service.addMember(staffId, id, request);
    }
}
