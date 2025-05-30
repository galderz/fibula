package org.mendrugo.fibula;

public enum GraalBlackhole
{
    DISABLED("")
    , PRE_JDK_21("org.graalvm.compiler.api.directives.GraalDirectives")
    , POST_JDK_21("jdk.graal.compiler.api.directives.GraalDirectives");

    private final String compilerDirectivesQName;

    GraalBlackhole(String compilerDirectivesQName)
    {
        this.compilerDirectivesQName = compilerDirectivesQName;
    }

    public String getCompilerDirectivesQName()
    {
        return compilerDirectivesQName;
    }

    public boolean isEnabled()
    {
        return !isDisabled();
    }

    public boolean isDisabled()
    {
        return this == DISABLED;
    }

    @Override
    public String toString()
    {
        switch (this)
        {
            case PRE_JDK_21:
                return "enabled (JDK 21 or earlier)";
            case POST_JDK_21:
                return "enabled (JDK 22 or later)";
           default:
               return "disabled";
        }
    }

    public static GraalBlackhole instance()
    {
        final int graalVMJavaVersion = NativeImage.getJavaVersion();
        if (0 == graalVMJavaVersion)
        {
            return DISABLED;
        }

        if (graalVMJavaVersion <= 21)
        {
            return PRE_JDK_21;
        }

        return POST_JDK_21;
    }
}
