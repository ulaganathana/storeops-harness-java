package com.storeops.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.EventType;
import com.storeops.common.events.InMemoryEventBus;
import com.storeops.common.util.Ids;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventBusAndIdsTest {

    @Test
    void publishDeliversOnlyToSubscribersOfThatType() {
        InMemoryEventBus bus = new InMemoryEventBus();
        List<DomainEvent> assigned = new ArrayList<>();
        List<DomainEvent> added = new ArrayList<>();
        bus.subscribe(EventType.ACTIVITY_ASSIGNED, assigned::add);
        bus.subscribe(EventType.PROGRAMME_MEMBER_ADDED, added::add);

        bus.publish(DomainEvent.of(EventType.ACTIVITY_ASSIGNED, "u-1", "hi"));

        assertEquals(1, assigned.size());
        assertTrue(added.isEmpty());
    }

    @Test
    void publishWithoutSubscribersIsANoOp() {
        new InMemoryEventBus().publish(DomainEvent.of(EventType.ACTIVITY_ASSIGNED, "u-1", "hi"));
    }

    @Test
    void idsArePrefixedAndUnique() {
        String first = Ids.next("act");

        assertTrue(first.startsWith("act-"));
        assertNotEquals(first, Ids.next("act"));
    }
}
