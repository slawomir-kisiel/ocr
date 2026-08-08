package pl.sk.ocr.cli;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import pl.sk.ocr.config.ConfigurationException;
import pl.sk.ocr.core.batch.BatchProcessingException;

public final class OcrCliApplication {
    private final CliBootstrap bootstrap;
    private final BatchExecutor batchExecutor;
    private final ExitCodeResolver exitCodeResolver;
    private final BatchIdFactory batchIdFactory;
    private final ConsoleSummaryRenderer summaryRenderer;
    private final PrintWriter out;
    private final PrintWriter err;

    public OcrCliApplication() {
        this(new DefaultCliBootstrap(), new DefaultBatchExecutor(), new ExitCodeResolver(), new DefaultBatchIdFactory(),
            new PrintWriter(System.out, true), new PrintWriter(System.err, true));
    }

    public OcrCliApplication(CliBootstrap bootstrap, BatchExecutor batchExecutor, ExitCodeResolver exitCodeResolver,
                             BatchIdFactory batchIdFactory, PrintWriter out, PrintWriter err) {
        this.bootstrap = bootstrap;
        this.batchExecutor = batchExecutor;
        this.exitCodeResolver = exitCodeResolver;
        this.batchIdFactory = batchIdFactory;
        this.summaryRenderer = new ConsoleSummaryRenderer();
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        System.exit(new OcrCliApplication().run(args));
    }

    public int run(String... args) {
        var options = new CliOptions();
        var command = new CommandLine(options);
        command.setOut(out);
        command.setErr(err);
        try {
            var parseResult = command.parseArgs(args);
            if (command.isUsageHelpRequested()) {
                command.usage(out);
                return 0;
            }
            if (command.isVersionHelpRequested()) {
                command.printVersionHelp(out);
                return 0;
            }
            validateRequired(options);
            var started = Instant.now();
            var context = bootstrap.bootstrap(options);
            summaryRenderer.started(out, context.configuration().profile().id(), context.batchOptions().workers(),
                context.configuration().profile().directories().input());
            var result = batchExecutor.execute(batchIdFactory.create(), context);
            summaryRenderer.completed(out, result, Duration.between(started, Instant.now()), context.batchOptions().outputFile());
            return exitCodeResolver.resolve(CliExecutionStatus.SUCCESS);
        } catch (ParameterException | IllegalArgumentException e) {
            return fail(CliExecutionStatus.ARGUMENT_ERROR, e, false);
        } catch (ConfigurationException e) {
            return fail(CliExecutionStatus.CONFIGURATION_ERROR, e, false);
        } catch (CliEnvironmentException e) {
            return fail(CliExecutionStatus.ENVIRONMENT_ERROR, e, false);
        } catch (BatchProcessingException e) {
            return fail(CliExecutionStatus.EXECUTION_ERROR, e, true);
        } catch (RuntimeException e) {
            return fail(CliExecutionStatus.EXECUTION_ERROR, e, true);
        }
    }

    private void validateRequired(CliOptions options) {
        if (options.profile() == null) {
            throw new ParameterException(new CommandLine(options), "Missing required option: --profile");
        }
        if (options.workers() != null && options.workers() < 1) {
            throw new ParameterException(new CommandLine(options), "--workers must be >= 1");
        }
    }

    private int fail(CliExecutionStatus status, RuntimeException e, boolean logStackTrace) {
        err.println("ERROR: " + e.getMessage());
        if (logStackTrace && LoggerFactory.getLogger(OcrCliApplication.class).isDebugEnabled()) {
            LoggerFactory.getLogger(OcrCliApplication.class).debug("CLI execution failed", e);
        }
        err.flush();
        return exitCodeResolver.resolve(status);
    }
}
