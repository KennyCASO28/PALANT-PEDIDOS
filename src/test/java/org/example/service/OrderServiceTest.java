package org.example.service;

import org.example.dao.PedidoDAO;
import org.example.dao.VendedorDAO;
import org.example.dto.OrderRequestDTO;
import org.example.model.DetallePedido;
import org.example.model.TipoPrenda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private PedidoDAO pedidoDAO;

    @Mock
    private VendedorDAO vendedorDAO;

    @InjectMocks
    private OrderService orderService;

    private OrderRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        // Manual injection using the test constructor
        orderService = new OrderService(pedidoDAO, vendedorDAO);

        // Prepare a valid request logic for reuse
        List<DetallePedido> roster = new ArrayList<>();
        // DetallePedido might not have that constructor. Let's use clean defaults.
        DetallePedido p = new DetallePedido("Player 1", "10", "M");
        p.setIncludeTop(true);
        p.setIncludeBottom(true);
        p.setIncludeSocks(true);
        roster.add(p);

        validRequest = new OrderRequestDTO.Builder()
                .clientName("Club Test")
                .sellerName("Juan Perez")
                .orderCode("P-100")
                .tipoPrenda(TipoPrenda.CAMISETA)
                .gender("HOMBRE")
                .deliveryDate(LocalDate.now().plusDays(15))
                .roster(roster)
                .build();
    }

    @Test
    @DisplayName("Should fail when client name is empty")
    void saveOrder_ShouldFail_WhenClientEmpty() {
        OrderRequestDTO invalidRequest = new OrderRequestDTO.Builder()
                .clientName("") // Empty
                .sellerName("Juan")
                .orderCode("P-101")
                .tipoPrenda(TipoPrenda.CAMISETA)
                .deliveryDate(LocalDate.now())
                .roster(Collections.singletonList(new DetallePedido("Test", "0", "M")))
                .build();

        boolean result = orderService.saveOrder(invalidRequest);

        assertFalse(result, "Should return false for empty client");
        verify(pedidoDAO, never()).guardarPedidoCompleto(
                any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                any(), any(), anyList());
    }

    @Test
    @DisplayName("Should fail when garment type is null")
    void saveOrder_ShouldFail_WhenTipoPrendaNull() {
        OrderRequestDTO invalidRequest = new OrderRequestDTO.Builder()
                .clientName("Test")
                .sellerName("Juan")
                .orderCode("P-101")
                .tipoPrenda(null) // NULL
                .deliveryDate(LocalDate.now())
                .roster(Collections.singletonList(new DetallePedido("Test", "0", "M")))
                .build();

        boolean result = orderService.saveOrder(invalidRequest);

        assertFalse(result, "Should return false for null garment type");
    }

    @Test
    @DisplayName("Should call DAO when data is valid")
    void saveOrder_ShouldCallDAO_WhenValid() {
        // Mock DAO success
        when(pedidoDAO.guardarPedidoCompleto(
                eq("Club Test"), anyString(), anyString(), eq("Camiseta"), any(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any(), anyList())).thenReturn(true);

        boolean result = orderService.saveOrder(validRequest);

        assertTrue(result, "Should return true when DAO succeeds");
        verify(pedidoDAO).guardarPedidoCompleto(
                any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                any(), any(), anyList());
    }
}
