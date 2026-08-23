package pl.sk.ocr.configurator.app;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.domain.trace.TraceImageRef;

public final class DiagnosticExportUseCase {
    private final ObjectMapper mapper = new ObjectMapper()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .enable(SerializationFeature.INDENT_OUTPUT);
    private final ObjectWriter writer = mapper.writer();

    public ExportResult exportSelectedImage(Path targetDirectory, ProcessingTrace trace, TraceImageStore store, List<TraceImageRef> selectedImages) throws IOException {
        var refs = selectedImages == null ? List.<TraceImageRef>of() : selectedImages;
        if (refs.isEmpty()) {
            throw new IllegalArgumentException("No trace image is selected");
        }
        var written = writeImages(targetDirectory, trace, store, refs);
        return new ExportResult(targetDirectory, written);
    }

    public ExportResult exportAllImages(Path targetDirectory, ProcessingTrace trace, TraceImageStore store) throws IOException {
        var refs = imageRefs(trace);
        if (refs.isEmpty()) {
            throw new IllegalArgumentException("Latest trace has no images");
        }
        var written = writeImages(targetDirectory, trace, store, refs);
        return new ExportResult(targetDirectory, written);
    }

    public ExportResult exportMetadata(Path targetDirectory, ProcessingTrace trace) throws IOException {
        requireTrace(trace);
        Files.createDirectories(targetDirectory);
        var target = targetDirectory.resolve("trace-metadata.json");
        mapper.writeValue(target.toFile(), metadata(trace));
        return new ExportResult(targetDirectory, List.of(target));
    }

    public ExportResult exportBundle(Path targetZip, ProcessingTrace trace, TraceImageStore store) throws IOException {
        requireTrace(trace);
        var parent = targetZip.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (var output = new ZipOutputStream(Files.newOutputStream(targetZip))) {
            output.putNextEntry(new ZipEntry("trace-metadata.json"));
            writer.writeValue(new NonClosingOutputStream(output), metadata(trace));
            output.closeEntry();
            var refs = imageRefs(trace);
            for (int i = 0; i < refs.size(); i++) {
                var image = store.get(refs.get(i));
                if (image.isPresent()) {
                    output.putNextEntry(new ZipEntry(imageFileName(i, refs.get(i), trace)));
                    ImageIO.write(image.get().asBufferedImage(), "png", output);
                    output.closeEntry();
                }
            }
            writeTextArtifacts(output, trace);
        }
        return new ExportResult(targetZip, List.of(targetZip));
    }

    private void writeTextArtifacts(ZipOutputStream output, ProcessingTrace trace) throws IOException {
        var artifactIndex = 1;
        for (var entry : trace.entries()) {
            var hocr = stringAttribute(entry.attributes(), "rawOcrHocr");
            if (hocr != null && !hocr.isBlank()) {
                output.putNextEntry(new ZipEntry("artifacts/%03d_%s_raw-ocr.hocr".formatted(artifactIndex, sanitize(entry.stage().name()))));
                output.write(hocr.getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
                artifactIndex++;
            }
            var text = stringAttribute(entry.attributes(), "rawOcr");
            if (text != null && !text.isBlank()) {
                output.putNextEntry(new ZipEntry("artifacts/%03d_%s_raw-ocr.txt".formatted(artifactIndex, sanitize(entry.stage().name()))));
                output.write(text.getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
                artifactIndex++;
            }
        }
    }

    private String stringAttribute(Map<String, Object> attributes, String key) {
        var value = attributes == null ? null : attributes.get(key);
        return value == null ? null : value.toString();
    }

    private List<Path> writeImages(Path targetDirectory, ProcessingTrace trace, TraceImageStore store, List<TraceImageRef> refs) throws IOException {
        Files.createDirectories(targetDirectory);
        var written = new java.util.ArrayList<Path>();
        for (int i = 0; i < refs.size(); i++) {
            var image = store.get(refs.get(i));
            if (image.isPresent()) {
                var target = targetDirectory.resolve(imageFileName(i, refs.get(i), trace));
                ImageIO.write(image.get().asBufferedImage(), "png", target.toFile());
                written.add(target);
            }
        }
        if (written.isEmpty()) {
            throw new IllegalArgumentException("Selected trace images are no longer available");
        }
        return written;
    }

    private List<TraceImageRef> imageRefs(ProcessingTrace trace) {
        requireTrace(trace);
        return trace.entries().stream()
            .flatMap(entry -> entry.images().stream())
            .toList();
    }

    private Map<String, Object> metadata(ProcessingTrace trace) {
        requireTrace(trace);
        var root = new LinkedHashMap<String, Object>();
        root.put("mode", trace.mode().name());
        root.put("stages", trace.stages().stream()
            .map(stage -> Map.of(
                "stage", stage.stage().name(),
                "status", stage.status().name(),
                "issues", stage.issues().stream().map(issue -> Map.of(
                    "severity", issue.severity().name(),
                    "code", issue.code().value(),
                    "stage", issue.stage().name(),
                    "message", issue.message()
                )).toList()
            ))
            .toList());
        root.put("entries", trace.entries().stream()
            .map(entry -> Map.of(
                "stage", entry.stage().name(),
                "message", entry.message(),
                "attributes", entry.attributes(),
                "images", entry.images().stream().map(ref -> Map.of("id", ref.id(), "label", ref.label())).toList()
            ))
            .toList());
        return root;
    }

    private String imageFileName(int index, TraceImageRef ref, ProcessingTrace trace) {
        var stage = stageFor(ref, trace);
        return "%03d_%s_%03d.png".formatted(index + 1, sanitize(stage), index + 1);
    }

    private String stageFor(TraceImageRef ref, ProcessingTrace trace) {
        if (trace != null) {
            for (var entry : trace.entries()) {
                if (entry.images().contains(ref)) {
                    return entry.stage().name();
                }
            }
        }
        return "TRACE_IMAGE";
    }

    private String sanitize(String value) {
        return (value == null || value.isBlank() ? "TRACE" : value)
            .replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private void requireTrace(ProcessingTrace trace) {
        if (trace == null || trace.entries().isEmpty() && trace.stages().isEmpty()) {
            throw new IllegalArgumentException("No trace data is available");
        }
    }

    private static final class NonClosingOutputStream extends java.io.FilterOutputStream {
        private NonClosingOutputStream(java.io.OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }

    public record ExportResult(Path target, List<Path> files) {
        public ExportResult {
            files = List.copyOf(files == null ? List.of() : files);
        }
    }
}
