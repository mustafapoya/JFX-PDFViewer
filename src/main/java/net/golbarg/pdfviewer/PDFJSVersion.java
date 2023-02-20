package net.golbarg.pdfviewer;

public enum PDFJSVersion {
    VERSION_3_3_122("/pdfjs_3.3.122/web/viewer.html"),
    VERSION_2_2_228("/pdfjs_2.2.228/web/viewer.html");

    private final String path;

    PDFJSVersion(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static PDFJSVersion latestVersion() {
        return VERSION_3_3_122;
    }
}
