package com.storeops;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Deterministic architecture gates. Test-field name prefixes (A1_, A2_, A3_, A4_) map to Evaluator check IDs
 * in .harness/eval/checks.csv - do not rename without updating collect-gates.py.
 */
@AnalyzeClasses(packages = "com.storeops", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // ---- A1: module boundary (failure mode 1) ----
    private static ArchRule repositoryIsPrivateTo(String module) {
        return noClasses().that().resideOutsideOfPackage("com.storeops." + module + "..")
            .should().dependOnClassesThat().resideInAPackage("com.storeops." + module + ".repository..")
            .because("module repositories are private; cross-module reads go through the service layer");
    }

    @ArchTest static final ArchRule A1_activities_repository_private = repositoryIsPrivateTo("activities");
    @ArchTest static final ArchRule A1_programmes_repository_private = repositoryIsPrivateTo("programmes");
    @ArchTest static final ArchRule A1_staff_repository_private = repositoryIsPrivateTo("staff");
    @ArchTest static final ArchRule A1_alerts_repository_private = repositoryIsPrivateTo("alerts");
    @ArchTest static final ArchRule A1_reports_repository_private = repositoryIsPrivateTo("reports");
    @ArchTest static final ArchRule A1_no_module_cycles =
        slices().matching("com.storeops.(*)..").should().beFreeOfCycles();

    // ---- A2: error contract (failure mode 2) ----
    @ArchTest static final ArchRule A2_no_raw_exceptions_in_services_or_controllers =
        noClasses().that().resideInAnyPackage("..service..", "..controller..")
            .should().callConstructorWhere(target(owner(belongToAnyOf(RuntimeException.class, Exception.class,
                IllegalStateException.class, IllegalArgumentException.class, Error.class))))
            .because("use the AppError hierarchy");

    // ---- A3: event bus only (failure mode 4) ----
    @ArchTest static final ArchRule A3_alerts_service_not_imported_cross_module =
        noClasses().that().resideOutsideOfPackage("com.storeops.alerts..")
            .should().dependOnClassesThat().resideInAPackage("com.storeops.alerts..")
            .because("cross-module triggers go through EventBus.emit, never direct AlertService calls");
    @ArchTest static final ArchRule A3_reports_service_not_imported_cross_module =
        noClasses().that().resideOutsideOfPackage("com.storeops.reports..")
            .should().dependOnClassesThat().resideInAPackage("com.storeops.reports..")
            .because("report triggers go through EventBus.emit");

    // ---- A4: layer separation ----
    @ArchTest static final ArchRule A4_controllers_do_not_touch_repositories =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");
    @ArchTest static final ArchRule A4_repositories_have_no_http_logic =
        noClasses().that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "jakarta.servlet..");
    @ArchTest static final ArchRule A4_services_do_not_depend_on_controllers =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");
}
