package org.mendrugo.fibula.jmh.it;

import org.junit.Ignore;

@Ignore("error: Could not find option 'StackTraceInThrowable'. Use -XX:PrintFlags= to list all available options.")
public class StackTraceInThrowableTest
    extends org.openjdk.jmh.it.StackTraceInThrowableTest
    implements MultiVmRunnerFactory
{
}
