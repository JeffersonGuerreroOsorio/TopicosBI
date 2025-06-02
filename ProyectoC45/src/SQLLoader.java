import java.sql.*;
import java.util.*;

public class SQLLoader {
    private final String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName=BD2_Medica;encrypt=true;trustServerCertificate=true";
    private final String user = "usuario_c45";
    private final String password = "1234";


    private static Map<Double, String> classLabelReverseMap = new HashMap<>();

    
    public static String getOriginalClassLabel(Double mappedValue) {
        return classLabelReverseMap.getOrDefault(mappedValue, "Desconocido");
    }
   
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

        if (!Double.isNaN(mappedValue)) {
            classLabelReverseMap.put(mappedValue, value); 
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

                    if (col.equalsIgnoreCase("Id")) { 
                        continue;
                    }

                    Double mappedValue = null;

                    if (val == null || val.trim().isEmpty()) {
                        mappedValue = Double.NaN;
                    } else {
                        try {
                           
                            mappedValue = Double.parseDouble(val);
                        } catch (NumberFormatException e) {
                        
                            switch (col.toLowerCase()) {
                                case "fatiga_persistente":
                                case "perdida_peso_inusual":
                                case "fiebre_recurrente":
                                case "historial_exposicion_radiacion":
                                case "historial_quimioterapia_previa":
                                    mappedValue = mapSiNoToDouble(val);
                                    break;
                                case "diagnostico_leucemia": 
                                    mappedValue = mapDiagnosticoLeucemiaToDouble(val);
                                    break;
                                default:
                                    
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


    public List<String> getAllColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT TOP 1 * FROM " + tableName)) { 

            ResultSetMetaData metaData = rs.getMetaData();
            int numCols = metaData.getColumnCount();

            for (int i = 1; i <= numCols; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar nombres de columnas de la tabla '" + tableName + "': " + e.getMessage());
            return null; 
        }
        return columnNames;
    }
}