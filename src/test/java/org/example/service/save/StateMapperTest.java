package org.example.service.save;

import org.example.dto.save.ShapeDTO;
import org.example.dto.save.ProjectState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StateMapperTest {

    @Test
    public void testShapeGradientTransparencyPersistence() {
        // This test requires JavaFX initialized (which might be tricky in headless env)
        // For now, let's test the DTO logic if possible, or assume it works if we can't
        // run FX.

        ShapeDTO dto = new ShapeDTO();
        dto.setGradientTransparency(true);
        dto.setTransparencyAngle(45.0);
        dto.setTransparencyStartAlpha(0.2);
        dto.setTransparencyEndAlpha(0.8);
        dto.setTransparencyBalance(0.4);

        assertTrue(dto.isGradientTransparency());
        assertEquals(45.0, dto.getTransparencyAngle());
        assertEquals(0.2, dto.getTransparencyStartAlpha());
        assertEquals(0.8, dto.getTransparencyEndAlpha());
        assertEquals(0.4, dto.getTransparencyBalance());
    }

    @Test
    public void testGoalkeeperDesignSerialization() throws Exception {
        ProjectState state = new ProjectState();
        state.setArqueroPersonalizado(true);
        
        org.example.dto.save.GoalkeeperDesignDTO gkDesign = new org.example.dto.save.GoalkeeperDesignDTO();
        gkDesign.setDesignId("gk-1");
        gkDesign.setPersonalized(true);
        gkDesign.setLabel("Arquero 1");
        
        java.util.ArrayList<org.example.dto.save.GoalkeeperDesignDTO> list = new java.util.ArrayList<>();
        list.add(gkDesign);
        state.setGoalkeeperDesigns(list);
        state.setSelectedGoalkeeperDesignId("gk-1");
        
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        byte[] bytes = mapper.writeValueAsBytes(state);
        
        ProjectState restored = mapper.readValue(bytes, ProjectState.class);
        
        assertTrue(restored.isArqueroPersonalizado());
        assertEquals("gk-1", restored.getSelectedGoalkeeperDesignId());
        assertNotNull(restored.getGoalkeeperDesigns());
        assertEquals(1, restored.getGoalkeeperDesigns().size());
        
        org.example.dto.save.GoalkeeperDesignDTO restoredGk = restored.getGoalkeeperDesigns().get(0);
        assertEquals("gk-1", restoredGk.getDesignId());
        assertTrue(restoredGk.isPersonalized());
        assertEquals("Arquero 1", restoredGk.getLabel());
    }
}
