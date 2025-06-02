import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class DecisionTreeGUI extends JFrame {

    private JTextField tableNameField;
    private JButton loadColumnsButton;
    private JList<String> attributeList;
    private JComboBox<String> targetColumnComboBox;
    private JButton trainButton;

    private SQLLoader sqlLoader; 
    private List<String> allColumns; 

    public DecisionTreeGUI() {
        super("Configuración de Árbol de Decisión C4.5");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); 

        sqlLoader = new SQLLoader(); 

        
        JPanel tablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Selección de Tabla"));
        tablePanel.add(new JLabel("Nombre de la Tabla:"));
        tableNameField = new JTextField(20);
        tableNameField.setText("Diagnostico_Leucemia"); 
        tablePanel.add(tableNameField);
        loadColumnsButton = new JButton("Cargar Columnas");
        tablePanel.add(loadColumnsButton);
        add(tablePanel, BorderLayout.NORTH);

        
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10)); 
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); 

        
        JPanel attributesPanel = new JPanel(new BorderLayout());
        attributesPanel.setBorder(BorderFactory.createTitledBorder("Atributos para el Árbol (Selección Múltiple)"));
        attributeList = new JList<>();
        attributeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane attributeScrollPane = new JScrollPane(attributeList);
        attributesPanel.add(attributeScrollPane, BorderLayout.CENTER);
        centerPanel.add(attributesPanel);

        
        JPanel targetPanel = new JPanel(new BorderLayout());
        targetPanel.setBorder(BorderFactory.createTitledBorder("Columna Objetivo (Clase)"));
        targetColumnComboBox = new JComboBox<>();
        targetPanel.add(targetColumnComboBox, BorderLayout.NORTH); 
        centerPanel.add(targetPanel);

        add(centerPanel, BorderLayout.CENTER);

        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        trainButton = new JButton("Entrenar y Visualizar Árbol");
        trainButton.setEnabled(false); 
        controlPanel.add(trainButton);
        add(controlPanel, BorderLayout.SOUTH);

        
        loadColumnsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadColumns();
            }
        });

        trainButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                trainAndVisualizeTree();
            }
        });

        pack();
        setLocationRelativeTo(null); 
        setVisible(true);
    }

    private void loadColumns() {
        String tableName = tableNameField.getText().trim();
        if (tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, introduce el nombre de la tabla.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        
        allColumns = sqlLoader.getAllColumnNames(tableName); 

        if (allColumns == null || allColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las columnas. Asegúrate de que la tabla existe y la conexión es correcta.", "Error", JOptionPane.ERROR_MESSAGE);
            
            attributeList.setListData(new String[0]);
            targetColumnComboBox.removeAllItems();
            trainButton.setEnabled(false);
            return;
        }

        
        List<String> selectableAttributes = new ArrayList<>(allColumns);
        
        selectableAttributes.removeIf(col -> col.equalsIgnoreCase("Id"));

        attributeList.setListData(selectableAttributes.toArray(new String[0]));

        
        targetColumnComboBox.removeAllItems();
        for (String col : selectableAttributes) {
            targetColumnComboBox.addItem(col);
        }
        
        if (selectableAttributes.contains("diagnostico_leucemia")) {
            targetColumnComboBox.setSelectedItem("diagnostico_leucemia");
        } else if (!selectableAttributes.isEmpty()) {
            targetColumnComboBox.setSelectedIndex(0); 
        }

        trainButton.setEnabled(true); 
    }

    private void trainAndVisualizeTree() {
        List<String> selectedAttributes = attributeList.getSelectedValuesList();
        String selectedTargetColumn = (String) targetColumnComboBox.getSelectedItem();
        String tableName = tableNameField.getText().trim();

        if (selectedAttributes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona al menos un atributo.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedTargetColumn == null || selectedTargetColumn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona la columna objetivo.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedAttributes.contains(selectedTargetColumn)) {
            JOptionPane.showMessageDialog(this, "La columna objetivo no puede ser un atributo.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> attributesForTraining = new ArrayList<>(selectedAttributes);
        attributesForTraining.remove(selectedTargetColumn); 

        List<Map<String, Double>> datos = sqlLoader.cargarDatos(tableName);

        if (datos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron datos en la tabla '" + tableName + "'.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Map<String, Double>> filteredDatos = new ArrayList<>();
        Set<String> desiredColumns = new HashSet<>(attributesForTraining);
        desiredColumns.add(selectedTargetColumn); 

        for (Map<String, Double> row : datos) {
            Map<String, Double> filteredRow = new HashMap<>();
            for (String col : row.keySet()) {
                if (desiredColumns.contains(col)) {
                    filteredRow.put(col, row.get(col));
                }
            }
            
            filteredDatos.add(filteredRow);
        }

        
        C45DecisionTree arbol = new C45DecisionTree();
        arbol.train(filteredDatos, attributesForTraining, selectedTargetColumn);

        
        System.out.println("\n--- Árbol de Decisión Generado ---");
        arbol.printTree();

        
        TreeVisualizer.mostrar(arbol.getRoot());
    }

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DecisionTreeGUI();
            }
        });
    }
}
