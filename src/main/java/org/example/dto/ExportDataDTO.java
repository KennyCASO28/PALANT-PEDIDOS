package org.example.dto;

import javafx.scene.image.Image;
import org.example.component.PrendaVisualizer;
import org.example.model.DetallePedido;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for export operations.
 * Encapsulates all necessary context to generate PDF or SVG exports.
 */
public class ExportDataDTO {
    private final File targetFile;
    private final PrendaVisualizer visualizer;
    private final ConfiguracionPrendaDTO config;
    private final String orderCode;
    private final String clientName;
    private final String sellerName;
    private final DatosEnvioDTO shippingInfo;
    private final List<DetallePedido> roster;
    private final LocalDate deliveryDate;
    private final String priority;
    private final String shortType;
    private final List<Image> referenceImages;
    private final ConfiguracionPrendaDTO arqueroConfig;
    private final Image arqueroSnapshotHombre;
    private final Image arqueroSnapshotMujer;
    private final Image mainGarmentSnapshot;
    private final List<org.example.service.PdfExportService.ShieldEntry> shields;
    private final java.util.Map<String, org.example.dto.save.GoalkeeperDesignDTO> disenosArquero;

    private ExportDataDTO(Builder builder) {
        this.targetFile = builder.targetFile;
        this.visualizer = builder.visualizer;
        this.config = builder.config;
        this.orderCode = builder.orderCode;
        this.clientName = builder.clientName;
        this.sellerName = builder.sellerName;
        this.shippingInfo = builder.shippingInfo;
        this.roster = new ArrayList<>(builder.roster);
        this.deliveryDate = builder.deliveryDate;
        this.priority = builder.priority;
        this.shortType = builder.shortType;
        this.referenceImages = new ArrayList<>(builder.referenceImages);
        this.arqueroConfig = builder.arqueroConfig;
        this.arqueroSnapshotHombre = builder.arqueroSnapshotHombre;
        this.arqueroSnapshotMujer = builder.arqueroSnapshotMujer;
        this.mainGarmentSnapshot = builder.mainGarmentSnapshot;
        this.shields = new ArrayList<>(builder.shields);
        this.disenosArquero = builder.disenosArquero;
    }

    // Getters
    public File getTargetFile() {
        return targetFile;
    }

    public PrendaVisualizer getVisualizer() {
        return visualizer;
    }

    public ConfiguracionPrendaDTO getConfig() {
        return config;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getClientName() {
        return clientName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public DatosEnvioDTO getShippingInfo() {
        return shippingInfo;
    }

    public List<DetallePedido> getRoster() {
        return roster;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public String getPriority() {
        return priority;
    }

    public String getShortType() {
        return shortType;
    }

    public List<Image> getReferenceImages() {
        return referenceImages;
    }

    public ConfiguracionPrendaDTO getArqueroConfig() {
        return arqueroConfig;
    }

    public Image getArqueroSnapshotHombre() {
        return arqueroSnapshotHombre;
    }

    public Image getArqueroSnapshotMujer() {
        return arqueroSnapshotMujer;
    }

    public Image getMainGarmentSnapshot() {
        return mainGarmentSnapshot;
    }

    public List<org.example.service.PdfExportService.ShieldEntry> getShields() {
        return shields;
    }

    public java.util.Map<String, org.example.dto.save.GoalkeeperDesignDTO> getDisenosArquero() {
        return disenosArquero;
    }

    public static class Builder {
        private File targetFile;
        private PrendaVisualizer visualizer;
        private ConfiguracionPrendaDTO config;
        private String orderCode;
        private String clientName;
        private String sellerName;
        private DatosEnvioDTO shippingInfo;
        private List<DetallePedido> roster = new ArrayList<>();
        private LocalDate deliveryDate;
        private String priority = "NORMAL";
        private String shortType = "Short";
        private List<Image> referenceImages = new ArrayList<>();
        private ConfiguracionPrendaDTO arqueroConfig;
        private Image arqueroSnapshotHombre;
        private Image arqueroSnapshotMujer;
        private Image mainGarmentSnapshot;
        private List<org.example.service.PdfExportService.ShieldEntry> shields = new ArrayList<>();
        private java.util.Map<String, org.example.dto.save.GoalkeeperDesignDTO> disenosArquero;

        public Builder targetFile(File targetFile) {
            this.targetFile = targetFile;
            return this;
        }

        public Builder visualizer(PrendaVisualizer visualizer) {
            this.visualizer = visualizer;
            return this;
        }

        public Builder config(ConfiguracionPrendaDTO config) {
            this.config = config;
            return this;
        }

        public Builder orderCode(String orderCode) {
            this.orderCode = orderCode;
            return this;
        }

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder sellerName(String sellerName) {
            this.sellerName = sellerName;
            return this;
        }

        public Builder shippingInfo(DatosEnvioDTO shippingInfo) {
            this.shippingInfo = shippingInfo;
            return this;
        }

        public Builder roster(List<DetallePedido> roster) {
            this.roster = roster;
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

        public Builder shortType(String shortType) {
            this.shortType = shortType;
            return this;
        }

        public Builder referenceImages(List<Image> referenceImages) {
            this.referenceImages = referenceImages;
            return this;
        }

        public Builder arqueroConfig(ConfiguracionPrendaDTO arqueroConfig) {
            this.arqueroConfig = arqueroConfig;
            return this;
        }

        public Builder arqueroSnapshotHombre(Image img) {
            this.arqueroSnapshotHombre = img;
            return this;
        }

        public Builder arqueroSnapshotMujer(Image img) {
            this.arqueroSnapshotMujer = img;
            return this;
        }

        public Builder mainGarmentSnapshot(Image mainGarmentSnapshot) {
            this.mainGarmentSnapshot = mainGarmentSnapshot;
            return this;
        }

        public Builder shields(List<org.example.service.PdfExportService.ShieldEntry> shields) {
            this.shields = shields;
            return this;
        }

        public Builder disenosArquero(java.util.Map<String, org.example.dto.save.GoalkeeperDesignDTO> disenosArquero) {
            this.disenosArquero = disenosArquero;
            return this;
        }

        public ExportDataDTO build() {
            return new ExportDataDTO(this);
        }
    }
}

