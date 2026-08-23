package pl.sk.ocr.extensions.standard;

import java.util.Collection;
import java.util.List;
import pl.sk.ocr.extension.api.Extension;
import pl.sk.ocr.extension.api.ExtensionProvider;
import pl.sk.ocr.extensions.standard.detector.BarcodeDetectorExtension;
import pl.sk.ocr.extensions.standard.detector.QrDetectorExtension;
import pl.sk.ocr.extensions.standard.detector.TextDetectorExtension;
import pl.sk.ocr.extensions.standard.image.CondenseContentImageProcessor;
import pl.sk.ocr.extensions.standard.image.CropEmptyMarginsImageProcessor;
import pl.sk.ocr.extensions.standard.image.RemoveBoxesImageProcessor;
import pl.sk.ocr.extensions.standard.matcher.ContainsMatcher;
import pl.sk.ocr.extensions.standard.matcher.ExactMatcher;
import pl.sk.ocr.extensions.standard.matcher.FuzzyMatcher;
import pl.sk.ocr.extensions.standard.matcher.NormalizedMatcher;
import pl.sk.ocr.extensions.standard.matcher.RegexMatcher;
import pl.sk.ocr.extensions.standard.transform.NormalizeTransformer;
import pl.sk.ocr.extensions.standard.transform.RemoveWhitespaceTransformer;
import pl.sk.ocr.extensions.standard.transform.SubstringTransformer;
import pl.sk.ocr.extensions.standard.transform.TrimTransformer;
import pl.sk.ocr.extensions.standard.validation.DictionaryValidator;
import pl.sk.ocr.extensions.standard.validation.NipValidator;
import pl.sk.ocr.extensions.standard.validation.PeselValidator;
import pl.sk.ocr.extensions.standard.validation.RegexValidator;
import pl.sk.ocr.extensions.standard.validation.RegonValidator;

public final class StandardExtensionProvider implements ExtensionProvider {
    @Override
    public Collection<? extends Extension> extensions() {
        return List.of(
            new ExactMatcher(),
            new ContainsMatcher(),
            new NormalizedMatcher(),
            new FuzzyMatcher(),
            new RegexMatcher(),
            new TextDetectorExtension(),
            new QrDetectorExtension(),
            new BarcodeDetectorExtension(),
            new RemoveBoxesImageProcessor(),
            new CondenseContentImageProcessor(),
            new CropEmptyMarginsImageProcessor(),
            new TrimTransformer(),
            new RemoveWhitespaceTransformer(),
            new SubstringTransformer(),
            new NormalizeTransformer(),
            new PeselValidator(),
            new NipValidator(),
            new RegonValidator(),
            new DictionaryValidator(),
            new RegexValidator()
        );
    }
}

