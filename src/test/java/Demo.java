import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import net.golbarg.pdfviewer.PDFJSVersion;
import net.golbarg.pdfviewer.PDFViewer;

import java.io.IOException;
import java.net.URL;

public class Demo extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        PDFViewer viewer = new PDFViewer(PDFJSVersion.VERSION_2_2_228);

        Stage alertStage = new Stage();
        alertStage.setScene(new Scene(new StackPane(new Label("Processing"))));
        alertStage.setAlwaysOnTop(true);
        alertStage.show();

        viewer.setOnLoaderTaskPresent(task -> {
            task.setOnRunning(e -> alertStage.show());
            task.setOnSucceeded(e -> alertStage.close());
            task.setOnFailed(e -> alertStage.close());
        });

        Button btn = new Button("Load");
        btn.setOnAction(e -> loadPDF(viewer));

        Text javaInfo = new Text(
                String.format("Java version: %s, JavaFX version: %s",
                        System.getProperty("java.version"),
                        System.getProperty("javafx.version"))
        );

        primaryStage.setScene(new Scene(new VBox(btn, viewer.toNode(), javaInfo)));
        primaryStage.show();

        viewer.setSecondaryToolbarToggleVisibility(true);
        viewer.toNode().getStylesheets().add("style.css");


        // viewer.executeScript("document.getElementById('secondaryToolbarToggle').style.backgroundColor = 'blue';");
        // JSLogListener.setOutputStream(System.err);
    }

    private void loadPDF(PDFViewer pdfViewer) {
        try {
            pdfViewer.loadPDF(new URL("https://www.tutorialspoint.com/jdbc/jdbc_tutorial.pdf"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
