package org.openjdk.jmh.generators.core;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import org.mendrugo.fibula.extension.deployment.JandexMethodInfo;
import org.mendrugo.fibula.extension.deployment.Reflection;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

final class JmhStateObjectHandler extends StateObjectHandler
{
    private final Reflection reflection = new Reflection(this, StateObjectHandler.class);
    private final Identifiers identifiers = new Identifiers();

    public JmhStateObjectHandler(CompilerControlPlugin compileControl)
    {
        super(compileControl);
    }

    LinkedHashSet<StateObject> stateOrder_(MethodInfo method, boolean reverse)
    {
        return reflection.invoke("stateOrder", method, MethodInfo.class, reverse, boolean.class);
    }

    Set<StateObject> stateObjects_()
    {
        return reflection.field("stateObjects", this);
    }

    void addHelperBlock(MethodInfo method, Level helperLevel, HelperType type, SequencedMap<StateObject, ResultHandle> stateHandles, ResultHandle[] params, BytecodeCreator block)
    {
        List<StateObject> statesForward = new ArrayList<>();
        for (StateObject so : stateOrder_(method, true))
        {
            for (HelperMethodInvocation hmi : so.getHelpers())
            {
                if (hmi.helperLevel == helperLevel)
                {
                    statesForward.add(so);
                    break;
                }
            }
        }

        List<StateObject> statesReverse = new ArrayList<>();
        for (StateObject so : stateOrder_(method, false))
        {
            for (HelperMethodInvocation hmi : so.getHelpers())
            {
                if (hmi.helperLevel == helperLevel)
                {
                    statesReverse.add(so);
                    break;
                }
            }
        }

        // Handle Thread object helpers
        for (StateObject so : statesForward)
        {
            if (type != HelperType.SETUP)
            {
                continue;
            }

            if (so.scope == Scope.Thread)
            {
                for (HelperMethodInvocation mi : so.getHelpers())
                {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.SETUP)
                    {
                        block.invokeVirtualMethod(((JandexMethodInfo) mi.method).getMethodDescriptor(), stateHandles.get(so), params);
                    }
                }
            }

            if (so.scope == Scope.Benchmark || so.scope == Scope.Group)
            {
                // todo add mutex
                for (HelperMethodInvocation mi : so.getHelpers())
                {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.SETUP)
                    {
                        block.invokeVirtualMethod(((JandexMethodInfo) mi.method).getMethodDescriptor(), stateHandles.get(so), params);
                    }
                }
                // todo add mutex
            }
        }

        for (StateObject so : statesReverse)
        {
            if (type != HelperType.TEARDOWN) continue;

            if (so.scope == Scope.Thread)
            {
                for (HelperMethodInvocation mi : so.getHelpers())
                {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.TEARDOWN)
                    {
                        block.invokeVirtualMethod(((JandexMethodInfo) mi.method).getMethodDescriptor(), stateHandles.get(so), params);
                    }
                }
            }

            if (so.scope == Scope.Benchmark || so.scope == Scope.Group)
            {
                // todo add mutex
                for (HelperMethodInvocation mi : so.getHelpers())
                {
                    if (mi.helperLevel == helperLevel && mi.type == HelperType.TEARDOWN)
                    {
                        block.invokeVirtualMethod(((JandexMethodInfo) mi.method).getMethodDescriptor(), stateHandles.get(so), params);
                    }
                }
                // todo add mutex
            }
        }
    }

    void addStateInitializers(ClassCreator classCreator)
    {
        Collection<StateObject> sos = cons(stateObjects_());

        List<MethodDescriptor> result = new ArrayList<>();

        for (StateObject so : sos) {
            if (so.scope != Scope.Benchmark) continue;

            // static final ReentrantLock f_lock = new ReentrantLock();
            final FieldDescriptor lockField = initStateLock(classCreator);

            // static volatile S f_state;
            final FieldDescriptor stateField = classCreator
                .getFieldCreator(so.fieldIdentifier, DescriptorUtils.extToInt(so.userType))
                .setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE)
                .getFieldDescriptor();

            final String methodName = "_jmh_tryInit_" + so.fieldIdentifier;
            try (final MethodCreator tryInit = classCreator.getMethodCreator(methodName, stateField.getType()))
            {
                // S val = f_state;
                // if (val != null) return val;
                final AssignableResultHandle val = tryInit.createVariable(stateField.getType());
                readStaticFieldAndReturnIfNotNull(val, stateField, tryInit);

                // f_lock.lock()
                final ResultHandle lock = tryInit.readStaticField(lockField);
                tryInit.invokeVirtualMethod(MethodDescriptor.ofMethod(ReentrantLock.class, "lock", void.class), lock);

                // try {
                try (final TryBlock tryBlock = tryInit.tryBlock())
                {
                    // val = f_state;
                    // if (val != null) return val;
                    readStaticFieldAndReturnIfNotNull(val, stateField, tryBlock);

                    // val = new F();
                    final ResultHandle newVal = tryBlock.newInstance(MethodDescriptor.ofConstructor(so.userType));
                    tryBlock.assign(val, newVal);

                    // val.setup(); for each trial setup methods
                    for (HelperMethodInvocation hmi : so.getHelpers()) {
                        if (hmi.helperLevel != Level.Trial) continue;
                        if (hmi.type != HelperType.SETUP) continue;
                        tryBlock.invokeVirtualMethod(((JandexMethodInfo) hmi.method).getMethodDescriptor(), val);
                    }

                    // f_state = val;
                    tryBlock.writeStaticField(stateField, val);

                    // f_lock.unlock()
                    tryBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(ReentrantLock.class, "unlock", void.class), lock);

                    // } catch (Throwable) {
                    try (final CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class))
                    {
                        // f_lock.unlock()
                        catchBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(ReentrantLock.class, "unlock", void.class), lock);
                    }
                    // }
                }

                tryInit.returnValue(val);
                result.add(tryInit.getMethodDescriptor());
            }
        }

        for (StateObject so : sos) {
            if (so.scope != Scope.Thread) continue;

            // Unshared f_unshared;
            final FieldDescriptor field = classCreator
                .getFieldCreator(so.fieldIdentifier, DescriptorUtils.extToInt(so.userType))
                .getFieldDescriptor();

            final String methodName = "_jmh_tryInit_" + so.fieldIdentifier;
            try (final MethodCreator tryInit = classCreator.getMethodCreator(methodName, field.getType()))
            {
                // Unshared val = this.f_unshared;
                final AssignableResultHandle val = tryInit.createVariable(field.getType());
                tryInit.assign(val, tryInit.readInstanceField(field, tryInit.getThis()));

                // if (val == null) {
                try (final BytecodeCreator trueBranch = tryInit.ifReferencesEqual(val, tryInit.loadNull()).trueBranch())
                {
                    // val = new Unshared(); or new Blackhole();
                    final ResultHandle instance = trueBranch.newInstance(MethodDescriptor.ofConstructor(so.userType));
                    trueBranch.assign(val, instance);
                    // val.setup(); for each trial setup methods
                    for (HelperMethodInvocation hmi : so.getHelpers()) {
                        if (hmi.helperLevel != Level.Trial) continue;
                        if (hmi.type != HelperType.SETUP) continue;
                        trueBranch.invokeVirtualMethod(((JandexMethodInfo) hmi.method).getMethodDescriptor(), val);
                    }
                    // this.f_unshared = val;
                    trueBranch.writeInstanceField(field, trueBranch.getThis(), val);
                }
                // }

                // return val
                tryInit.returnValue(val);
                result.add(tryInit.getMethodDescriptor());
            }

        }

        // todo add group state objects
    }

    SequencedMap<StateObject, ResultHandle> addStateGetters(MethodInfo method, MethodCreator methodCreator, ClassCreator classCreator)
    {
        SequencedMap<StateObject, ResultHandle> stateHandlers = new LinkedHashMap<>();
        for (StateObject so : stateOrder_(method, true))
        {
            AssignableResultHandle var = methodCreator.createVariable(DescriptorUtils.extToInt(so.userType));
            final String methodName = "_jmh_tryInit_" + so.fieldIdentifier;
            methodCreator.assign(var, methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(classCreator.getClassName(), methodName, so.userType), methodCreator.getThis()));
            stateHandlers.put(so, var);
        }
        return stateHandlers;
    }

    private FieldDescriptor initStateLock(ClassCreator classCreator)
    {
        final String lockFieldIdentifier = "f_" + identifiers.collapseTypeName(ReentrantLock.class.getName()) + identifiers.identifier(Scope.Benchmark);
        final FieldDescriptor lockField = classCreator.getFieldCreator(lockFieldIdentifier, DescriptorUtils.extToInt(ReentrantLock.class.getName()))
            .setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
            .getFieldDescriptor();

        // static {
        try (MethodCreator staticInit = classCreator.getMethodCreator("<clinit>", void.class))
        {
            staticInit.setModifiers(Opcodes.ACC_STATIC);
            ResultHandle lockInstance = staticInit.newInstance(MethodDescriptor.ofConstructor(ReentrantLock.class));
            // f_lock = new ReentrantLock();
            staticInit.writeStaticField(lockField, lockInstance);
            staticInit.returnVoid();
        }
        // }
        return lockField;
    }

    private void readStaticFieldAndReturnIfNotNull(AssignableResultHandle val, FieldDescriptor field, BytecodeCreator method)
    {
        // F val = this.f;
        method.assign(val, method.readStaticField(field));

        // if (val != null) {
        try (final BytecodeCreator trueBranch = method.ifReferencesNotEqual(val, method.loadNull()).trueBranch())
        {
            // return val
            trueBranch.returnValue(val);
        }
        // }
    }
}
