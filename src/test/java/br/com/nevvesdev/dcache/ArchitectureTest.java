package br.com.nevvesdev.dcache;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("br.com.nevvesdev.dcache");

    @Test
    void domainShouldNotDependOnOuterLayers() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..adapter..", "..infrastructure..")
            .check(CLASSES);
    }

    @Test
    void domainShouldBeFrameworkFree() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..")
            .check(CLASSES);
    }

    @Test
    void applicationShouldNotDependOnAdaptersOrInfrastructure() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter..", "..infrastructure..")
            .check(CLASSES);
    }

    @Test
    void applicationShouldBeFrameworkFree() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..")
            .check(CLASSES);
    }
}
