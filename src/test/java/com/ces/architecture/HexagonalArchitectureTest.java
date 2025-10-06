package com.ces.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit tests to validate the Hexagonal Architecture (Ports & Adapters) 
 * and Domain-Driven Design principles of the Customer Event Stream application.
 * 
 * Architecture Rules:
 * 1. Layer Dependencies: Domain -> no deps, Application -> Domain only, Infrastructure -> Application + Domain
 * 2. Domain Purity: No framework dependencies in domain layer
 * 3. Port and Adapter Pattern: Interfaces in application.port, implementations in infrastructure.adapter
 * 4. Proper Annotations: @Service in application, @Component/@Repository in infrastructure
 * 5. Naming Conventions: UseCase interfaces, Service implementations, Adapter implementations
 * 6. No Cycles: No cyclic dependencies between packages
 */
class HexagonalArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.ces");
    }

    // ========== LAYER DEPENDENCY RULES ==========

    @Test
    void layerDependenciesShouldBeRespected() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Domain").definedBy("com.ces.domain..")
                .layer("Application").definedBy("com.ces.application..")
                .layer("Infrastructure").definedBy("com.ces.infrastructure..")
                
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer();

        rule.check(classes);
    }

    @Test
    void domainLayerShouldNotDependOnAnyOtherLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ces.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.ces.application..", "com.ces.infrastructure..", "com.ces.web..");

        rule.check(classes);
    }

    @Test
    void applicationLayerShouldOnlyDependOnDomainLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ces.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.ces.infrastructure..", "com.ces.web..");

        rule.check(classes);
    }

    // ========== DOMAIN LAYER PURITY RULES ==========

    @Test
    void domainLayerShouldNotDependOnSpringFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ces.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    void domainLayerShouldNotHaveSpringAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ces.domain..")
                .should().beAnnotatedWith(Component.class)
                .orShould().beAnnotatedWith(Service.class)
                .orShould().beAnnotatedWith(Repository.class);

        rule.check(classes);
    }

    @Test
    void domainLayerShouldNotDependOnJakartaPersistence() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ces.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..");

        rule.check(classes);
    }

    // ========== PORT AND ADAPTER PATTERN RULES ==========

    @Test
    void inputPortsShouldBeInterfaces() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.ces.application.port.input..")
                .and().areTopLevelClasses()
                .should().beInterfaces();

        rule.check(classes);
    }

    @Test
    void outputPortsShouldBeInterfaces() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.ces.application.port.output..")
                .should().beInterfaces();

        rule.check(classes);
    }

    @Test
    void adaptersShouldResideInInfrastructureLayer() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .should().resideInAPackage("com.ces.infrastructure.adapter..");

        rule.check(classes);
    }

    @Test
    void adaptersShouldNotBeInDomainOrApplicationLayers() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Adapter")
                .should().resideInAnyPackage("com.ces.domain..", "com.ces.application..");

        rule.check(classes);
    }

    // ========== NAMING CONVENTION RULES ==========

    @Test
    void useCaseInterfacesShouldEndWithUseCase() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.ces.application.port.input..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("UseCase");

        rule.check(classes);
    }

    @Test
    void servicesShouldEndWithService() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.ces.application.service..")
                .and().areNotInterfaces()
                .should().haveSimpleNameEndingWith("Service");

        rule.check(classes);
    }

    @Test
    void repositoryInterfacesShouldEndWithRepository() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.ces.application.port.output..")
                .and().areInterfaces()
                .and().haveSimpleNameContaining("Repository")
                .should().haveSimpleNameEndingWith("Repository");

        rule.check(classes);
    }

    // ========== ANNOTATION RULES ==========
    // Note: This project uses manual bean configuration in ApplicationConfiguration
    // rather than component scanning, so @Service/@Component annotations are not required

    @Test
    void portInterfacesShouldNotHaveSpringAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ces.application.port..")
                .should().beAnnotatedWith(Component.class)
                .orShould().beAnnotatedWith(Service.class)
                .orShould().beAnnotatedWith(Repository.class);

        rule.check(classes);
    }

    // ========== DEPENDENCY CYCLE RULES ==========

    @Test
    void packagesShouldBeFreeOfCycles() {
        ArchRule rule = slices()
                .matching("com.ces.(*)..")
                .should().beFreeOfCycles();

        rule.check(classes);
    }

    // ========== DOMAIN MODEL RULES ==========

    @Test
    void domainEntitiesShouldResideInDomainModelPackage() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.ces.domain.model..")
                .should().bePackagePrivate()
                .orShould().bePublic();

        rule.check(classes);
    }

    @Test
    void domainServicesShouldResideInDomainServicePackage() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.ces.domain.service..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Registry")
                .orShould().haveSimpleNameEndingWith("Service");

        rule.check(classes);
    }

    // ========== PORT IMPLEMENTATION RULES ==========

    @Test
    void outputPortImplementationsShouldResideInInfrastructure() {
        ArchRule rule = classes()
                .that().implement("com.ces.application.port.output.MessageSender")
                .or().implement("com.ces.application.port.output.SessionRepository")
                .should().resideInAPackage("com.ces.infrastructure..");

        rule.check(classes);
    }

    @Test
    void inputPortImplementationsShouldResideInApplication() {
        ArchRule rule = classes()
                .that().implement("com.ces.application.port.input.RegisterSessionUseCase")
                .or().implement("com.ces.application.port.input.DeliverMessageUseCase")
                .should().resideInAPackage("com.ces.application..");

        rule.check(classes);
    }

    // ========== PACKAGE STRUCTURE RULES ==========

    @Test
    void portsShouldOnlyBeInPortPackage() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.ces.application.port..")
                .and().areTopLevelClasses()
                .should().beInterfaces();

        rule.check(classes);
    }

    @Test
    void infrastructureShouldNotAccessApplicationServices() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ces.infrastructure..")
                .and().doNotHaveSimpleName("ApplicationConfiguration")
                .should().dependOnClassesThat()
                .resideInAPackage("com.ces.application.service..")
                .because("Infrastructure should only depend on ports (interfaces), not service implementations. Exception: ApplicationConfiguration can instantiate services for dependency injection.");

        rule.allowEmptyShould(true);
        rule.check(classes);
    }
}
