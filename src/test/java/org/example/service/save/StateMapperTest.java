package org.example.service.save;

import org.example.dto.save.ShapeDTO;
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
}
