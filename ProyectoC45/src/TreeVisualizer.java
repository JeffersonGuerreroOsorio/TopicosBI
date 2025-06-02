import javax.swing.*;
import java.awt.*;

public class TreeVisualizer extends JPanel {
    private final TreeNode root;
    private final int nodeWidth = 240; // Aumentado para texto más largo (antes 180)
    private final int nodeHeight = 60; // Aumentado para texto más largo (antes 50)
    private final int vSpacing = 70;   // Espaciado vertical entre niveles
    private final int hSpacing = 30;   // Espaciado horizontal base

    public TreeVisualizer(TreeNode root) {
        this.root = root;
        // Ajusta el tamaño preferido del panel para acomodar árboles más grandes y nodos con texto más largo
        setPreferredSize(new Dimension(1400, 900)); // Aumentado el tamaño preferido
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Calcula el ancho inicial para el primer nivel. Podría ser dinámico basado en la profundidad del árbol.
        // Para empezar, un valor fijo grande o calculado.
        int initialOffset = getWidth() / 4; // Ajusta este valor si los árboles se solapan mucho

        // Dibuja el árbol comenzando desde el centro superior del panel
        drawTree(g, root, getWidth() / 2, 40, initialOffset);
    }

    private void drawTree(Graphics g, TreeNode node, int x, int y, int offset) {
        if (node == null) return;

        String displayLabel;
        if (node.isLeaf()) {
            // --- CAMBIO CLAVE AQUÍ: Convertir el valor numérico de vuelta a texto significativo ---
            // Asumiendo que node.label es un String como "0.0" o "1.0"
            try {
                Double classValue = Double.parseDouble(node.label);
                String originalLabel = SQLLoader.getOriginalClassLabel(classValue); // Obtener el texto original (ej. "positivo", "negativo")
                
                // Asigna tus mensajes personalizados basados en el original
                if (originalLabel.equalsIgnoreCase("positivo")) {
                    displayLabel = "Posible leucemia, realizar estudios";
                } else if (originalLabel.equalsIgnoreCase("negativo")) {
                    displayLabel = "Posiblemente no sea leucemia";
                } else {
                    displayLabel = "Clase: " + originalLabel; // En caso de que haya un valor inesperado
                }
            } catch (NumberFormatException e) {
                // Si node.label no es un número parseable (lo cual no debería pasar en nodos hoja de clase)
                displayLabel = "Error Clase: " + node.label; 
            }
        } else {
            // Nodos de decisión (internos), muestran el atributo y el umbral
            displayLabel = node.attribute + " \u2264 " + String.format("%.2f", node.threshold); // \u2264 es el símbolo de menor o igual
        }

        // Dibuja el rectángulo del nodo
        g.setColor(Color.BLACK);
        g.drawRect(x - nodeWidth / 2, y, nodeWidth, nodeHeight);
        
        // Ajusta el centrado del texto dentro del nodo
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(displayLabel);
        // Calcular la posición Y del texto para que esté centrado verticalmente
        int textY = y + (nodeHeight / 2) + (fm.getAscent() / 2) - fm.getDescent(); // Adjusted calculation

        g.drawString(displayLabel, x - textWidth / 2, textY);


        // Dibuja las líneas a los hijos y las etiquetas "Sí"/"No" si no es una hoja
        if (!node.isLeaf()) {
            int childY = y + nodeHeight + vSpacing; // La posición Y de los hijos

            // Rama izquierda (Sí: atributo <= umbral)
            if (node.left != null) {
                int leftX = x - offset;
                g.drawLine(x, y + nodeHeight, leftX, childY); // Línea desde el padre al hijo izquierdo
                drawTree(g, node.left, leftX, childY, offset / 2); // Dibujar el subárbol izquierdo

                // Etiqueta "Sí" en la línea
                g.drawString("Sí", (x + leftX) / 2 - 15, (y + childY) / 2); // Ajusta -15 para mover la etiqueta
            }

            // Rama derecha (No: atributo > umbral)
            if (node.right != null) {
                int rightX = x + offset;
                g.drawLine(x, y + nodeHeight, rightX, childY); // Línea desde el padre al hijo derecho
                drawTree(g, node.right, rightX, childY, offset / 2); // Dibujar el subárbol derecho

                // Etiqueta "No" en la línea
                g.drawString("No", (x + rightX) / 2 + 15, (y + childY) / 2); // Ajusta +15 para mover la etiqueta
            }
        }
    }

    public static void mostrar(TreeNode root) {
        JFrame frame = new JFrame("Árbol de Decisión");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Cambiado a DISPOSE_ON_CLOSE
        
        // Envuelve el TreeVisualizer en un JScrollPane para que el árbol sea desplazable si es muy grande
        JScrollPane scrollPane = new JScrollPane(new TreeVisualizer(root));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        frame.add(scrollPane);
        frame.pack();
        frame.setLocationRelativeTo(null); // Centrar la ventana
        frame.setVisible(true);
    }
}