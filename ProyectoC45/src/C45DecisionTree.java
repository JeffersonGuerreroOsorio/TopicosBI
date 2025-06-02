import java.util.*;

public class C45DecisionTree {
    private TreeNode root;

    // CAMBIO: Tipo de datos de 'data' y 'instance' a Map<String, Double>
    public void train(List<Map<String, Double>> data, List<String> attributes, String target) {
        root = buildTree(data, attributes, target);
    }

    // CAMBIO: Tipo de datos de 'instance' a Map<String, Double>
    public String predict(Map<String, Double> instance) {
        TreeNode node = root;
        while (!node.isLeaf()) {
            Double val = instance.get(node.attribute); // Obtener directamente como Double

            // --- Manejo de Double.NaN (valores faltantes) durante la predicción ---
            if (val == null || val.isNaN()) {
                // Si el valor es nulo o NaN, puedes:
                // 1. Devolver la clase mayoritaria de este nodo (simple para empezar)
                // 2. Elegir la rama que contenga más ejemplos del conjunto de entrenamiento
                // 3. Devolver un indicador de "no se puede predecir" o una clase por defecto
                // Por ahora, para simplificar y evitar errores, lo enviamos por una rama predeterminada (ej. izquierda)
                // O podemos retornar la clase mayoritaria del subárbol si lo conocemos (más complejo)
                // Para este ejemplo, simplifiquemos a que si es NaN, va por la rama izquierda
                // En una implementación real, esto debería ser más sofisticado.
                System.out.println("Advertencia: Valor NaN encontrado para '" + node.attribute + "' durante la predicción. Recorriendo rama izquierda por defecto.");
                node = node.left; // O podrías tener una lógica más inteligente aquí.
                if (node == null) { // Caso borde si la rama izquierda es nula
                    return "NoClassFound"; // O la clase mayoritaria general
                }
            } else {
                node = (val <= node.threshold) ? node.left : node.right;
            }
        }
        return node.label;
    }

    // CAMBIO: Tipo de datos de 'data' a Map<String, Double>
    private TreeNode buildTree(List<Map<String, Double>> data, List<String> attributes, String target) {
        if (data.isEmpty()) return null;

        if (allSameClass(data, target)) {
            TreeNode leaf = new TreeNode();
            leaf.label = data.get(0).get(target).toString(); // Convertir el Double (0.0 o 1.0) a String para la etiqueta
            return leaf;
        }

        if (attributes.isEmpty()) {
            TreeNode leaf = new TreeNode();
            leaf.label = majorityClass(data, target);
            return leaf;
        }

        String bestAttr = null;
        double bestGainRatio = -1;
        double bestThreshold = 0.0;

        for (String attr : attributes) {
            // Saltarse el atributo si todos sus valores son NaN en el subconjunto actual
            boolean allNaNForAttr = true;
            for(Map<String, Double> row : data) {
                if (row.get(attr) != null && !row.get(attr).isNaN()) {
                    allNaNForAttr = false;
                    break;
                }
            }
            if (allNaNForAttr) {
                continue; // Saltar este atributo, no se puede dividir por él
            }


            double[] result = gainRatio(data, attr, target);
            if (result[0] > bestGainRatio) {
                bestGainRatio = result[0];
                bestThreshold = result[1];
                bestAttr = attr;
            }
        }

        // Si no se pudo encontrar ningún atributo con una ganancia de información útil
        // (ej. todos los atributos restantes tienen NaN o la ganancia es 0)
        if (bestAttr == null || bestGainRatio <= 0) {
            TreeNode leaf = new TreeNode();
            leaf.label = majorityClass(data, target);
            return leaf;
        }


        TreeNode node = new TreeNode();
        node.attribute = bestAttr;
        node.threshold = bestThreshold;

        // CAMBIO: Tipo de datos de 'left' y 'right' a Map<String, Double>
        List<Map<String, Double>> left = new ArrayList<>();
        List<Map<String, Double>> right = new ArrayList<>();

        for (Map<String, Double> row : data) {
            Double val = row.get(bestAttr); // Obtener directamente como Double
            // Manejo de NaN durante la división: si el valor es NaN, ignorar esta fila para esta división.
            // O, una estrategia común para C4.5 es enviar la fila por AMBAS ramas y ponderar los resultados.
            // Para simplificar, la ignoraremos si es NaN para la división actual.
            if (val != null && !val.isNaN()) { // Solo considerar filas con valor válido para la división
                if (val <= bestThreshold) left.add(row);
                else right.add(row);
            }
        }

        // Si una de las ramas queda vacía debido al manejo de NaN o por datos sesgados,
        // el nodo actual se convierte en una hoja con la clase mayoritaria.
        if (left.isEmpty() || right.isEmpty()) {
            TreeNode leaf = new TreeNode();
            leaf.label = majorityClass(data, target);
            return leaf;
        }


        List<String> newAttrs = new ArrayList<>(attributes);
        newAttrs.remove(bestAttr);

        node.left = buildTree(left, newAttrs, target);
        node.right = buildTree(right, newAttrs, target);
        return node;
    }

    // CAMBIO: Tipo de datos de 'data' a Map<String, Double>
    private boolean allSameClass(List<Map<String, Double>> data, String target) {
        if (data.isEmpty()) return true; // Si no hay datos, se considera que tienen la misma clase (base case)
        Double label = data.get(0).get(target);
        for (Map<String, Double> row : data) {
            if (!row.get(target).equals(label)) return false;
        }
        return true;
    }

    // CAMBIO: Tipo de datos de 'data' a Map<String, Double>
    private String majorityClass(List<Map<String, Double>> data, String target) {
        if (data.isEmpty()) return "N/A"; // Si no hay datos para calcular mayoría

        Map<Double, Integer> count = new HashMap<>(); // Usar Double para las etiquetas
        for (Map<String, Double> row : data) {
            Double label = row.get(target);
            if (label != null && !label.isNaN()) { // Asegurarse de que la etiqueta no sea NaN
                count.put(label, count.getOrDefault(label, 0) + 1);
            }
        }
        if (count.isEmpty()) return "N/A"; // Si todas las etiquetas son NaN

        // Encontrar la clase con mayor conteo
        Double majorityLabel = null;
        int maxCount = -1;
        for (Map.Entry<Double, Integer> entry : count.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                majorityLabel = entry.getKey();
            }
        }
        return majorityLabel != null ? majorityLabel.toString() : "N/A"; // Devolver la etiqueta como String
    }

    // CAMBIO: Tipo de datos de 'data' a Map<String, Double>
    private double entropy(List<Map<String, Double>> data, String target) {
        if (data.isEmpty()) return 0.0; // Entropía de un conjunto vacío es 0

        Map<Double, Integer> count = new HashMap<>(); // Usar Double para las etiquetas
        int validRows = 0; // Contar solo filas con etiquetas válidas
        for (Map<String, Double> row : data) {
            Double label = row.get(target);
            if (label != null && !label.isNaN()) {
                count.put(label, count.getOrDefault(label, 0) + 1);
                validRows++;
            }
        }

        if (validRows == 0) return 0.0; // Si no hay etiquetas válidas, entropía es 0

        double entropy = 0.0;
        for (int c : count.values()) {
            double p = c / (double) validRows;
            entropy -= p * Math.log(p) / Math.log(2);
        }
        return entropy;
    }

    // CAMBIO: Tipo de datos de 'data' a Map<String, Double>
    private double[] gainRatio(List<Map<String, Double>> data, String attr, String target) {
        List<Double> values = new ArrayList<>();
        // Filtrar valores NaN para el cálculo de umbrales y la ganancia
        for (Map<String, Double> row : data) {
            Double val = row.get(attr);
            if (val != null && !val.isNaN()) {
                values.add(val);
            }
        }

        if (values.isEmpty()) { // Si todos los valores para este atributo son NaN o nulos
            return new double[]{-1.0, 0.0}; // No se puede calcular ganancia, devolver un valor inválido
        }

        Collections.sort(values);
        double bestThreshold = 0.0;
        double bestGainRatio = -1;

        // Calcular umbrales solo con valores únicos para evitar divisiones redundantes
        Set<Double> uniqueValues = new HashSet<>(values);
        List<Double> sortedUniqueValues = new ArrayList<>(uniqueValues);
        Collections.sort(sortedUniqueValues);

        for (int i = 0; i < sortedUniqueValues.size() - 1; i++) {
            double threshold = (sortedUniqueValues.get(i) + sortedUniqueValues.get(i + 1)) / 2;

            List<Map<String, Double>> left = new ArrayList<>();
            List<Map<String, Double>> right = new ArrayList<>();

            for (Map<String, Double> row : data) {
                Double val = row.get(attr);
                if (val != null && !val.isNaN()) { // Solo considerar filas con valor válido para la división
                    if (val <= threshold) left.add(row);
                    else right.add(row);
                }
            }

            // Evitar división por cero si un subconjunto queda vacío tras filtrar NaN
            if (left.isEmpty() || right.isEmpty()) {
                continue; // Este umbral no es válido para una división
            }

            double infoGain = entropy(data, target)
                    - (left.size() / (double) data.size()) * entropy(left, target)
                    - (right.size() / (double) data.size()) * entropy(right, target);

            double splitInfo = 0;
            if (!left.isEmpty()) {
                double p = left.size() / (double) data.size();
                splitInfo -= p * Math.log(p) / Math.log(2);
            }
            if (!right.isEmpty()) {
                double p = right.size() / (double) data.size();
                splitInfo -= p * Math.log(p) / Math.log(2);
            }

            double ratio = (splitInfo == 0) ? 0 : infoGain / splitInfo; // Si splitInfo es 0, ratio es 0

            if (ratio > bestGainRatio) {
                bestGainRatio = ratio;
                bestThreshold = threshold;
            }
        }

        return new double[]{bestGainRatio, bestThreshold};
    }

    public void printTree() {
        printTreeRec(root, 0);
    }

    private void printTreeRec(TreeNode node, int nivel) {
        if (node == null) return;

        String indent = "   ".repeat(nivel);

        if (node.isLeaf()) {
            System.out.println(indent + "→ Clase: " + node.label);
        } else {
            System.out.println(indent + "[ATRIBUTO: " + node.attribute + " ≤ " + node.threshold + "]");
            System.out.println(indent + "Si es verdadero:");
            printTreeRec(node.left, nivel + 1);
            System.out.println(indent + "Si es falso:");
            printTreeRec(node.right, nivel + 1);
        }
    }
    
    public TreeNode getRoot() {
        return root;
    }
}