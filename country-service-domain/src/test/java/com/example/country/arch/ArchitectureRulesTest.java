package com.example.country.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTest {
    @Test
    void domainHasNoDependencyOnApplicationOrAdapters() {
        JavaClasses imported = new ClassFileImporter().importPackages(
                "com.example.country.domain",
                "com.example.country.application",
                "com.example.country.adapters");

        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapters..")
                .check(imported);
    }
}
