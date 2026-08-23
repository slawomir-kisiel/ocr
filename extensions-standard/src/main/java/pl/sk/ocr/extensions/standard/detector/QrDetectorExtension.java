package pl.sk.ocr.extensions.standard.detector;

import com.google.zxing.BarcodeFormat;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;

public final class QrDetectorExtension extends AbstractZxingDetectorExtension {
    public QrDetectorExtension() {
        super(java.util.List.of(BarcodeFormat.QR_CODE));
    }

    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("qr", ExtensionType.DETECTOR, "QR", "Detects QR codes using ZXing.");
    }
}

