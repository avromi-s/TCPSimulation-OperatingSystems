// Avromi Schneierson - 11/3/2023
package src;

import javafx.application.Application;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;

/**
 * The GUI application for the Client. This class is responsible for setting up the Client GUI and managing the MessageReceiver
 * task based on user input to the GUI.
 */
public class ClientApplication extends Application {
    private final int WINDOW_WIDTH = 330;
    private final int WINDOW_HEIGHT = 275;
    private final int STAGE_PADDING = 10;
    private final int PORT_NUM = 30121;
    private Stage primaryStage;
    private Label fileReceiverMessageLabel;
    private Label appMessageLabel;
    private TextField ipField;
    private Button connectButton;
    private ProgressBar progressBar;
    private MessageReceiver messageReceiverTask;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // Build the GUI

        this.primaryStage = stage;
        Label ipLabel = new Label("IP Address:");
        ipField = new TextField("127.0.0.1");
        GUI.linkLabelToTextField(ipLabel, ipField);
        connectButton = new Button("Receive File");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(215);
        progressBar.setPrefHeight(28);
        FlowPane root = new FlowPane(STAGE_PADDING, STAGE_PADDING);
        root.setPadding(new Insets(STAGE_PADDING));

        appMessageLabel = new Label();
        fileReceiverMessageLabel = new Label();
        appMessageLabel.setWrapText(true);
        appMessageLabel.setAlignment(Pos.CENTER);
        appMessageLabel.setTextAlignment(TextAlignment.CENTER);
        appMessageLabel.setMaxWidth(WINDOW_WIDTH - STAGE_PADDING * 2);
        fileReceiverMessageLabel.setWrapText(true);
        fileReceiverMessageLabel.setAlignment(Pos.CENTER);
        fileReceiverMessageLabel.setTextAlignment(TextAlignment.CENTER);
        fileReceiverMessageLabel.setMaxWidth(WINDOW_WIDTH - STAGE_PADDING * 2);

        // GUI handlers:

        // When connect button is clicked, connect to the specified IP and receive the file
        connectButton.setOnAction(event -> {
            String ipPattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
            String selectedIp = ipField.getText();
            if (!selectedIp.matches(ipPattern)) {
                appMessageLabel.setText("Invalid IP: please provide a valid IPv4 address");
                return;
            }
            if (messageReceiverTask != null && messageReceiverTask.isRunning()) {
                appMessageLabel.setText("Please wait for the task to complete before receiving another file");
                return;
            }
            startMessageReceiveTask(selectedIp);
        });

        // When window is closed, cancel messageReceiverTask if it is running
        stage.setOnCloseRequest(windowEvent -> {
            if (messageReceiverTask != null && messageReceiverTask.isRunning()) {
                messageReceiverTask.cancel();
            }
        });

        HBox ipBox = new HBox(STAGE_PADDING * 2);
        ipBox.getChildren().addAll(ipLabel, ipField);
        ipBox.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(STAGE_PADDING * 2);
        vBox.getChildren().addAll(ipBox, connectButton, appMessageLabel, progressBar, fileReceiverMessageLabel);
        vBox.setAlignment(Pos.CENTER);
        root.setAlignment(Pos.CENTER);
        root.getChildren().add(vBox);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.setTitle("Client Application");
        stage.setMinHeight(WINDOW_HEIGHT + STAGE_PADDING * 6);
        stage.setMinWidth(WINDOW_WIDTH + STAGE_PADDING * 6);
        stage.setMaxWidth(WINDOW_WIDTH + STAGE_PADDING * 9);
        stage.setMaxHeight(WINDOW_HEIGHT + STAGE_PADDING * 9);
        stage.show();
    }

    /**
     * Launch the MessageReceiver Task if it isn't currently running (if it hasn't started, or it has finished a previous run)
     */
    private void startMessageReceiveTask(String selectedIp) {
        appMessageLabel.setText("Connecting to server...");
        messageReceiverTask = new MessageReceiver(selectedIp, PORT_NUM);
        messageReceiverTask.setOnRunning(event -> {
            appMessageLabel.setText("");
            // Disable controls while task is running:
            ipField.setDisable(true);
            connectButton.setDisable(true);
        });

        EventHandler<WorkerStateEvent> reenableControls = event -> {
            ipField.setDisable(false);
            connectButton.setDisable(false);
        };

        messageReceiverTask.setOnSucceeded(event -> {
            getPathAndWriteFile(messageReceiverTask.getValue(), appMessageLabel);
            reenableControls.handle(event);
        });
        messageReceiverTask.setOnCancelled(reenableControls);
        messageReceiverTask.setOnFailed(reenableControls);

        progressBar.progressProperty().bind(messageReceiverTask.progressProperty());  // bind the task's progress property to the progress bar so that the GUI gets updated as the task completes
        fileReceiverMessageLabel.textProperty().bind(messageReceiverTask.messageProperty());
        new Thread(messageReceiverTask, "CLIENT-FileReceiverThread").start();
    }

    /**
     * Get the path to write the file to from the user and write the given fileContent to a new file there
     */
    private void getPathAndWriteFile(String fileContent, Label resultLabel) {
        if (fileContent == null) {
            resultLabel.setText("No file received");
            return;
        }
        resultLabel.setText("");
        File outputFile = getFileToSaveAs(resultLabel);
        if (outputFile == null) return;
        System.out.println("Writing file to disk...");
        resultLabel.setText("Selected file '" + outputFile.getPath() + "'");
        try (FileWriter fileWriter = new FileWriter(outputFile, false)) {
            fileWriter.write(fileContent);
            resultLabel.setText("Saved file '" + outputFile.getName() + "' to '" + outputFile.getParent() + "'");
            System.out.println("Completed writing file to disk.");
        } catch (Exception e) {
            resultLabel.setText("Unable to write file to disk at path '" + outputFile.getAbsolutePath() +
                    "'.\nException stack trace: " + e.getMessage());
            System.out.println("Unable to write file to disk at path '" + outputFile.getAbsolutePath() +
                    "'.\nException stack trace: " + e.getMessage());
        }
    }

    /**
     * Get a file to save as from the user with a FileChooser dialog box
     */
    private File getFileToSaveAs(Label resultLabel) {
        File fileToSaveAs;
        FileChooser fileChooser = GUI.createSaveFileChooser();
        fileToSaveAs = fileChooser.showSaveDialog(ClientApplication.this.primaryStage);
        if (fileToSaveAs == null) {
            resultLabel.setText("No path selected - file not saved");
            return null;
        }
        return fileToSaveAs;
    }
}