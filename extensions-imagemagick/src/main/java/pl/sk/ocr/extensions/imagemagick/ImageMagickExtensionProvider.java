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
            new GrayscaleImageProcessor(),
            new NormalizeImageProcessor(),
            new ContrastStretchImageProcessor(),
            new LevelImageProcessor(),
            new GammaImageProcessor(),
            new SigmoidalContrastImageProcessor(),
            new EqualizeImageProcessor(),
            new ClaheImageProcessor(),
            new LocalContrastImageProcessor(),
            new WhiteBalanceImageProcessor(),
            new ThresholdImageProcessor(),
            new BlackThresholdImageProcessor(),
            new WhiteThresholdImageProcessor(),
            new RangeThresholdImageProcessor(),
            new HsvThresholdImageProcessor(),
            new AutoThresholdImageProcessor(),
            new AdaptiveThresholdImageProcessor(),
            new BoxBlurImageProcessor(),
            new GaussianBlurImageProcessor(),
            new SharpenImageProcessor(),
            new UnsharpImageProcessor(),
            new BilateralImageProcessor(),
            new KuwaharaImageProcessor(),
            new SobelImageProcessor(),
            new RotateImageProcessor(),
            new FlipVerticalImageProcessor(),
            new FlopHorizontalImageProcessor(),
            new TransposeImageProcessor(),
            new TransverseImageProcessor(),
            new CropImageProcessor(),
            new ShaveImageProcessor(),
            new ExtentImageProcessor(),
            new PerspectiveImageProcessor(),
            new TrimImageProcessor(),
            new PageContourCropImageProcessor(),
            new RemoveSmallComponentsImageProcessor(),
            new NegateImageProcessor(),
            new PosterizeImageProcessor(),
            new StripMetadataImageProcessor(),
            new DeskewImageProcessor(),
            new BackgroundCorrectImageProcessor(),
            new MedianImageProcessor(),
            new MorphologyImageProcessor(),
            new RemoveTableFramesImageProcessor()
        );
    }
}
