package org.example.service;

import org.example.dao.PedidoDAO;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Smart Scheduler for Palant.
 * Rules:
 * - Urgent: 5 days.
 * - Normal: 7 days.
 * - Max Capacity: 4 orders/day.
 * - Working Days: Mon-Sat (Skip Sundays).
 */
public class OrderSchedulerService {

    private final PedidoDAO pedidoDAO;
    private static final int MAX_ORDERS_PER_DAY = 4;

    public OrderSchedulerService() {
        this.pedidoDAO = new PedidoDAO();
    }

    public enum Priority {
        NORMAL("Normal"),
        URGENTE("Urgente");

        private final String label;

        Priority(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * Calculates the smart delivery date.
     * Finds the first valid date (starting from base duration) that has capacity <
     * 4.
     */
    public LocalDate calculateDeliveryDate(Priority priority) {
        int baseDays = (priority == Priority.URGENTE) ? 5 : 7;
        LocalDate candidate = addWorkingDays(LocalDate.now(), baseDays);

        // Find first day with capacity
        while (true) {
            // Count orders for this candidate date
            int currentLoad = pedidoDAO.contarPedidosPorFechaEntrega(candidate);

            if (currentLoad < MAX_ORDERS_PER_DAY) {
                return candidate;
            }

            // If full, try next working day
            candidate = addWorkingDays(candidate, 1);
        }
    }

    /**
     * Adds n working days (skipping Sundays).
     */
    private LocalDate addWorkingDays(LocalDate date, int days) {
        LocalDate result = date;
        int added = 0;
        while (added < days) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }
}
