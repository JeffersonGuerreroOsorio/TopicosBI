import java.util.*;

public class C45DecisionTree {
    private TreeNode root;

    public void train(List<Map<String, Double>> data, List<String> attributes, String target) {
        root = buildTree(data, attributes, target);
    }

    
    public String predict(Map<String, Double> instance) {
        TreeNode node = root;
        while (!node.isLeaf()) {
            Double val = instance.get(node.attribute); 

            
            if (val == null || val.isNaN()) {            
                System.out.println("Advertencia: Valor NaN encontrado para '" + node.attribute + "' durante la predicción. Recorriendo rama izquierda por defecto.");
                node = node.left; 
                if (node == null) { 
                    return "NoClassFound"; 
                }
            } else {
                node = (val <= node.threshold) ? node.left : node.right;
            }
        }
        return node.label;
    }

    private TreeNode buildTree(List<Map<String, Double>> data, List<String> attributes, String target) {
        if (data.isEmpty()) return null;

        if (allSameClass(data, target)) {
            TreeNode leaf = new TreeNode();
            leaf.label = data.get(0).get(target).toString(); 
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
            
            boolean allNaNForAttr = true;
            for(Map<String, Double> row : data) {
                if (row.get(attr) != null && !row.get(attr).isNaN()) {
                    allNaNForAttr = false;
                    break;
                }
            }
            if (allNaNForAttr) {
                continue; 
            }


            double[] result = gainRatio(data, attr, target);
            if (result[0] > bestGainRatio) {
                bestGainRatio = result[0];
                bestThreshold = result[1];
                bestAttr = attr;
            }
        }

        if (bestAttr == null || bestGainRatio <= 0) {
            TreeNode leaf = new TreeNode();
            leaf.label = majorityClass(data, target);
            return leaf;
        }


        TreeNode node = new TreeNode();
        node.attribute = bestAttr;
        node.threshold = bestThreshold;

        List<Map<String, Double>> left = new ArrayList<>();
        List<Map<String, Double>> right = new ArrayList<>();

        for (Map<String, Double> row : data) {
            Double val = row.get(bestAttr); 
            if (val != null && !val.isNaN()) { 
                if (val <= bestThreshold) left.add(row);
                else right.add(row);
            }
        }

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

    
    private boolean allSameClass(List<Map<String, Double>> data, String target) {
        if (data.isEmpty()) return true; 
        Double label = data.get(0).get(target);
        for (Map<String, Double> row : data) {
            if (!row.get(target).equals(label)) return false;
        }
        return true;
    }

    private String majorityClass(List<Map<String, Double>> data, String target) {
        if (data.isEmpty()) return "N/A"; 

        Map<Double, Integer> count = new HashMap<>(); 
        for (Map<String, Double> row : data) {
            Double label = row.get(target);
            if (label != null && !label.isNaN()) { 
                count.put(label, count.getOrDefault(label, 0) + 1);
            }
        }
        if (count.isEmpty()) return "N/A"; 

        Double majorityLabel = null;
        int maxCount = -1;
        for (Map.Entry<Double, Integer> entry : count.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                majorityLabel = entry.getKey();
            }
        }
        return majorityLabel != null ? majorityLabel.toString() : "N/A"; 
    }

    private double entropy(List<Map<String, Double>> data, String target) {
        if (data.isEmpty()) return 0.0; 

        Map<Double, Integer> count = new HashMap<>(); 
        int validRows = 0; 
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

    
    private double[] gainRatio(List<Map<String, Double>> data, String attr, String target) {
        List<Double> values = new ArrayList<>();
        
        for (Map<String, Double> row : data) {
            Double val = row.get(attr);
            if (val != null && !val.isNaN()) {
                values.add(val);
            }
        }

        if (values.isEmpty()) { 
            return new double[]{-1.0, 0.0}; 
        }

        Collections.sort(values);
        double bestThreshold = 0.0;
        double bestGainRatio = -1;

        
        Set<Double> uniqueValues = new HashSet<>(values);
        List<Double> sortedUniqueValues = new ArrayList<>(uniqueValues);
        Collections.sort(sortedUniqueValues);

        for (int i = 0; i < sortedUniqueValues.size() - 1; i++) {
            double threshold = (sortedUniqueValues.get(i) + sortedUniqueValues.get(i + 1)) / 2;

            List<Map<String, Double>> left = new ArrayList<>();
            List<Map<String, Double>> right = new ArrayList<>();

            for (Map<String, Double> row : data) {
                Double val = row.get(attr);
                if (val != null && !val.isNaN()) { 
                    if (val <= threshold) left.add(row);
                    else right.add(row);
                }
            }

            
            if (left.isEmpty() || right.isEmpty()) {
                continue; 
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

            double ratio = (splitInfo == 0) ? 0 : infoGain / splitInfo; 

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