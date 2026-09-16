package de.javamark.matchoracle;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;

import java.util.List;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "de.javamark.matchoracle", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT = "de.javamark.matchoracle.";
    static final List<String> FEATURES = List.of("matchday", "forecast", "review");

    /** BCE within a feature. Dependencies between different features are the business of the rule below. */
    @ArchTest
    static final ArchRule bce_layers = ignoringCrossFeatureDependencies(layeredArchitecture()
            .consideringAllDependencies()
            .layer("Boundary").definedBy("..boundary..")
            .layer("Control").definedBy("..control..")
            .layer("Entity").definedBy("..entity..")
            .whereLayer("Boundary").mayNotBeAccessedByAnyLayer()
            .whereLayer("Control").mayOnlyBeAccessedByLayers("Boundary")
            .whereLayer("Entity").mayOnlyBeAccessedByLayers("Boundary", "Control"))
            .allowEmptyShould(true);

    private static Architectures.LayeredArchitecture ignoringCrossFeatureDependencies(Architectures.LayeredArchitecture architecture) {
        for (String from : FEATURES) {
            for (String to : FEATURES) {
                if (!from.equals(to)) {
                    architecture = architecture.ignoreDependency(resideInAPackage(ROOT + from + ".."), resideInAPackage(ROOT + to + ".."));
                }
            }
        }
        return architecture;
    }

    /** Other features may only use a feature's boundary — never its control or entity classes. */
    @ArchTest
    static final ArchRule features_collaborate_only_via_boundary = classes()
            .should(new ArchCondition<JavaClass>("not depend on control or entity classes of another feature") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    String ownFeature = feature(clazz);
                    for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                        JavaClass target = dependency.getTargetClass();
                        String targetFeature = feature(target);
                        boolean internal = target.getPackageName().endsWith(".control") || target.getPackageName().endsWith(".entity");
                        if (ownFeature != null && targetFeature != null && !ownFeature.equals(targetFeature) && internal) {
                            events.add(SimpleConditionEvent.violated(clazz, dependency.getDescription()));
                        }
                    }
                }
            });

    /** The feature name is the first package below the root, e.g. "matchday" for de.javamark.matchoracle.matchday.control.Foo. */
    private static String feature(JavaClass clazz) {
        if (!clazz.getPackageName().startsWith(ROOT)) {
            return null;
        }
        String rest = clazz.getPackageName().substring(ROOT.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? null : rest.substring(0, dot);
    }

    @ArchTest
    static final ArchRule features_are_free_of_cycles = slices()
            .matching("de.javamark.matchoracle.(*)..")
            .should().beFreeOfCycles();
}
