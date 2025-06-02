import java.sql.*;
import java.util.*;

public class SQLLoader {
    private final String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName=BD2_Medica;encrypt=true;trustServerCertificate=true";
    private final String user = "usuario_c45";
    private final String password = "1234";

    // --- NUEVO: Mapa estático para almacenar el mapeo inverso de la columna objetivo ---
    private static Map<Double, String> classLabelReverseMap = new HashMap<>();

    // --- NUEVO: Método para obtener el mapeo inverso (para uso en el visualizador) ---
    public static String getOriginalClassLabel(Double mappedValue) {
        return classLabelReverseMap.getOrDefault(mappedValue, "Desconocido");
    }
    // ----------------------------------------------------------------------------------

    // --- Mapeos de valores categóricos a numéricos para la tabla Diagnostico_Leucemia ---

    // Este método se usa para columnas binarias como fatiga_persistente, perdida_peso_inusual, etc.
    private double mapSiNoToDouble(String value) {
        if (value == null) return Double.NaN;
        value = value.trim().toLowerCase();
        switch (value) {
            case "sí": case "si": return 1.0; // Sí: 1.0
            case "no":           return 0.0; // No: 0.0
            default:
                System.err.println("Advertencia: Valor Sí/No desconocido: '" + value + "'. Devolviendo NaN.");
                return Double.NaN;
        }
    }

    // Este método es específico para la columna objetivo 'diagnostico_leucemia'
    private double mapDiagnosticoLeucemiaToDouble(String value) {
        if (value == null) return Double.NaN;
        value = value.trim().toLowerCase();
        double mappedValue;
        switch (value) {
            case "positivo": mappedValue = 1.0; break; // Positivo: 1.0
            case "negativo": mappedValue = 0.0; break; // Negativo: 0.0
            default:
                System.err.println("Advertencia: Valor de Diagnóstico Leucemia desconocido: '" + value + "'. Devolviendo NaN.");
                mappedValue = Double.NaN;
        }
        // --- Importante: Almacenar el mapeo inverso para la clase objetivo ---
        if (!Double.isNaN(mappedValue)) {
            classLabelReverseMap.put(mappedValue, value); // Guarda el mapeo de número a texto original
        }
        return mappedValue;
    }


    public List<Map<String, Double>> cargarDatos(String tabla) {
        List<Map<String, Double>> datos = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tabla)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int numCols = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Double> fila = new HashMap<>();
                for (int i = 1; i <= numCols; i++) {
                    String col = metaData.getColumnName(i);
                    String val = rs.getString(i);

                    if (col.equalsIgnoreCase("Id")) { // Ignorar la columna Id
                        continue;
                    }

                    Double mappedValue = null;

                    if (val == null || val.trim().isEmpty()) {
                        mappedValue = Double.NaN;
                    } else {
                        try {
                            // Intentar parsear como Double si es numérico (ej. 'edad', recuentos)
                            mappedValue = Double.parseDouble(val);
                        } catch (NumberFormatException e) {
                            // Si no es numérico, usa los mapeos personalizados
                            switch (col.toLowerCase()) {
                                case "fatiga_persistente":
                                case "perdida_peso_inusual":
                                case "fiebre_recurrente":
                                case "historial_exposicion_radiacion":
                                case "historial_quimioterapia_previa":
                                    mappedValue = mapSiNoToDouble(val);
                                    break;
                                case "diagnostico_leucemia": // La columna clase también necesita ser numérica
                                    mappedValue = mapDiagnosticoLeucemiaToDouble(val);
                                    break;
                                // Si tuvieras otras columnas categóricas que no fueran Sí/No y no fueran la clase
                                // Por ejemplo, si regresaras a una columna como "resultado_biopsia_preliminar"
                                // Tendrías que añadir su case aquí y su método map...ToDouble
                                /*
                                case "resultado_biopsia_preliminar":
                                    mappedValue = mapResultadoBiopsiaPreliminarToDouble(val);
                                    break;
                                */
                                default:
                                    // Esto debería capturar cualquier columna que no sea numérica y no esté en el switch
                                    System.err.println("Advertencia: Columna '" + col + "' con valor no numérico no mapeado: '" + val + "'. Devolviendo NaN.");
                                    mappedValue = Double.NaN;
                                    break;
                            }
                        }
                    }
                    fila.put(col, mappedValue);
                }
                datos.add(fila);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return datos;
    }

    // Este método ya no se usa directamente desde Main, pero se mantiene por si acaso
    public List<String> obtenerAtributos(String tabla, String columnaClase) {
        List<String> atributos = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT TOP 1 * FROM " + tabla)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int numCols = metaData.getColumnCount();

            for (int i = 1; i <= numCols; i++) {
                String col = metaData.getColumnName(i);
                if (!col.equalsIgnoreCase("Id") && !col.equalsIgnoreCase(columnaClase)) {
                    atributos.add(col);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return atributos;
    }

    // --- MÉTODO AGREGADO PARA LA GUI ---
    public List<String> getAllColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT TOP 1 * FROM " + tableName)) { // TOP 1 para obtener solo la estructura

            ResultSetMetaData metaData = rs.getMetaData();
            int numCols = metaData.getColumnCount();

            for (int i = 1; i <= numCols; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar nombres de columnas de la tabla '" + tableName + "': " + e.getMessage());
            return null; // Devolver null para indicar un error
        }
        return columnNames;
    }
}