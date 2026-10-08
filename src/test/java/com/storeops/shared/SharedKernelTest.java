package com.storeops.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.storeops.shared.error.AppError;
import com.storeops.shared.error.ConflictError;
import com.storeops.shared.error.ForbiddenError;
import com.storeops.shared.error.NotFoundError;
import com.storeops.shared.error.ValidationError;
import com.storeops.shared.event.DomainEvent;
import com.storeops.shared.event.SpringEventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SharedKernelTest {

    @Test
    void appErrorSubclassesCarryCodeMessageAndStatus() {
        List<AppError> errors = List.of(new NotFoundError("Activity", "x"), new ValidationError("bad"),
            new ForbiddenError("no"), new ConflictError("dup"));
        assertThat(errors).extracting(AppError::getStatusCode).containsExactly(404, 400, 403, 409);
        assertThat(errors).extracting(AppError::getCode)
            .containsExactly("NOT_FOUND", "VALIDATION_ERROR", "FORBIDDEN", "CONFLICT");
        assertThat(errors.get(0).getMessage()).contains("Activity", "x");
    }

    @Test
    void eventBusPublishesDomainEventWithTypeAndPayload() {
        List<Object> published = new ArrayList<>();
        ApplicationEventPublisher publisher = published::add;
        new SpringEventBus(publisher).emit("X", Map.of("k", "v"));
        assertThat(published).hasSize(1);
        DomainEvent e = (DomainEvent) published.get(0);
        assertThat(e.type()).isEqualTo("X");
        assertThat(e.payload()).containsEntry("k", "v");
        assertThat(e.occurredAt()).isNotNull();
    }
}
