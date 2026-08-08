package pl.sk.ocr.core.batch;

import java.util.List;
import pl.sk.ocr.core.output.BatchSummaryBuilder;
import pl.sk.ocr.core.output.CsvResultWriter;
import pl.sk.ocr.core.output.JsonBatchSummaryWriter;
import pl.sk.ocr.core.output.OutputSchemaBuilder;
import pl.sk.ocr.core.output.OutputSchemaValidator;
import pl.sk.ocr.core.output.ResultRowMapper;
import pl.sk.ocr.core.processing.DocumentProcessor;
import pl.sk.ocr.domain.identifier.BatchId;
import pl.sk.ocr.domain.result.BatchResult;

public final class BatchDispatcher {
    private final InputScanner scanner;
    private final WorkerPool workerPool;
    private final OutputSchemaBuilder schemaBuilder;
    private final OutputSchemaValidator schemaValidator;
    private final ResultRowMapper rowMapper;
    private final CsvResultWriter csvWriter;
    private final BatchSummaryBuilder summaryBuilder;
    private final JsonBatchSummaryWriter summaryWriter;

    public BatchDispatcher(DocumentProcessor processor) {
        this(
            new InputScanner(),
            new WorkerPool(processor),
            new OutputSchemaBuilder(),
            new OutputSchemaValidator(),
            new ResultRowMapper(),
            new CsvResultWriter(),
            new BatchSummaryBuilder(),
            new JsonBatchSummaryWriter()
        );
    }

    public BatchDispatcher(InputScanner scanner, WorkerPool workerPool, OutputSchemaBuilder schemaBuilder,
                           OutputSchemaValidator schemaValidator, ResultRowMapper rowMapper, CsvResultWriter csvWriter,
                           BatchSummaryBuilder summaryBuilder, JsonBatchSummaryWriter summaryWriter) {
        this.scanner = scanner;
        this.workerPool = workerPool;
        this.schemaBuilder = schemaBuilder;
        this.schemaValidator = schemaValidator;
        this.rowMapper = rowMapper;
        this.csvWriter = csvWriter;
        this.summaryBuilder = summaryBuilder;
        this.summaryWriter = summaryWriter;
    }

    public BatchResult run(BatchId batchId, BatchOptions options) {
        var schema = schemaBuilder.build(options.configuration());
        var schemaProblems = schemaValidator.validate(options.configuration());
        if (!schemaProblems.isEmpty()) {
            throw new BatchProcessingException("OUTPUT_SCHEMA_INVALID", schemaProblems.toString(), null);
        }
        var jobs = scanner.scan(options.configuration().profile().directories().input());
        var counters = new BatchCounters();
        var items = workerPool.process(jobs, options.configuration(), options.workers(), counters);
        var rows = items.stream()
            .map(item -> rowMapper.map(item.result(), schema, item.processingDurationMs()))
            .toList();
        csvWriter.write(options.outputFile(), schema, rows, options.configuration().profile().csvOutput());
        var documents = items.stream().map(BatchItemResult::result).toList();
        var summary = summaryBuilder.build(documents);
        options.summaryOutputFile().ifPresent(path -> summaryWriter.write(path, summary));
        return BatchResult.from(batchId, documents, List.of());
    }
}
