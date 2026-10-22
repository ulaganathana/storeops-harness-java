package com.storeops.programmes.routes;

import com.storeops.common.security.CurrentUser;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/programmes")
public class ProgrammesController {

    private final ProgrammeService service;

    public ProgrammesController(ProgrammeService service) {
        this.service = service;
    }

    @GetMapping
    public List<Project> list(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return service.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @Valid @RequestBody CreateProgrammeRequest request) {
        return service.create(user, request);
    }

    @PostMapping("/{id}/members")
    public Project addMember(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @PathVariable("id") String id,
            @Valid @RequestBody AddMemberRequest request) {
        return service.addMember(user, id, request.staffId());
    }
}
