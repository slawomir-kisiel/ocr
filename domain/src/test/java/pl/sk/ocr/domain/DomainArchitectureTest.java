package pl.sk.ocr.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class DomainArchitectureTest {

    @Test
    void domainMustNotDependOnInfrastructureOrUi() {
        var classes = new ClassFileImporter().importPackages("pl.sk.ocr.domain");

        noClasses()
            .that().resideInAPackage("pl.sk.ocr.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "javafx..",
                "net.sourceforge.tess4j..",
                "org.apache.pdfbox..",
                "com.google.zxing..",
                "com.fasterxml.jackson..",
                "org.apache.commons.csv..",
                "pl.sk.ocr.cli..",
                "pl.sk.ocr.configurator.."
            )
            .check(classes);
    }
}
