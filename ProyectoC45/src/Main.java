import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Ejecutar la interfaz gráfica en el Event Dispatch Thread de Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DecisionTreeGUI(); // Lanza la interfaz gráfica
            }
        });
    }
}