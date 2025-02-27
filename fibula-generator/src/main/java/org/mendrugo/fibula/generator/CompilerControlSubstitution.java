package org.mendrugo.fibula.generator;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.generators.annotations.APGeneratorSource;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.GeneratorSource;
import org.openjdk.jmh.generators.core.MethodInfo;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import java.util.Collection;
import java.util.Map;

final class CompilerControlSubstitution
{
    private ProcessingEnvironment processingEnv;
    private Multimap<ClassInfo, CompilerControlMethod> compilerControls = new HashMultimap<>();

    void init(ProcessingEnvironment processingEnv)
    {
        this.processingEnv = processingEnv;
    }

    void process(RoundEnvironment roundEnv)
    {
        final GeneratorSource source = new APGeneratorSource(roundEnv, processingEnv);
        for (MethodInfo method : BenchmarkGeneratorUtils.getMethodsAnnotatedWith(source, CompilerControl.class))
        {
            final CompilerControl ann = method.getAnnotation(CompilerControl.class);
            switch (ann.value())
            {
                case DONT_INLINE:
                case INLINE:
                    compilerControls.put(method.getDeclaringClass(), new CompilerControlMethod(method, ann));
                    break;
            }
        }
    }

    public void complete(ClassOutput classOutput)
    {
        for (Map.Entry<ClassInfo, Collection<CompilerControlMethod>> entry : compilerControls.entrySet())
        {
            final ClassInfo classInfo = entry.getKey();

            final String className = String.format(
                "%s.Target_%s"
                , NativeAssetsGenerator.PACKAGE_NAME
                , classInfo.getQualifiedName().replace('.', '_')
            );

            try (final ClassCreator genClass = ClassCreator.builder()
                .classOutput(classOutput)
                .className(className)
                .setFinal(true)
                .build()
            ) {
                generateCCMethodSubstitution(entry.getValue(), classInfo, genClass);
            }
        }
    }

    private static void generateCCMethodSubstitution(
        Collection<CompilerControlMethod> ccMethods
        , ClassInfo classInfo
        , ClassCreator genClass
    ) {
        genClass.addAnnotation("com.oracle.svm.core.annotate.TargetClass")
            .add("className", classInfo.getQualifiedName());

        for (CompilerControlMethod ccMethod : ccMethods)
        {
            final String methodName = ccMethod.methodInfo.getName();
            final String returnTypeName = ccMethod.methodInfo.getReturnType();
            final String[] argumentTypeNames = ccMethod.methodInfo.getParameters()
                .stream()
                .map(paramInfo -> paramInfo.getType().getName())
                .toArray(String[]::new);

            try (MethodCreator ccMethodCreator = genClass.getMethodCreator(methodName, returnTypeName, argumentTypeNames))
            {
                ccMethodCreator.setModifiers(Opcodes.ACC_NATIVE);
                ccMethodCreator.addAnnotation("com.oracle.svm.core.annotate.AnnotateOriginal");

                final String inlineAnnotation =
                    switch (ccMethod.compilerControl.value())
                    {
                        case DONT_INLINE ->
                            "NeverInline";
                        case INLINE ->
                            "AlwaysInline";
                        default ->
                            throw new IllegalStateException("Unexpected value: " + ccMethod.compilerControl);
                    };
                final AnnotationInstance svmAnnotation = AnnotationInstance
                    .builder(DotName.createSimple("com.oracle.svm.core." + inlineAnnotation))
                    .add("value", "Because of CompilerControl." + ccMethod.compilerControl.value())
                    .build();
                ccMethodCreator.addAnnotation(svmAnnotation);
            }
        }
    }

    record CompilerControlMethod(
        MethodInfo methodInfo
        , CompilerControl compilerControl
    ) {}
}
