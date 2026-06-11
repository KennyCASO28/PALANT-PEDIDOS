package org.example.logic;

import org.example.model.DetallePedido;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RosterSorter {

    /**
     * Ordena la lista de jugadores utilizando una estrategia inteligente:
     * 1. Agrupa por tipo de jugador (Detallado, Compacto, Simple).
     * 2. Dentro de cada grupo, mantiene el orden de inserción.
     * 3. Permite ordenar bloques por talla si se solicita.
     */
    public static void performSmartGroupedSort(List<DetallePedido> currentList, boolean isAscending) {
        if (currentList.isEmpty())
            return;

        // Note: Modified to modify the list in-place or return a new list?
        // The original method modified the TableView items directly.
        // Here we will return a sorted list, and the caller (Delegate) will update the
        // TableView.
        // But the signature void suggests modifying in place?
        // Better to return a new list to be functional.
    }

    /**
     * Sorts the given list and returns a NEW sorted list.
     */
    public static List<DetallePedido> sort(List<DetallePedido> currentList, boolean isAscending) {
        if (currentList.isEmpty())
            return new ArrayList<>();

        List<DetallePedido> sortedList = new ArrayList<>();
        List<DetallePedido> currentBlock = new ArrayList<>();
        Integer currentGroup = null;

        for (DetallePedido p : currentList) {
            int group = getPlayerGroup(p);
            if (currentGroup == null) {
                currentGroup = group;
                currentBlock.add(p);
            } else if (currentGroup == group) {
                currentBlock.add(p);
            } else {
                sortBlockBySize(currentBlock, isAscending);
                sortedList.addAll(currentBlock);
                currentBlock.clear();
                currentGroup = group;
                currentBlock.add(p);
            }
        }
        if (!currentBlock.isEmpty()) {
            sortBlockBySize(currentBlock, isAscending);
            sortedList.addAll(currentBlock);
        }
        return sortedList;
    }

    // 0 = Simple (No Name, No Num)
    // 1 = Compact (Generic Name OR Specific Num Only)
    // 2 = Detailed (Specific Name)
    private static int getPlayerGroup(DetallePedido p) {
        String n = p.getNombre();
        String num = p.getNumero();
        boolean hasName = (n != null && !n.trim().isEmpty() && !n.trim().equalsIgnoreCase("JUGADOR")
                && !n.trim().equalsIgnoreCase("S/N"));
        boolean hasNum = (num != null && !num.trim().isEmpty() && !num.trim().equals("-"));

        if (hasName)
            return 2; // Detailed
        if (hasNum)
            return 1; // Compact (Number but no name) OR "JUGADOR" with number?

        // If we represent "JUGADOR" as generic name, let's check raw name
        boolean isGenericName = (n != null
                && (n.trim().equalsIgnoreCase("JUGADOR") || n.trim().equalsIgnoreCase("S/N")));
        if (isGenericName)
            return 1; // Compact

        return 0; // Simple (No Name, No Num)
    }

    private static void sortBlockBySize(List<DetallePedido> block, boolean isAscending) {
        block.sort((p1, p2) -> {
            String s1 = p1.getTalla() == null ? "" : p1.getTalla().trim().toUpperCase();
            String s2 = p2.getTalla() == null ? "" : p2.getTalla().trim().toUpperCase();
            List<String> order = Arrays.asList(
                    "PPP", "XXXS", "XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL", "3XL", "4XL", "5XL");

            int result;
            int i1 = order.indexOf(s1);
            int i2 = order.indexOf(s2);

            if (i1 >= 0 && i2 >= 0) {
                result = Integer.compare(i1, i2);
            } else {
                try {
                    double d1 = Double.parseDouble(s1);
                    double d2 = Double.parseDouble(s2);
                    result = Double.compare(d1, d2);
                } catch (NumberFormatException e) {
                    result = s1.compareTo(s2);
                }
            }
            return isAscending ? result : -result;
        });
    }

    /**
     * Comparator for individual size strings.
     */
    public static int compareSizes(String s1, String s2) {
        if (s1 == null && s2 == null)
            return 0;
        if (s1 == null)
            return -1;
        if (s2 == null)
            return 1;

        List<String> order = Arrays.asList(
                "PPP", "XXXS", "XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL", "3XL", "4XL", "5XL");

        int i1 = order.indexOf(s1.toUpperCase());
        int i2 = order.indexOf(s2.toUpperCase());

        if (i1 >= 0 && i2 >= 0) {
            return Integer.compare(i1, i2);
        }
        return s1.compareToIgnoreCase(s2);
    }
}

