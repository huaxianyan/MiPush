package one.yufz.xposed;

import java.lang.reflect.Executable;

import io.github.libxposed.api.XposedInterface;

/** Java-backed parameter object keeps reflection values as platform types for Kotlin callers. */
public final class HookParam {
    private final Executable executable;
    private Object thisObject;
    private final Object[] args;
    private Object result;
    private Throwable throwable;
    private boolean returnEarly;
    private XposedInterface.HookHandle hookHandle;

    HookParam(Executable executable, Object thisObject, Object[] args,
              XposedInterface.HookHandle hookHandle) {
        this.executable = executable;
        this.thisObject = thisObject;
        this.args = args;
        this.hookHandle = hookHandle;
    }

    public Executable getExecutable() {
        return executable;
    }

    public Executable getMethod() {
        return executable;
    }

    public Object getThisObject() {
        return thisObject;
    }

    public void setThisObject(Object thisObject) {
        this.thisObject = thisObject;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
        this.throwable = null;
        this.returnEarly = true;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
        if (throwable != null) this.returnEarly = true;
    }

    public boolean getReturnEarly() {
        return returnEarly;
    }

    public void setProceededResult(Object result) {
        this.result = result;
        this.throwable = null;
        this.returnEarly = false;
    }

    public void setProceededThrowable(Throwable throwable) {
        this.throwable = throwable;
        this.returnEarly = false;
    }

    public void unhook() {
        if (hookHandle != null) hookHandle.unhook();
    }
}
