package pl.sk.ocr.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.ConfigurationException;
import pl.sk.ocr.config.ConfigurationProblem;
import pl.sk.ocr.config.runtime.*;
import pl.sk.ocr.core.batch.BatchOptions;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.identifier.BatchId;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.result.BatchResult;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.trace.ProcessingTrace;
import pl.sk.ocr.domain.trace.TraceMode;

class OcrCliApplicationTest {

    @Test
    void helpDoesNotBootstrapAndReturnsZero() {
        var harness = harness(failingBootstrap());

        var code = harness.app.run("--help");

        assertThat(code).isZero();
        assertThat(harness.outText()).contains("Usage:");
        assertThat(harness.errText()).isBlank();
    }

    @Test
    void versionDoesNotBootstrapAndReturnsZero() {
        var harness = harness(failingBootstrap());

        var code = harness.app.run("--version");

        assertThat(code).isZero();
        assertThat(harness.outText()).contains("pl.sk.ocr").contains("Java");
    }

    @Test
    void missingProfileReturnsArgumentError() {
        var harness = harness(failingBootstrap());

        var code = harness.app.run("--workers", "2");

        assertThat(code).isEqualTo(1);
        assertThat(harness.errText()).contains("--profile");
        assertThat(harness.errText()).doesNotContain("CLI execution failed");
    }

    @Test
    void invalidWorkersReturnsArgumentError() {
        var harness = harness(failingBootstrap());

        var code = harness.app.run("--profile", "profile.json", "--workers", "0");

        assertThat(code).isEqualTo(1);
        assertThat(harness.errText()).contains("workers");
    }

    @Test
    void invalidTraceReturnsArgumentError() {
        var harness = harness(failingBootstrap());

        var code = harness.app.run("--profile", "profile.json", "--trace", "SOMETHING");

        assertThat(code).isEqualTo(1);
    }

    @Test
    void configurationErrorReturnsCodeTwo() {
        var harness = harness(options -> {
            throw new ConfigurationException(List.of(new ConfigurationProblem("PROFILE_INVALID", "$", "bad")));
        });

        var code = harness.app.run("--profile", "profile.json");

        assertThat(code).isEqualTo(2);
        assertThat(harness.errText()).contains("Configuration is invalid");
    }

    @Test
    void environmentErrorReturnsCodeThree() {
        var harness = harness(options -> {
            throw new CliEnvironmentException("input directory is not readable");
        });

        var code = harness.app.run("--profile", "profile.json");

        assertThat(code).isEqualTo(3);
        assertThat(harness.errText()).contains("input directory");
    }

    @Test
    void documentFailuresStillReturnSuccessExitCode() {
        var context = context(2);
        var harness = harness(options -> context, (batchId, processingContext) -> BatchResult.from(batchId, List.of(
            new DocumentResult(new DocumentId("ok.pdf"), null, ProcessingStatus.SUCCESS, List.of(), List.of(), ProcessingTrace.off()),
            new DocumentResult(new DocumentId("bad.pdf"), null, ProcessingStatus.FAILED, List.of(), List.of(), ProcessingTrace.off())
        ), List.of()));

        var code = harness.app.run("--profile", "profile.json");

        assertThat(code).isZero();
        assertThat(harness.outText()).contains("Batch completed").contains("Documents: 2").contains("Failed: 1");
    }

    @Test
    void passesWorkerOverrideToBootstrapContext() {
        var harness = harness(options -> context(options.workers()), (batchId, processingContext) -> {
            assertThat(processingContext.batchOptions().workers()).isEqualTo(7);
            return BatchResult.from(batchId, List.of(), List.of());
        });

        var code = harness.app.run("--profile", "profile.json", "--workers", "7");

        assertThat(code).isZero();
    }

    private static Harness harness(CliBootstrap bootstrap) {
        return harness(bootstrap, (batchId, context) -> BatchResult.from(batchId, List.of(), List.of()));
    }

    private static Harness harness(CliBootstrap bootstrap, BatchExecutor executor) {
        var out = new StringWriter();
        var err = new StringWriter();
        var app = new OcrCliApplication(
            bootstrap,
            executor,
            new ExitCodeResolver(),
            () -> new BatchId("batch-1"),
            new PrintWriter(out, true),
            new PrintWriter(err, true)
        );
        return new Harness(app, out, err);
    }

    private static CliBootstrap failingBootstrap() {
        return options -> {
            throw new AssertionError("bootstrap should not run");
        };
    }

    private static ProcessingContext context(Integer workers) {
        var profile = new ProfileRuntimeConfiguration(
            "test",
            new ConfigurationVersion("1.0"),
            Path.of("."),
            CategoriesMode.EXPLICIT,
            List.of(new CategoryId("category")),
            new DirectoriesConfiguration(Path.of("input"), Path.of("success"), Path.of("error")),
            new ProcessingConfiguration(workers == null ? 1 : workers, 16),
            OcrSettings.defaults(),
            TraceMode.OFF,
            new CsvOutputConfiguration(Path.of("result.csv"), java.nio.charset.StandardCharsets.UTF_8, ";", "\"", true, true)
        );
        var configuration = new RuntimeConfiguration(profile, List.of());
        return new ProcessingContext(configuration, null, new BatchOptions(configuration, workers, null, null));
    }

    private record Harness(OcrCliApplication app, StringWriter out, StringWriter err) {
        public String outText() {
            return out.toString();
        }

        public String errText() {
            return err.toString();
        }
    }
}
