package pl.sk.ocr.cli;

import picocli.CommandLine.IVersionProvider;

public final class OcrVersionProvider implements IVersionProvider {
    @Override
    public String[] getVersion() {
        var version = OcrVersionProvider.class.getPackage().getImplementationVersion();
        return new String[] {
            "pl.sk.ocr " + (version == null ? "0.1.0-SNAPSHOT" : version),
            "Java " + Runtime.version().feature()
        };
    }
}
