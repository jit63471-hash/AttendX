import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AttendanceModel model = new AttendanceModel();
            AttendanceView view = new AttendanceView();
            new AttendanceController(model, view);
            view.setVisible(true);
        });
    }
}
