package org.mendrugo.fibula.extension.deployment;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.WhileLoop;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.mendrugo.fibula.results.ThroughputResult;
import org.objectweb.asm.Opcodes;

import java.util.concurrent.Callable;

public class BenchmarkGenerator
{
    static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";

    void generate(BenchmarkInfo benchmarkInfo, ClassOutput beanOutput)
    {
        final MethodInfo methodInfo = benchmarkInfo.info();
        final ClassInfo classInfo = methodInfo.declaringClass();
        System.out.println(methodInfo);
        System.out.println(classInfo);

        final ClassCreator callable = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(String.format("%s.%s_BenchmarkCallable", PACKAGE_NAME, classInfo.simpleName()))
            .interfaces(Callable.class)
            .build();

        callable.close();
    }

//    void generate(BenchmarkInfo benchmarkInfo, ClassOutput classOutput, ClassOutput beanOutput)
//    {
//        generateBenchmarkCallable(benchmarkInfo, classOutput);
//        generateBenchmarkRunner(benchmarkInfo, beanOutput);
//
////        final MethodInfo methodInfo = benchmarkInfo.info();
////        final ClassInfo classInfo = methodInfo.declaringClass();
////
////        final ClassCreator creator = ClassCreator.builder()
////            .classOutput(classOutput)
////            .className(String.format("%s.%s_Benchmark", PACKAGE_NAME, classInfo.name()))
////            .interfaces(Callable.class)
////            .build();
////
////        final FieldCreator infraField = creator.getFieldCreator("infrastructure", Infrastructure.class);
////
////        final MethodCreator constructor = creator.getMethodCreator(MethodDescriptor.INIT, void.class, Infrastructure.class);
////        // Neeeded? ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(Object.class, MethodDescriptor.INIT, void.class), ctor.getThis());
////        constructor.writeInstanceField(infraField.getFieldDescriptor(), constructor.getThis(), constructor.getMethodParam(0));
////        constructor.close();
////
////        final MethodCreator method = creator.getMethodCreator(String.format("%s_Throughput", methodInfo.name()), ThroughputResult.class, Infrastructure.class);
////        final AssignableResultHandle benchmarkInstance = method.createVariable(classInfo.simpleName());
////        // method.cre
////        // final ResultHandle newInstance = methodCreator.newInstance(MethodDescriptor.ofConstructor(classInfo));
////        final AssignableResultHandle operations = method.createVariable(double.class);
////        method.assign(operations, method.load(0));
////        // final ResultHandle operations = methodCreator.load(0);
////        final AssignableResultHandle startTime = method.createVariable(long.class);
////        method.assign(startTime, method.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class)));
////        final ResultHandle infraParam = method.getMethodParam(0);
////        final WhileLoop whileLoop = method.whileLoop(b ->
////            b.ifFalse(method.readInstanceField(FieldDescriptor.of(Infrastructure.class, "isDone", boolean.class), infraParam)));
////        final BytecodeCreator loopBlock = whileLoop.block();
////        loopBlock.invokeVirtualMethod(MethodDescriptor.of(methodInfo), benchmarkInstance);
////        // loopBlock.increment(operations);
////        loopBlock.assign(operations, loopBlock.increment(operations));
////        loopBlock.close();
////        final AssignableResultHandle stopTime = method.createVariable(long.class);
////        method.assign(stopTime, method.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class)));
////        final ResultHandle result = method.invokeStaticMethod(
////            MethodDescriptor.ofMethod(ThroughputResult.class, "of", ThroughputResult.class, String.class, double.class, long.class, long.class)
////            , method.load(methodInfo.name())
////            , operations
////            , stopTime
////            , startTime
////        );
////        method.returnValue(result);
////        method.close();
////
////        final MethodCreator call = creator.getMethodCreator("call", void.class);
////        final ResultHandle infraInstance = call.readInstanceField(infraField.getFieldDescriptor(), call.getThis());
////        call.invokeStaticMethod(method.getMethodDescriptor(), infraInstance);
////        call.close();
////
////        creator.close();
//    }

    void generateBenchmarkCallable(BenchmarkInfo benchmarkInfo, ClassOutput classOutput)
    {
        final MethodInfo methodInfo = benchmarkInfo.info();
        final ClassInfo classInfo = methodInfo.declaringClass();

        final ClassCreator callable = ClassCreator.builder()
            .classOutput(classOutput)
            .className(String.format("%s.%s_BenchmarkCallable", PACKAGE_NAME, classInfo.simpleName()))
            .interfaces(Callable.class)
            .build();

        // private final Infrastructure infrastructure;
        final FieldCreator infrastructure = callable
            .getFieldCreator("infrastructure", "org.mendrugo.fibula.runner.Infrastructure")
            .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);

        final MethodCreator ctor = callable.getMethodCreator(MethodDescriptor.INIT, "V", "org.mendrugo.fibula.runner.Infrastructure");
        ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(Object.class, MethodDescriptor.INIT, void.class), ctor.getThis());
        ctor.writeInstanceField(infrastructure.getFieldDescriptor(), ctor.getThis(), ctor.getMethodParam(0));
        ctor.returnValue(null);
        ctor.close();

        final MethodCreator callableGet = callable.getMethodCreator("call", Object.class);

        String annotatedBenchmarkClassName = classInfo.name().toString();
        String annotatedBenchmarkClassTypeDescriptor = "L" + annotatedBenchmarkClassName.replace('.', '/') + ";";
        final AssignableResultHandle annotatedBenchmark = callableGet.createVariable(annotatedBenchmarkClassTypeDescriptor);
        callableGet.assign(annotatedBenchmark, callableGet.newInstance(MethodDescriptor.ofConstructor(classInfo.name().toString())));

        // long operations = 0;
        final AssignableResultHandle operations = callableGet.createVariable(double.class);
        callableGet.assign(operations, callableGet.load((double) 0));

        // long startTime = System.nanoTime();
        final AssignableResultHandle startTime = callableGet.createVariable(long.class);
        final ResultHandle startNanoTime = callableGet.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class));
        callableGet.assign(startTime, startNanoTime);

        final WhileLoop whileLoop = callableGet.whileLoop(condition ->
            {
                final ResultHandle localInfrastructure = condition.readInstanceField(infrastructure.getFieldDescriptor(), callableGet.getThis());
                final FieldDescriptor isDoneField = FieldDescriptor.of("org.mendrugo.fibula.runner.Infrastructure", "isDone", boolean.class);
                final ResultHandle isDone = condition.readInstanceField(isDoneField, localInfrastructure);
                return condition.ifFalse(isDone);
            }
        );
        final BytecodeCreator loopBlock = whileLoop.block();
        // loopBlock.invokeStaticMethod(MethodDescriptor.of(methodInfo));
        loopBlock.invokeVirtualMethod(MethodDescriptor.of(methodInfo), annotatedBenchmark);
        // operations++;
        loopBlock.assign(operations, loopBlock.add(operations, loopBlock.load((double) 1)));
        loopBlock.close();

        // long stopTime = System.nanoTime();
        final AssignableResultHandle stopTime = callableGet.createVariable(long.class);
        final ResultHandle stopNanoTime = callableGet.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class));
        callableGet.assign(stopTime, stopNanoTime);

        final ResultHandle result = callableGet.invokeStaticMethod(
            MethodDescriptor.ofMethod(ThroughputResult.class, "of", ThroughputResult.class, String.class, double.class, long.class, long.class)
            , callableGet.load(methodInfo.name())
            , operations
            , stopTime
            , startTime
        );
        callableGet.returnValue(result);
        callableGet.close();

//        final ResultHandle result = callableGet.invokeStaticMethod(
//            MethodDescriptor.ofMethod(ThroughputResult.class, "of", ThroughputResult.class, String.class, double.class, long.class, long.class)
//            , callableGet.load(methodInfo.name())
//            , operations
//            , stopTime
//            , startTime
//        );
//        callableGet.returnValue(result);
//        callableGet.close();

        callable.close();
    }

    void generateBenchmarkRunner(BenchmarkInfo benchmarkInfo, ClassOutput classOutput)
    {
        final MethodInfo methodInfo = benchmarkInfo.info();
        final ClassInfo classInfo = methodInfo.declaringClass();
        final String benchmarkCallableName = String.format("%s.%s_BenchmarkCallable", PACKAGE_NAME, classInfo.simpleName());

        final ClassCreator runner = ClassCreator.builder()
            .classOutput(classOutput)
            .className(String.format("%s.%s_BenchmarkRunnable", PACKAGE_NAME, classInfo.simpleName()))
            .superClass("org.mendrugo.fibula.runner.AbstractBenchmarkRunner")
            .build();

        final MethodCreator doBenchmark = runner.getMethodCreator(
            "doBenchmark"
            , "org.mendrugo.fibula.results.ThroughputResult"
            , "org.mendrugo.fibula.runner.Handler"
            , "org.mendrugo.fibula.runner.Infrastructure"
        );
        final MethodDescriptor runIteration = MethodDescriptor.ofMethod(
            "org.mendrugo.fibula.runner.Handler"
            , "runIteration"
            , "org.mendrugo.fibula.results.ThroughputResult"
            , Callable.class.getName()
        );
        final ResultHandle handler = doBenchmark.getMethodParam(0);
        final ResultHandle infrastructure = doBenchmark.getMethodParam(1);
        final MethodDescriptor ctor = MethodDescriptor.ofConstructor(benchmarkCallableName, "org.mendrugo.fibula.runner.Infrastructure");
        final ResultHandle benchmarkCallable = doBenchmark.newInstance(ctor, infrastructure);
        final ResultHandle result = doBenchmark.invokeVirtualMethod(runIteration, handler, benchmarkCallable);
        doBenchmark.returnValue(result);
        doBenchmark.close();

        runner.close();
    }

//    void generateCallable(BenchmarkInfo benchmarkInfo, ClassOutput classOutput)
//    {
//        final MethodInfo methodInfo = benchmarkInfo.info();
//        final ClassInfo classInfo = methodInfo.declaringClass();
//
//        final ClassCreator creator = ClassCreator.builder()
//            .classOutput(classOutput)
//            .className(String.format("%s.%s_Callable", PACKAGE_NAME, classInfo.name()))
//            .interfaces(Callable.class)
//            .build();
//
//        final FieldCreator infrastructure = creator.getFieldCreator("infrastructure", Infrastructure.class);
//
//        final MethodCreator constructor = creator.getMethodCreator(MethodDescriptor.INIT, void.class, Infrastructure.class);
//        // Neeeded? ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(Object.class, MethodDescriptor.INIT, void.class), ctor.getThis());
//        constructor.writeInstanceField(infrastructure.getFieldDescriptor(), constructor.getThis(), constructor.getMethodParam(0));
//        constructor.close();
//
//        final MethodCreator call = creator.getMethodCreator("call", void.class);
//        call.invokeStaticMethod()
//        call.close();
//
//        creator.close();
//    }

//    void generate(BenchmarkInfo benchmarkInfo, ClassOutput classOutput)
//    {
//        final ClassCreator runner = ClassCreator.builder()
//            .classOutput(classOutput)
//            .className("org.mendrugo.fibula.gen.BenchmarkRunner")
//            .build();
//
//        runner.addAnnotation(ApplicationScoped.class);
//
//        runner
//            .getFieldCreator(
//                "resultProxy"
//                , "org.mendrugo.fibula.runner.client.ResultProxy")
//            .addAnnotation(RestClient.class);
//
//        final MethodCreator onStart = runner
//            .getMethodCreator(
//                "onStart"
//                , "void"
//                , "io.quarkus.runtime.StartupEvent"
//            );
//        onStart.getParameterAnnotations(0).addAnnotation(Observes.class);
//
//        benchmarkInfo.info().forEach(((classInfo, methodInfos) -> this.generateBenchmarkClass(classInfo, methodInfos, onStart, classOutput)));
//
//        onStart.close();
//
//        runner.close();
//    }

//    private void generateBenchmarkClass(ClassInfo classInfo, List<MethodInfo> methodInfos, MethodCreator onStart, ClassOutput classOutput)
//    {
//        final ClassCreator creator = ClassCreator.builder()
//            .classOutput(classOutput)
//            .className(String.format("%s.%s_Fibula", PACKAGE_NAME, classInfo.name()))
//            .build();
//
//        methodInfos.stream()
//            .map(methodInfo -> this.generateBenchmarkMethod(methodInfo, creator))
//            .peek(benchMethod -> onStart.invokeStaticMethod() mainBuilder.addStatement("$S()", benchMethod.name))
//            .forEach(typeBuilder::addMethod);
//
//        creator.close();
//    }

//    void generateBenchmarkMethod(MethodInfo method, ClassCreator creator)
//    {
//        final MethodCreator methodCreator = creator.getMethodCreator(String.format("%s_Throughput", method.name()), ThroughputResult.class);
//        final ResultHandle newInstance = methodCreator.newInstance(MethodDescriptor.ofConstructor(ThroughputResult.class));
//        final ResultHandle operations = methodCreator.load(0);
//        final ResultHandle startTime = methodCreator.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class));
//        methodCreator.whileLoop()
//        methodCreator.close();
//
//        return MethodSpec.methodBuilder(method.name() + "_Throughput")
//            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//            .returns(ThroughputResult.class)
//            .addStatement("$T obj = new $T()", method.declaringClass().name())
//            // .addStatement("$T resultBuilder = new $T();", TaskResultBuilder.class)
//            .addStatement("long operations = 0")
//            .addStatement("long startTime = $T.nanoTime()", System.class)
//            .beginControlFlow("do")
//            .addStatement("obj.$S()", method.name())
//            .addStatement("operations++")
//            .endControlFlow("while(!control.isDone)")
//            .addStatement("long stopTime = $T.nanoTime()", System.class)
//            .addStatement("return ThroughputResult.of($S, operations, stopTime, startTime)", method.name())
//            .build();
//    }

//    void generate(Map<ClassInfo, List<MethodInfo>> benchmarkInfo)
//    {
//        MethodSpec.Builder mainBuilder = MethodSpec.methodBuilder("main")
//            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//            .returns(void.class)
//            .addParameter(String[].class, "args");
//        // .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
//        // .build();
//
//        // benchmarkInfo.map(this::generateClass);
//        benchmarkInfo.entrySet().stream()
//            .map(this::generateBenchmarkClass)
//            .forEach(benchClass -> mainBuilder.addStatement("$S.main()", benchClass.name));
//
//        TypeSpec runner = TypeSpec.classBuilder("FibulaRunner")
//            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//            .addMethod(mainBuilder.build())
//            .build();
//
//        JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, runner).build();
//        writeToPath(javaFile);
//    }

//    private void generateBenchmarkClass(Map.Entry<ClassInfo, List<MethodInfo>> entry, ClassOutput classOutput)
//    {
//        final ClassInfo classInfo = entry.getKey();
//        final List<MethodInfo> methodInfos = entry.getValue();
//
//        final ClassCreator creator = ClassCreator.builder()
//            .classOutput(classOutput)
//            .className(String.format("%s.%s_Fibula", PACKAGE_NAME, classInfo.name()))
//            .build();
//
//        methodInfos.stream()
//            .map(this::generateBenchmarkMethod)
//            .peek(benchMethod -> mainBuilder.addStatement("$S()", benchMethod.name))
//            .forEach(typeBuilder::addMethod);
//
//
//        creator.close();
//    }

//    private TypeSpec generateBenchmarkClass(Map.Entry<ClassInfo, List<MethodInfo>> entry)
//    {
//        final ClassInfo classInfo = entry.getKey();
//        final List<MethodInfo> methodInfos = entry.getValue();
//
//        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(classInfo.simpleName() + "_Fibula")
//            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
//
//        MethodSpec.Builder mainBuilder = MethodSpec.methodBuilder("main")
//            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//            .returns(void.class);
//
//        methodInfos.stream()
//            .map(this::generateBenchmarkMethod)
//            .peek(benchMethod -> mainBuilder.addStatement("$S()", benchMethod.name))
//            .forEach(typeBuilder::addMethod);
//
//        typeBuilder.addMethod(mainBuilder.build());
//
//        final TypeSpec type = typeBuilder.build();
//        final JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, type).build();
//        writeToPath(javaFile);
//
//        return type;
//    }
//
//    private static void writeToPath(JavaFile javaFile)
//    {
//        try
//        {
//            javaFile.writeTo(Path.of("target/generated-sources/fibula"));
//        }
//        catch (IOException e)
//        {
//            throw new UncheckedIOException(e);
//        }
//    }
//
//    MethodSpec generateBenchmarkMethod(MethodInfo method)
//    {
//        return MethodSpec.methodBuilder(method.name() + "_Throughput")
//            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//            .returns(ThroughputResult.class)
//            .addStatement("$T obj = new $T()", method.declaringClass().name())
//            // .addStatement("$T resultBuilder = new $T();", TaskResultBuilder.class)
//            .addStatement("long operations = 0")
//            .addStatement("long startTime = $T.nanoTime()", System.class)
//            .beginControlFlow("do")
//            .addStatement("obj.$S()", method.name())
//            .addStatement("operations++")
//            .endControlFlow("while(!control.isDone)")
//            .addStatement("long stopTime = $T.nanoTime()", System.class)
//            .addStatement("return ThroughputResult.of($S, operations, stopTime, startTime)", method.name())
//            .build();
//    }
}
