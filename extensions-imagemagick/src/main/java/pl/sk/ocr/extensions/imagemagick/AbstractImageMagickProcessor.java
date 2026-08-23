package pl.sk.ocr.extensions.imagemagick;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import pl.sk.ocr.extension.api.ExtensionDescriptor;
import pl.sk.ocr.extension.api.ExtensionParameters;
import pl.sk.ocr.extension.api.image.ImageProcessingContext;
import pl.sk.ocr.extension.api.image.ImageProcessingRequest;
import pl.sk.ocr.extension.api.image.ImageProcessor;
import pl.sk.ocr.extension.api.image.ProcessingImage;

abstract class AbstractImageMagickProcessor implements ImageProcessor {
    private final ExtensionDescriptor descriptor;

    AbstractImageMagickProcessor(ExtensionDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public final ExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public final ProcessingImage process(ImageProcessingRequest request, ImageProcessingContext context) {
        if (request == null || request.image() == null) {
            throw new IllegalArgumentException("image is required");
        }
        var input = request.image().asBufferedImage();
        var output = apply(input, request.parameters() == null ? ExtensionParameters.empty() : request.parameters(), context);
        trace(request, context, input, output);
        return new ImageMagickProcessingImage(output);
    }

    abstract BufferedImage apply(BufferedImage input, ExtensionParameters parameters, ImageProcessingContext context);

    int integer(ExtensionParameters parameters, String name, int defaultValue) {
        return parameters.get(name).map(value -> {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(value.toString());
        }).orElse(defaultValue);
    }

    double decimal(ExtensionParameters parameters, String name, double defaultValue) {
        return parameters.get(name).map(value -> {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            return Double.parseDouble(value.toString());
        }).orElse(defaultValue);
    }

    boolean bool(ExtensionParameters parameters, String name, boolean defaultValue) {
        return parameters.get(name).map(value -> {
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return Boolean.parseBoolean(value.toString());
        }).orElse(defaultValue);
    }

    String string(ExtensionParameters parameters, String name, String defaultValue) {
        return parameters.get(name).map(Object::toString).filter(value -> !value.isBlank()).orElse(defaultValue);
    }

    <T extends Enum<T>> T enumValue(ExtensionParameters parameters, String name, Class<T> type, T defaultValue) {
        return Enum.valueOf(type, string(parameters, name, defaultValue.name()));
    }

    private void trace(ImageProcessingRequest request, ImageProcessingContext context, BufferedImage input, BufferedImage output) {
        if (context == null || context.trace() == null) {
            return;
        }
        var attributes = new HashMap<String, Object>();
        attributes.put("processorId", descriptor.id().value());
        attributes.put("inputWidth", input.getWidth());
        attributes.put("inputHeight", input.getHeight());
        attributes.put("outputWidth", output.getWidth());
        attributes.put("outputHeight", output.getHeight());
        attributes.put("parameters", request.parameters() == null ? Map.of() : request.parameters().asMap());
        extraTrace().apply(output).forEach(attributes::put);
        context.trace().add("imagemagick.processor", attributes);
    }

    Function<BufferedImage, Map<String, Object>> extraTrace() {
        return ignored -> Map.of();
    }
}
