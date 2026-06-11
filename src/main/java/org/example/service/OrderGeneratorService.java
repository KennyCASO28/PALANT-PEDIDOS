package org.example.service;

import org.example.model.DetallePedido;
import org.example.model.TipoGenero;

import java.util.ArrayList;
import java.util.List;

public class OrderGeneratorService {

    /**
     * Genera una lista de jugadores con números correlativos según las reglas de
     * negocio.
     *
     * Reglas:
     * - Hombres: El #1 se reserva para arquero. Jugadores de campo inician en 2.
     * - Mujeres: Jugadoras de campo pueden usar el #1.
     * - Arqueros: Tienen prioridad sobre el 1 y el 21.
     *
     * @param genero            Género del equipo (HOMBRE/MUJER)
     * @param cantidadJugadores Cantidad de jugadores de campo (SIN contar arqueros)
     * @param cantidadArqueros  Cantidad de arqueros
     * @return Lista de DetallePedido generada
     */
    public List<DetallePedido> generarCorrelativos(TipoGenero genero, int cantidadJugadores, int cantidadArqueros) {
        List<DetallePedido> lista = new ArrayList<>();

        // 1. Generar Arqueros
        // Regla: Arqueros usan 1, luego 21, luego consecutivo libre si hay más (aunque
        // usualmente son 1 o 2)
        int[] arquerosReservados = { 1, 21, 12 }; // Prioridad de números para arqueros

        for (int i = 0; i < cantidadArqueros; i++) {
            String numero;
            if (i < arquerosReservados.length) {
                numero = String.valueOf(arquerosReservados[i]);
            } else {
                // Si hay más de 3 arqueros (raro), asignar 22 en adelante
                numero = String.valueOf(22 + (i - 3));
            }
            lista.add(new DetallePedido("ARQUERO " + (i + 1), numero, "L")); // Talla default L
        }

        // 2. Determinar inicio de correlativo para jugadores de campo
        int numeroActual = 1;

        if (genero == TipoGenero.HOMBRE) {
            // En hombres, el 1 está reservado para arquero, iniciamos en 2
            numeroActual = 2;
        } else {
            // En mujeres, el 1 está permitido para campo si no lo usó un arquero
            // Pero si YA hay un arquero que usó el 1, debemos saltarlo
            if (cantidadArqueros > 0) {
                // Asumimos que el primer arquero tomó el 1
                numeroActual = 2;
            } else {
                numeroActual = 1;
            }
        }

        // 3. Generar Jugadores de Campo
        for (int i = 0; i < cantidadJugadores; i++) {
            // Evitar conflictos con números ya asignados a arqueros (e.g. 21)
            while (esNumeroOcupado(numeroActual, lista)) {
                numeroActual++;
            }

            lista.add(new DetallePedido("JUGADOR " + (i + 1), String.valueOf(numeroActual), "L")); // Talla default L
            numeroActual++;
        }

        return lista;
    }

    private boolean esNumeroOcupado(int numero, List<DetallePedido> lista) {
        String numStr = String.valueOf(numero);
        for (DetallePedido p : lista) {
            if (p.getNumero().equals(numStr)) {
                return true;
            }
        }
        return false;
    }
}

