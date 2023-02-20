package net.golbarg.pdfviewer;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.Parent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class PDFViewer {
    private static final Logger logger = LoggerFactory.getLogger(PDFViewer.class);

    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t;
    });

    public final ObjectProperty<Consumer<Task<String>>> onLoaderTaskPresent = new SimpleObjectProperty<>();

    private boolean pdfJsLoaded;

    private final PDFJSVersion version;

    private WebView webView;
    private String loadScript;
    private String toExecuteWhenPDFJSLoaded = "";

    public PDFViewer(PDFJSVersion version) {
        this.version = version;
    }

    public PDFViewer() {
        this(PDFJSVersion.latestVersion());
    }

    public PDFViewer(PDFJSVersion version, File file) throws IOException {
        this(version);
        loadPDF(file);
    }

    public PDFViewer(File file) throws IOException {
        this(PDFJSVersion.latestVersion(), file);
    }

    public PDFViewer(PDFJSVersion version, URL url) throws IOException {
        this(version);
        loadPDF(url);
    }

    public PDFViewer(URL url) throws IOException {
        this(PDFJSVersion.latestVersion(), url);
    }

    public PDFViewer(PDFJSVersion version, InputStream inputStream) throws IOException {
        this(version);
        loadPDF(inputStream);
    }

    public PDFViewer(InputStream inputStream) throws IOException {
        this(PDFJSVersion.latestVersion(), inputStream);
    }

    /**
     * @deprecated Use {@link #loadPDF(InputStream)} instead
     */
    @Deprecated
    public void displayPdf(File file) throws IOException {
        loadPDF(file);
    }

    @Deprecated
    public void displayPdf(URL url) throws IOException {
        loadPDF(url);
    }

    @Deprecated
    public void displayPdf(InputStream inputStream) throws IOException {
        loadPDF(inputStream);
    }

    public void loadPDF(File file) throws IOException {
        loadPDF(new BufferedInputStream(new FileInputStream(file)));
    }

    public void loadPDF(URL url) throws IOException {
        loadPDF(new BufferedInputStream(url.openConnection().getInputStream()));
    }

    public void loadPDF(InputStream inputStream) {
        if(inputStream == null) {
            return;
        }

        Task<String> task = buildLoadingTask(inputStream);

        final Consumer<Task<String>> onLoaderTaskPresent = this.onLoaderTaskPresent.get();

        if(onLoaderTaskPresent != null) {
            Platform.runLater(() -> onLoaderTaskPresent.accept(task));
        }

        THREAD_POOL.submit(task);
    }

    private Task<String> buildLoadingTask(InputStream inputStream) {
        final Task<String> task = new Task<String> () {
            @Override
            protected String call() throws Exception {
                try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    long contentSize = inputStream.available();
                    long onePercent = contentSize / 100;

                    int allReadBytesCount = 0;

                    byte[] buf = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buf)) >= 0) {
                        allReadBytesCount += bytesRead;
                        outputStream.write(buf, 0, bytesRead);

                        if (onePercent > 0) {
                            double percent = allReadBytesCount / (double) onePercent;
                            updateProgress(percent, 100d);
                        }

                        if (this.isCancelled()) {
                            return null;
                        }
                    }

                    byte[] data = outputStream.toByteArray();
                    String base64 = Base64.getEncoder().encodeToString(data);

                    //JavaScript Function declaration
                    return "openFileFromBase64('" + base64 + "');";
                } finally {
                    inputStream.close();
                }
            }
        };

        task.valueProperty().addListener((observable, oldValue, js) -> {
            if(js != null) {
                try {
                    webView.getEngine().executeScript(js);
                } catch (Exception ex) {
                    if(!pdfJsLoaded) {
                        loadScript = js;
                    }
                }
            }
        });

        return task;
    }

    @SuppressWarnings("all")
    public void setSecondaryToolbarToggleVisibility(boolean value) {
        setVisibilityOf("secondaryToolbarToggle", value);

        String js;

        if(value) {
            js = new StringBuilder()
                    .append("var element = document.getElementsByClassName('verticalToolbarSeparator')[0];")
                    .append("element.style.display = 'inherit';")
                    .append("element.style.visibility = 'inherit';")
                    .toString();
        } else {
            js = new StringBuilder()
                    .append("var element = document.getElementsByClassName('verticalToolbarSeparator')[0];")
                    .append("element.style.display = 'none';")
                    .append("element.style.visibility = 'hidden';")
                    .toString();
        }

        try {
            webView.getEngine().executeScript(js);
        } catch (Exception exception) {
            if(!pdfJsLoaded) {
                toExecuteWhenPDFJSLoaded += js;
            }
        }
    }

    @SuppressWarnings("all")
    public void setVisibilityOf(String id, boolean value) {
        String css;

        if (value) {
            css = new StringBuilder()
                    .append("document.getElementById('" + id + "').style.display = 'inherit';")
                    .append("document.getElementById('" + id + "').style.visibility = 'inherit';")
                    .toString();
        } else {
            css = new StringBuilder()
                    .append("document.getElementById('" + id + "').style.display = 'none';")
                    .append("document.getElementById('" + id + "').style.visibility = 'hidden';")
                    .toString();
        }

        try {
            webView.getEngine().executeScript(css);
        } catch (Exception ex) {
            if (!pdfJsLoaded) {
                this.toExecuteWhenPDFJSLoaded += css;
            }
        }
    }

    public int getActualPageNumber(){
        try {
            return (int) webView.getEngine().executeScript("PDFViewerApplication.page;");
        } catch (Exception e) {
            return 0;
        }
    }

    public int getTotalPageCount(){
        try {
            return (int) webView.getEngine().executeScript("PDFViewerApplication.pagesCount;");
        } catch (Exception e) {
            return 0;
        }
    }

    public void navigateByPage(int pageNum) {
        String jsCommand = "goToPage(" + pageNum + ");";
        try {
            webView.getEngine().executeScript(jsCommand);
        } catch (Exception ex) {
            if (!pdfJsLoaded) toExecuteWhenPDFJSLoaded += jsCommand;
        }
    }

    public void executeScript(String js) {
        try {
            this.webView.getEngine().executeScript(js);
        } catch (Exception ex) {
            if (!pdfJsLoaded) toExecuteWhenPDFJSLoaded += String.format("%s;", js);
        }
    }

    private WebView createWebView() {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);

        WebEngine engine = webView.getEngine();
        String url = getClass().getResource(version.getPath()).toExternalForm();

        engine.setJavaScriptEnabled(true);
        engine.load(url);

        engine.getLoadWorker()
                .stateProperty()
                .addListener(new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("java", new JavaSriptLogListener());
                        engine.executeScript("console.log = function(message){ try {java.log(message);} catch(e) {} };");

                        if(newValue == Worker.State.SUCCEEDED) {
                            try {
                                pdfJsLoaded = true;

                                if (loadScript != null) {
                                    logger.debug("PDF already loaded");
                                    engine.executeScript(loadScript);
                                }

                                engine.executeScript(toExecuteWhenPDFJSLoaded);
                                toExecuteWhenPDFJSLoaded = null;
                                observable.removeListener(this);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });

        return webView;
    }

    public Parent toNode() {
        if (webView == null)
            return webView = createWebView();
        else
            return webView;
    }

    public Consumer<Task<String>> getOnLoaderTaskPresent() {
        return onLoaderTaskPresent.get();
    }

    public ObjectProperty<Consumer<Task<String>>> onLoaderTaskPresentProperty() {
        return onLoaderTaskPresent;
    }

    public void setOnLoaderTaskPresent(Consumer<Task<String>> onLoaderTaskPresent) {
        this.onLoaderTaskPresent.set(onLoaderTaskPresent);
    }
}
