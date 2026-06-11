package org.example.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.example.dto.save.PrendaStateDTO;

import java.util.List;
import java.util.Collection;

/**
 * Utilidad basada en Google Guava para la agrupación avanzada de órdenes y prendas.
 * Implementado para optimizar el rendimiento del panel Kanban en el área de Taller.
 */
public class GuavaOrderUtil {

    /**
     * Agrupa una lista plana de prendas según su talla de forma automática
     * utilizando un Multimap de Google Guava.
     *
     * @param prendas Lista completa de prendas de un pedido
     * @return Multimap que asocia cada talla con sus respectivas prendas
     */
    public static Multimap<String, PrendaStateDTO> agruparPrendasPorTalla(List<PrendaStateDTO> prendas) {
        Multimap<String, PrendaStateDTO> prendasAgrupadas = ArrayListMultimap.create();
        
        if (prendas != null) {
            for (PrendaStateDTO prenda : prendas) {
                // Si la talla es nula, la agrupa en la categoría "Desconocida"
                String talla = (prenda.getTalla() != null && !prenda.getTalla().isEmpty()) 
                                ? prenda.getTalla() : "Desconocida";
                
                prendasAgrupadas.put(talla, prenda);
            }
        }
        
        return prendasAgrupadas;
    }

    /**
     * Obtiene rápidamente todas las prendas asociadas a una talla específica.
     */
    public static Collection<PrendaStateDTO> obtenerPrendasDeTalla(Multimap<String, PrendaStateDTO> mapa, String talla) {
        return mapa.get(talla);
    }
}
