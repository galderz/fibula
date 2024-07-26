package org.mendrugo.fibula.results;

import org.openjdk.jmh.infra.BenchmarkParams;

public final class RunnerArguments
{
    public static final String COMMAND = "command";
    public static final String PARAMS = "params";
    public static final String SUPPLIER_NAME = "supplier-name";
    public static final String OPTIONS = "options";
    public static final String ACTION_PLAN = "action-plan";

    public static String toSupplierName(BenchmarkParams params)
    {
        return params.generatedBenchmark().replaceAll("\\.", "_") + "_Supplier";
    }

    public static String toSupplierName(Class<?> supplierClass)
    {
        return supplierClass.getName().replaceAll("\\.", "_");
    }

    public static boolean isSupplier(String supplierName, Class<?> supplierClass)
    {
        return toSupplierName(supplierClass).startsWith(supplierName);
    }
}
