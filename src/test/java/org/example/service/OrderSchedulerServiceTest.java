package org.example.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class OrderSchedulerServiceTest {

    private final OrderSchedulerService service = new OrderSchedulerService();

    @Test
    @DisplayName("Should calculate delivery date correctly for Normal priority")
    void testCalculateDeliveryDateNormal() {
        LocalDate now = LocalDate.now();
        LocalDate result = service.calculateDeliveryDate(OrderSchedulerService.Priority.NORMAL);

        // Normal priority is 7 working days (not 12)
        // 7 working days + max 2 weekends (if start Friday) = ~9-10 days real time
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now, result);
        assertTrue(daysDiff >= 7 && daysDiff <= 10, "Delivery date should be ~7-10 days away (7 working days)");
    }

    @Test
    @DisplayName("Should never deliver on a Sunday")
    void testNoSundayDelivery() {
        // We find a day that would result in Sunday
        // This is a bit hard without mocking LocalDate.now(), but we can test the logic
        // by observing that the result is never Sunday
        for (OrderSchedulerService.Priority p : OrderSchedulerService.Priority.values()) {
            LocalDate result = service.calculateDeliveryDate(p);
            assertNotEquals(DayOfWeek.SUNDAY, result.getDayOfWeek(),
                    "Delivery date for priority " + p + " should not be Sunday");
        }
    }
}
