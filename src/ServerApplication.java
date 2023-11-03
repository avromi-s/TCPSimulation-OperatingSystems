// Avromi Schneierson - 11/3/2023
package com.example.assignment4gui;

import javafx.application.Application;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * The GUI application for the Server. This class is responsible for setting up the Server GUI and managing the MessageSender
 * task based on user input to the GUI.
 * */
public class ServerApplication extends Application {
    private final int WINDOW_WIDTH = 330;
    private final int WINDOW_HEIGHT = 275;
    private final int STAGE_PADDING = 10;
    private final int PORT_NUM = 30121;
    private Label fileSelectedLabel;
    private Label appMessageLabel;
    private Label fileSenderMessageLabel;
    private ProgressBar progressBar;
    private File fileToSend;
    private String fileContentToSend;
    private CheckBox allowConnectionsCheckbox;
    private Button selectFileButtonCheckbox;
    private MessageSender messageSenderTask;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // Build the GUI

        allowConnectionsCheckbox = new CheckBox("Send file once connection is established");
        selectFileButtonCheckbox = new Button("Select file...");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(215);
        progressBar.setPrefHeight(28);
        fileSelectedLabel = new Label();
        FlowPane root = new FlowPane(STAGE_PADDING, STAGE_PADDING);

        root.setPadding(new Insets(STAGE_PADDING));
        appMessageLabel = new Label();
        fileSenderMessageLabel = new Label();
        appMessageLabel.setWrapText(true);
        appMessageLabel.setAlignment(Pos.CENTER);
        appMessageLabel.setTextAlignment(TextAlignment.CENTER);
        appMessageLabel.setMaxWidth(WINDOW_WIDTH - STAGE_PADDING * 2);
        fileSenderMessageLabel.setWrapText(true);
        fileSenderMessageLabel.setAlignment(Pos.CENTER);
        fileSenderMessageLabel.setTextAlignment(TextAlignment.CENTER);
        fileSenderMessageLabel.setMaxWidth(WINDOW_WIDTH - STAGE_PADDING * 2);

        // GUI handlers:

        // When allow connections is selected - allow sending of a file once one is selected, or send a file if a file
        // was already selected.
        allowConnectionsCheckbox.setOnAction(actionEvent -> {
            if (allowConnectionsCheckbox.isSelected() && fileContentToSend != null) {
                startMessageSendTask(fileContentToSend);
            }
        });

        // When select file button is selected, launch a FileChooser for user. If a valid, non-empty file is selected, start
        // the task to send it if the allowConnectionsCheckbox is selected.
        selectFileButtonCheckbox.setOnAction(actionEvent -> {
            try {
                FileChooser fileChooser = GUI.createOpenFileChooser();
                File oldFileToSend = fileToSend;  // save in case FileChooser selection is cancelled, to retain last file selection
                fileToSend = fileChooser.showOpenDialog(stage);
                if (fileToSend == null || !fileToSend.isFile()) {
                    appMessageLabel.setText("Please select a valid file to send");
                    fileToSend = oldFileToSend;  // retain last selected file, if applicable
                    return;
                }
                fileContentToSend = getFileContents(fileToSend);
                if (fileContentToSend.equals("")) {
                    fileContentToSend = null;
                    appMessageLabel.setText("Please select a non-empty file to send");
                    return;
                }
            } catch (IOException e) {
                appMessageLabel.setText("Error selecting file");
                System.out.println("SERVER-IOException while attempting to read file selected by user at path: '" + fileToSend.getPath() +
                        "'\n" + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
                return;
            }
            fileSelectedLabel.setText(fileToSend.getName());
            if (allowConnectionsCheckbox.isSelected()) {
                startMessageSendTask(fileContentToSend);
            }
        });

        // When window is closed, cancel messageSenderTask if it is running
        stage.setOnCloseRequest(windowEvent -> {
            if (messageSenderTask != null && messageSenderTask.isRunning()) {
                messageSenderTask.cancel();
            }
        });

        HBox fileSelectionBox = new HBox(STAGE_PADDING * 2);
        fileSelectionBox.getChildren().addAll(fileSelectedLabel, selectFileButtonCheckbox);
        fileSelectionBox.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(STAGE_PADDING * 2);
        vBox.getChildren().addAll(fileSelectionBox, allowConnectionsCheckbox, appMessageLabel, progressBar, fileSenderMessageLabel);
        vBox.setAlignment(Pos.CENTER);
        root.setAlignment(Pos.CENTER);
        root.getChildren().add(vBox);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.setTitle("Server Application");
        stage.setMinHeight(WINDOW_HEIGHT + STAGE_PADDING * 6);
        stage.setMinWidth(WINDOW_WIDTH + STAGE_PADDING * 6);
        stage.setMaxWidth(WINDOW_WIDTH + STAGE_PADDING * 9);
        stage.setMaxHeight(WINDOW_HEIGHT + STAGE_PADDING * 9);
        stage.show();
    }


    /**
     * Launch the MessageSender Task if connections are allowed and the task isn't currently running (if it hasn't
     * started, or it has finished a previous run)
     */
    private void startMessageSendTask(String fileContentToSend) {
        // Start the file sender task if it is not already running.
        if (messageSenderTask == null || !messageSenderTask.isRunning()) {
            System.out.println("Creating and starting Thread to send file at '" + fileToSend.getAbsolutePath() + "'");
            appMessageLabel.setText("Sending file at '" + fileToSend.getAbsolutePath() + "'");
            messageSenderTask = new MessageSender(fileContentToSend, PORT_NUM);

            // Disable controls while task is running:
            allowConnectionsCheckbox.setDisable(true);
            selectFileButtonCheckbox.setDisable(true);
            EventHandler<WorkerStateEvent> reenableControls = event -> {
                allowConnectionsCheckbox.setSelected(false);
                allowConnectionsCheckbox.setDisable(false);
                selectFileButtonCheckbox.setDisable(false);
            };
            messageSenderTask.setOnSucceeded(reenableControls);
            messageSenderTask.setOnCancelled(reenableControls);
            messageSenderTask.setOnFailed(reenableControls);

            progressBar.progressProperty().bind(messageSenderTask.progressProperty());  // bind the task's progress property to the progress bar so that the GUI gets updated as the task completes
            fileSenderMessageLabel.textProperty().bind(messageSenderTask.messageProperty());
            new Thread(messageSenderTask, "SERVER-FileSenderThread").start();
        }
    }

    /**
     * Read the given file and return the contents as a String
     */
    private String getFileContents(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
