import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.golbarg.pdfviewer.PDFViewer;

import java.io.IOException;
import java.net.URL;

public class ProcessListenerDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //create a progressBar
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(500);
        progressBar.setProgress(0);

        //create a pdfDisplayer
        PDFViewer pdfViewer = new PDFViewer();
        //set the process listener of it

        pdfViewer.setOnLoaderTaskPresent(task -> {
            task.setOnRunning((e) -> progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));
            task.setOnSucceeded((e) -> progressBar.setProgress(0));
            task.setOnFailed((e) -> progressBar.setProgress(0));
            //OR using task.progressProperty()
        });

        //create a btn for loading the pdf
        Button loaderBtn = new Button("Load");
        loaderBtn.setOnAction(e ->
                //start a new thread for load the pdf document
                new Thread(() -> {
                    try {
                        pdfViewer.loadPDF(new URL("https://www.tutorialspoint.com/javafx/javafx_tutorial.pdf"));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start()
        );

        final Parent pdfNode = pdfViewer.toNode();
        VBox.setVgrow(pdfNode, Priority.ALWAYS);
        primaryStage.setScene(new Scene(new VBox(new StackPane(loaderBtn), pdfNode, new StackPane(progressBar))));
        primaryStage.show();
    }
}
