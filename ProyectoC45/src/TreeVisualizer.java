import javax.swing.*;
import java.awt.*;

public class TreeVisualizer extends JPanel {
    private final TreeNode root;
    private final int nodeWidth = 240; 
    private final int nodeHeight = 60; 
    private final int vSpacing = 70;   
    private final int hSpacing = 30;   

    public TreeVisualizer(TreeNode root) {
        this.root = root;
        
        setPreferredSize(new Dimension(1400, 900)); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int initialOffset = getWidth() / 4; 

        drawTree(g, root, getWidth() / 2, 40, initialOffset);
    }

    private void drawTree(Graphics g, TreeNode node, int x, int y, int offset) {
        if (node == null) return;

        String displayLabel;
        if (node.isLeaf()) {
            
            try {
                Double classValue = Double.parseDouble(node.label);
                String originalLabel = SQLLoader.getOriginalClassLabel(classValue); 
                
                if (originalLabel.equalsIgnoreCase("positivo")) {
                    displayLabel = "Posible leucemia, realizar estudios";
                } else if (originalLabel.equalsIgnoreCase("negativo")) {
                    displayLabel = "Posiblemente no sea leucemia";
                } else {
                    displayLabel = "Clase: " + originalLabel; 
                }
            } catch (NumberFormatException e) {
                
                displayLabel = "Error Clase: " + node.label; 
            }
        } else {
            
            displayLabel = node.attribute + " \u2264 " + String.format("%.2f", node.threshold); 
        }

        
        g.setColor(Color.BLACK);
        g.drawRect(x - nodeWidth / 2, y, nodeWidth, nodeHeight);
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(displayLabel);
        
        int textY = y + (nodeHeight / 2) + (fm.getAscent() / 2) - fm.getDescent(); 

        g.drawString(displayLabel, x - textWidth / 2, textY);

        if (!node.isLeaf()) {
            int childY = y + nodeHeight + vSpacing; 
        
            if (node.left != null) {
                int leftX = x - offset;
                g.drawLine(x, y + nodeHeight, leftX, childY); 
                drawTree(g, node.left, leftX, childY, offset / 2); 

                
                g.drawString("Sí", (x + leftX) / 2 - 15, (y + childY) / 2); 
            }

            
            if (node.right != null) {
                int rightX = x + offset;
                g.drawLine(x, y + nodeHeight, rightX, childY); 
                drawTree(g, node.right, rightX, childY, offset / 2); 

                
                g.drawString("No", (x + rightX) / 2 + 15, (y + childY) / 2); 
            }
        }
    }

    public static void mostrar(TreeNode root) {
        JFrame frame = new JFrame("Árbol de Decisión");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
        
        JScrollPane scrollPane = new JScrollPane(new TreeVisualizer(root));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        frame.add(scrollPane);
        frame.pack();
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
    }
}