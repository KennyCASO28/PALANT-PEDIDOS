package org.example.dto;

import org.example.model.DetallePedido;
import org.example.model.TipoPrenda;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for creating a new order.
 */
public class OrderRequestDTO {
    private final String clientName;
    private final String sellerName;
    private final String orderCode;
    private final TipoPrenda tipoPrenda;
    private final String gender;
    private final String sleeveInfo;
    private final String neckInfo;
    private final boolean hasMesh;
    private final boolean hasSocks;
    private final boolean hasShirtCuffs;
    private final boolean hasShortCuffs;
    private final LocalDate deliveryDate;
    private final String priority;
    private final List<DetallePedido> roster;

    public OrderRequestDTO(Builder builder) {
        this.clientName = builder.clientName;
        this.sellerName = builder.sellerName;
        this.orderCode = builder.orderCode;
        this.tipoPrenda = builder.tipoPrenda;
        this.gender = builder.gender;
        this.sleeveInfo = builder.sleeveInfo;
        this.neckInfo = builder.neckInfo;
        this.hasMesh = builder.hasMesh;
        this.hasSocks = builder.hasSocks;
        this.hasShirtCuffs = builder.hasShirtCuffs;
        this.hasShortCuffs = builder.hasShortCuffs;
        this.deliveryDate = builder.deliveryDate;
        this.priority = builder.priority;
        this.roster = new ArrayList<>(builder.roster);
    }

    // Getters
    public String getClientName() {
        return clientName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public TipoPrenda getTipoPrenda() {
        return tipoPrenda;
    }

    public String getGender() {
        return gender;
    }

    public String getSleeveInfo() {
        return sleeveInfo;
    }

    public String getNeckInfo() {
        return neckInfo;
    }

    public boolean isHasMesh() {
        return hasMesh;
    }

    public boolean isHasSocks() {
        return hasSocks;
    }

    public boolean isHasShirtCuffs() {
        return hasShirtCuffs;
    }

    public boolean isHasShortCuffs() {
        return hasShortCuffs;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public String getPriority() {
        return priority;
    }

    public List<DetallePedido> getRoster() {
        return roster;
    }

    public static class Builder {
        private String clientName;
        private String sellerName;
        private String orderCode;
        private TipoPrenda tipoPrenda;
        private String gender;
        private String sleeveInfo;
        private String neckInfo;
        private boolean hasMesh;
        private boolean hasSocks;
        private boolean hasShirtCuffs;
        private boolean hasShortCuffs;
        private LocalDate deliveryDate;
        private String priority;
        private List<DetallePedido> roster;

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder sellerName(String sellerName) {
            this.sellerName = sellerName;
            return this;
        }

        public Builder orderCode(String orderCode) {
            this.orderCode = orderCode;
            return this;
        }

        public Builder tipoPrenda(TipoPrenda tipoPrenda) {
            this.tipoPrenda = tipoPrenda;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder sleeveInfo(String sleeveInfo) {
            this.sleeveInfo = sleeveInfo;
            return this;
        }

        public Builder neckInfo(String neckInfo) {
            this.neckInfo = neckInfo;
            return this;
        }

        public Builder hasMesh(boolean hasMesh) {
            this.hasMesh = hasMesh;
            return this;
        }

        public Builder hasSocks(boolean hasSocks) {
            this.hasSocks = hasSocks;
            return this;
        }

        public Builder hasShirtCuffs(boolean hasShirtCuffs) {
            this.hasShirtCuffs = hasShirtCuffs;
            return this;
        }

        public Builder hasShortCuffs(boolean hasShortCuffs) {
            this.hasShortCuffs = hasShortCuffs;
            return this;
        }

        public Builder deliveryDate(LocalDate deliveryDate) {
            this.deliveryDate = deliveryDate;
            return this;
        }

        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public Builder roster(List<DetallePedido> roster) {
            this.roster = roster;
            return this;
        }

        public OrderRequestDTO build() {
            return new OrderRequestDTO(this);
        }
    }
}

