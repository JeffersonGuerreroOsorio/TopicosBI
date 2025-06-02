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

    private SQLLoader sqlLoader; // Para interactuar con la base de datos
    private List<String> allColumns; // Para almacenar todos los nombres de las columnas de la tabla seleccionada

    public DecisionTreeGUI() {
        super("Configuración de Árbol de Decisión C4.5");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Espacio entre componentes

        sqlLoader = new SQLLoader(); // Inicializa el SQLLoader

        // --- Panel Superior: Selección de Tabla ---
        JPanel tablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Selección de Tabla"));
        tablePanel.add(new JLabel("Nombre de la Tabla:"));
        tableNameField = new JTextField(20);
        tableNameField.setText("Diagnostico_Leucemia"); // Valor por defecto
        tablePanel.add(tableNameField);
        loadColumnsButton = new JButton("Cargar Columnas");
        tablePanel.add(loadColumnsButton);
        add(tablePanel, BorderLayout.NORTH);

        // --- Panel Central: Selección de Atributos y Columna Objetivo ---
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10)); // 1 fila, 2 columnas
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Margen interior

        // Sub-panel para Atributos
        JPanel attributesPanel = new JPanel(new BorderLayout());
        attributesPanel.setBorder(BorderFactory.createTitledBorder("Atributos para el Árbol (Selección Múltiple)"));
        attributeList = new JList<>();
        attributeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane attributeScrollPane = new JScrollPane(attributeList);
        attributesPanel.add(attributeScrollPane, BorderLayout.CENTER);
        centerPanel.add(attributesPanel);

        // Sub-panel para Columna Objetivo
        JPanel targetPanel = new JPanel(new BorderLayout());
        targetPanel.setBorder(BorderFactory.createTitledBorder("Columna Objetivo (Clase)"));
        targetColumnComboBox = new JComboBox<>();
        targetPanel.add(targetColumnComboBox, BorderLayout.NORTH); // Usar NORTH para que no ocupe todo el espacio
        centerPanel.add(targetPanel);

        add(centerPanel, BorderLayout.CENTER);

        // --- Panel Inferior: Botón de Entrenar ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        trainButton = new JButton("Entrenar y Visualizar Árbol");
        trainButton.setEnabled(false); // Deshabilitado inicialmente
        controlPanel.add(trainButton);
        add(controlPanel, BorderLayout.SOUTH);

        // --- Configuración de Eventos (Listeners) ---
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

        // Empaquetar y hacer visible la ventana
        pack();
        setLocationRelativeTo(null); // Centrar en la pantalla
        setVisible(true);
    }

    private void loadColumns() {
        String tableName = tableNameField.getText().trim();
        if (tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, introduce el nombre de la tabla.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Obtener todos los nombres de las columnas
        allColumns = sqlLoader.getAllColumnNames(tableName); // Necesitaremos este nuevo método en SQLLoader

        if (allColumns == null || allColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las columnas. Asegúrate de que la tabla existe y la conexión es correcta.", "Error", JOptionPane.ERROR_MESSAGE);
            // Limpiar listas si no se cargaron columnas
            attributeList.setListData(new String[0]);
            targetColumnComboBox.removeAllItems();
            trainButton.setEnabled(false);
            return;
        }

        // Poblar la JList de atributos y el JComboBox de columna objetivo
        List<String> selectableAttributes = new ArrayList<>(allColumns);
        // Excluir la columna 'Id' si existe
        selectableAttributes.removeIf(col -> col.equalsIgnoreCase("Id"));

        attributeList.setListData(selectableAttributes.toArray(new String[0]));

        // Limpiar y poblar el ComboBox de la columna objetivo
        targetColumnComboBox.removeAllItems();
        for (String col : selectableAttributes) {
            targetColumnComboBox.addItem(col);
        }
        // Seleccionar por defecto la clase esperada (si existe)
        if (selectableAttributes.contains("diagnostico_leucemia")) {
            targetColumnComboBox.setSelectedItem("diagnostico_leucemia");
        } else if (!selectableAttributes.isEmpty()) {
            targetColumnComboBox.setSelectedIndex(0); // Seleccionar la primera si no se encuentra la de leucemia
        }

        trainButton.setEnabled(true); // Habilitar el botón de entrenar una vez que se cargan las columnas
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

        // Quitar la columna objetivo de la lista de atributos seleccionados
        // Esto es importante para el método train del C45DecisionTree
        List<String> attributesForTraining = new ArrayList<>(selectedAttributes);
        attributesForTraining.remove(selectedTargetColumn); // Asegurarse de que no esté aquí

        // Ahora, cargar los datos con las columnas seleccionadas
        // (Modificaremos SQLLoader para que pueda cargar solo columnas específicas si es necesario,
        //  o podemos simplemente cargar todas y luego filtrar en memoria para el entrenamiento,
        //  por ahora cargaremos todas como antes y C45 lo usará, pero el getAtributos ya las filtra)
        List<Map<String, Double>> datos = sqlLoader.cargarDatos(tableName);

        if (datos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron datos en la tabla '" + tableName + "'.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Filtrar los datos para que solo contengan los atributos seleccionados y la columna objetivo
        // Esto es crucial para que el C45 reciba solo lo que el usuario quiere
        List<Map<String, Double>> filteredDatos = new ArrayList<>();
        Set<String> desiredColumns = new HashSet<>(attributesForTraining);
        desiredColumns.add(selectedTargetColumn); // Asegurarse de que la columna objetivo también esté

        for (Map<String, Double> row : datos) {
            Map<String, Double> filteredRow = new HashMap<>();
            for (String col : row.keySet()) {
                if (desiredColumns.contains(col)) {
                    filteredRow.put(col, row.get(col));
                }
            }
            // Asegurarse de que las columnas numéricas como 'edad' se manejen correctamente si están en el Set
            filteredDatos.add(filteredRow);
        }

        // Entrenar el árbol
        C45DecisionTree arbol = new C45DecisionTree();
        arbol.train(filteredDatos, attributesForTraining, selectedTargetColumn);

        // Mostrar el árbol en la consola
        System.out.println("\n--- Árbol de Decisión Generado ---");
        arbol.printTree();

        // Mostrar el árbol en la ventana gráfica
        TreeVisualizer.mostrar(arbol.getRoot());
    }

    public static void main(String[] args) {
        // Ejecutar la interfaz en el Event Dispatch Thread de Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DecisionTreeGUI();
            }
        });
    }
}
