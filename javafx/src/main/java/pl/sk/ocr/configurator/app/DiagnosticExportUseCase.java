package pl.sk.ocr.configurator.app;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import pl.sk.ocr.configurator.result.CategoryReferenceDocumentTestResult;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.StageResult;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.domain.trace.TraceEntry;
import pl.sk.ocr.domain.trace.TraceImageRef;
import pl.sk.ocr.extension.api.image.ProcessingImage;

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

    public ExportResult exportReferenceDocumentBundle(Path targetZip, List<CategoryReferenceDocumentTestResult> results) throws IOException {
        var documents = results == null ? List.<CategoryReferenceDocumentTestResult>of() : results;
        if (documents.isEmpty()) {
            throw new IllegalArgumentException("No reference document test results are available");
        }
        var parent = targetZip.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (var output = new ZipOutputStream(Files.newOutputStream(targetZip))) {
            output.putNextEntry(new ZipEntry("metadata.json"));
            writer.writeValue(new NonClosingOutputStream(output), referenceDocumentsMetadata(documents));
            output.closeEntry();
            for (var document : documents) {
                writeReferenceDocument(output, document);
            }
        }
        return new ExportResult(targetZip, List.of(targetZip));
    }

    private void writeReferenceDocument(ZipOutputStream output, CategoryReferenceDocumentTestResult document) throws IOException {
        var prefix = "documents/" + sanitize(document.referenceDocumentId()) + "/";
        var trace = document.result() == null ? ProcessingTrace.off() : document.result().trace();
        output.putNextEntry(new ZipEntry(prefix + "trace.json"));
        writer.writeValue(new NonClosingOutputStream(output), trace == null ? metadata(ProcessingTrace.off()) : metadata(trace));
        output.closeEntry();
        var refs = trace == null || trace.entries().isEmpty() && trace.stages().isEmpty() ? List.<TraceImageRef>of() : imageRefs(trace);
        var store = document.traceImageStore();
        for (int i = 0; i < refs.size(); i++) {
            if (store == null) {
                continue;
            }
            var image = store.get(refs.get(i));
            if (image.isPresent()) {
                output.putNextEntry(new ZipEntry(prefix + "images/" + imageFileName(i, refs.get(i), trace)));
                ImageIO.write(image.get().asBufferedImage(), "png", output);
                output.closeEntry();
            }
        }
        writeTextArtifacts(output, trace, prefix + "artifacts/");
    }

    private void writeTextArtifacts(ZipOutputStream output, ProcessingTrace trace) throws IOException {
        writeTextArtifacts(output, trace, "artifacts/");
    }

    private void writeTextArtifacts(ZipOutputStream output, ProcessingTrace trace, String prefix) throws IOException {
        if (trace == null) {
            return;
        }
        var artifactIndex = 1;
        for (var entry : trace.entries()) {
            var hocr = stringAttribute(entry.attributes(), "rawOcrHocr");
            if (hocr != null && !hocr.isBlank()) {
                output.putNextEntry(new ZipEntry(prefix + "%03d_%s_raw-ocr.hocr".formatted(artifactIndex, sanitize(entry.stage().name()))));
                output.write(hocr.getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
                artifactIndex++;
            }
            var text = stringAttribute(entry.attributes(), "rawOcr");
            if (text != null && !text.isBlank()) {
                output.putNextEntry(new ZipEntry(prefix + "%03d_%s_raw-ocr.txt".formatted(artifactIndex, sanitize(entry.stage().name()))));
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
        if (trace == null) {
            trace = ProcessingTrace.off();
        }
        var root = new LinkedHashMap<String, Object>();
        root.put("mode", trace.mode().name());
        root.put("stages", trace.stages().stream()
            .map(this::stageMetadata)
            .toList());
        root.put("entries", trace.entries().stream()
            .map(this::entryMetadata)
            .toList());
        return root;
    }

    private Map<String, Object> stageMetadata(StageResult stage) {
        var item = new LinkedHashMap<String, Object>();
        item.put("stage", stage.stage().name());
        item.put("status", stage.status().name());
        item.put("issues", stage.issues().stream().map(this::issueMetadata).toList());
        return item;
    }

    private Map<String, Object> issueMetadata(ProcessingIssue issue) {
        var item = new LinkedHashMap<String, Object>();
        item.put("severity", issue.severity().name());
        item.put("code", issue.code().value());
        item.put("stage", issue.stage().name());
        item.put("message", issue.message());
        return item;
    }

    private Map<String, Object> entryMetadata(TraceEntry entry) {
        var item = new LinkedHashMap<String, Object>();
        item.put("stage", entry.stage().name());
        item.put("message", entry.message());
        item.put("attributes", jsonSafe(entry.attributes()));
        item.put("images", entry.images().stream().map(this::traceImageRefMetadata).toList());
        return item;
    }

    private Map<String, Object> traceImageRefMetadata(TraceImageRef ref) {
        var item = new LinkedHashMap<String, Object>();
        item.put("id", ref.id());
        item.put("label", ref.label());
        return item;
    }

    private Object jsonSafe(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Path path) {
            return path.toString();
        }
        if (value instanceof Rectangle rectangle) {
            var item = new LinkedHashMap<String, Object>();
            item.put("x", rectangle.x);
            item.put("y", rectangle.y);
            item.put("width", rectangle.width);
            item.put("height", rectangle.height);
            return item;
        }
        if (value instanceof ProcessingImage image) {
            var item = new LinkedHashMap<String, Object>();
            item.put("type", value.getClass().getName());
            item.put("width", image.width());
            item.put("height", image.height());
            return item;
        }
        if (value instanceof TraceImageRef ref) {
            return traceImageRefMetadata(ref);
        }
        if (value instanceof ProcessingIssue issue) {
            return issueMetadata(issue);
        }
        if (value instanceof Map<?, ?> map) {
            var item = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                item.put(String.valueOf(entry.getKey()), jsonSafe(entry.getValue()));
            }
            return item;
        }
        if (value instanceof Iterable<?> iterable) {
            var items = new ArrayList<Object>();
            for (var item : iterable) {
                items.add(jsonSafe(item));
            }
            return items;
        }
        if (value.getClass().isArray()) {
            var items = new ArrayList<Object>();
            for (int i = 0; i < Array.getLength(value); i++) {
                items.add(jsonSafe(Array.get(value, i)));
            }
            return items;
        }
        return value.toString();
    }

    private Map<String, Object> referenceDocumentsMetadata(List<CategoryReferenceDocumentTestResult> results) {
        var root = new LinkedHashMap<String, Object>();
        root.put("documents", results.stream().map(result -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("referenceDocumentId", result.referenceDocumentId());
            item.put("referenceDocumentPath", result.referenceDocumentPath());
            item.put("resolvedPath", result.resolvedPath() == null ? "" : result.resolvedPath().toString());
            var document = result.result();
            item.put("documentId", document == null ? "" : document.documentId().value());
            item.put("status", document == null ? "" : document.status().name());
            item.put("categoryId", document == null || document.categoryId() == null ? "" : document.categoryId().value());
            item.put("issueCodes", document == null ? List.of() : issueCodes(document));
            item.put("traceEntries", document == null || document.trace() == null ? 0 : document.trace().entries().size());
            item.put("traceImages", document == null ? 0 : traceImageCount(document.trace()));
            return item;
        }).toList());
        return root;
    }

    private int traceImageCount(ProcessingTrace trace) {
        if (trace == null || trace.entries().isEmpty()) {
            return 0;
        }
        return imageRefs(trace).size();
    }

    private List<String> issueCodes(DocumentResult result) {
        var codes = new java.util.ArrayList<String>();
        result.issues().forEach(issue -> codes.add(issue.code().value()));
        result.fields().stream()
            .flatMap(field -> field.issues().stream())
            .forEach(issue -> codes.add(issue.code().value()));
        return List.copyOf(codes);
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
