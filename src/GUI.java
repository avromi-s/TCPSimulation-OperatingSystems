// Avromi Schneierson - 11/3/2023
package src;

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import java.io.File;
import java.util.List;

/**
 * Provides convenience methods for handling GUI operations and setup
 */
public class GUI {

    private static ExtensionFilter allFilter = new ExtensionFilter("All Files", "*.txt", "*.csv");
    private static ExtensionFilter txtFilter = new ExtensionFilter("Text File", "*.txt");
    private static ExtensionFilter csvFilter = new ExtensionFilter("CSV File", "*.csv");

    public static void linkLabelToTextField(Label label, TextField textField) {
        label.setLabelFor(textField);
        label.setOnMouseClicked(x -> {
            textField.requestFocus();
            textField.selectAll();
        });
    }

    private static FileChooser createFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getenv("USERPROFILE") + "/Downloads"));
        return fileChooser;
    }

    public static FileChooser createOpenFileChooser() {
        FileChooser fileChooser = createFileChooser();
        fileChooser.getExtensionFilters().addAll(
                allFilter,
                txtFilter,
                csvFilter
        );
        fileChooser.setTitle("Open file...");
        return fileChooser;
    }


    public static FileChooser createSaveFileChooser() {
        FileChooser fileChooser = createFileChooser();
        fileChooser.getExtensionFilters().addAll(
                csvFilter,
                txtFilter
        );
        fileChooser.setTitle("Save file as...");
        return fileChooser;
    }

    public static void enableControls(List<Control> controls) {
        for (Control control : controls) {
            control.setDisable(false);
        }
    }

    public static void disableControl(List<Control> controls) {
        for (Control control : controls) {
            control.setDisable(true);
        }
    }
}
