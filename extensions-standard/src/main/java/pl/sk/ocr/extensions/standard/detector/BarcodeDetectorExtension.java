package pl.sk.ocr.extensions.standard.detector;

import com.google.zxing.BarcodeFormat;
import static pl.sk.ocr.extensions.standard.StandardDescriptors.extensionDescriptor;

import java.util.List;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionType;

public final class BarcodeDetectorExtension extends AbstractZxingDetectorExtension {
    public BarcodeDetectorExtension() {
        super(List.of(
            BarcodeFormat.AZTEC,
            BarcodeFormat.CODABAR,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.ITF,
            BarcodeFormat.PDF_417,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E
        ));
    }

    @Override
    public ExtensionDescriptor descriptor() {
        return extensionDescriptor("barcode", ExtensionType.DETECTOR, "Barcode", "Detects barcodes using ZXing.");
    }
}

