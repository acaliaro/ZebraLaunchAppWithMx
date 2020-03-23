package it.soluzione1.zebralaunchappwithmx;

public abstract class Sdk {

    Sdk() {
    }

    public abstract void release();
    public abstract String SdkVersion();
    public abstract String ExtensionVersion();
    public abstract String BarcodeVersion();
    public abstract boolean isSdkReady();
    public abstract void executeCommand(String xml, String command);
}
