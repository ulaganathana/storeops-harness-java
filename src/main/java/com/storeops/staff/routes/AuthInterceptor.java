package com.storeops.staff.routes;

import com.storeops.common.security.CurrentUser;
import com.storeops.staff.service.StaffService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Resolves the caller from the X-User-Id header and exposes it as a request attribute. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_HEADER = "X-User-Id";

    private final StaffService staffService;

    public AuthInterceptor(StaffService staffService) {
        this.staffService = staffService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CurrentUser user = staffService.authenticate(request.getHeader(USER_HEADER));
        request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, user);
        return true;
    }
}
