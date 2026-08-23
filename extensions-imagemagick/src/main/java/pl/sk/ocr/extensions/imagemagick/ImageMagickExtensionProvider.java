package pl.sk.ocr.extensions.imagemagick;

import java.util.Collection;
import java.util.List;
import pl.sk.ocr.extension.api.Extension;
import pl.sk.ocr.extension.api.ExtensionProvider;

public final class ImageMagickExtensionProvider implements ExtensionProvider {
    @Override
    public Collection<? extends Extension> extensions() {
        return List.of(
            new ProfileImageProcessor(),
            new NormalizeImageProcessor(),
            new AutoThresholdImageProcessor(),
            new AdaptiveThresholdImageProcessor(),
            new DeskewImageProcessor(),
            new BackgroundCorrectImageProcessor(),
            new MedianImageProcessor(),
            new MorphologyImageProcessor()
        );
    }
}
