package com.rnctech.nrdataservice.common;


public class StackTraceElementData {
    public StackTraceElementData() {}
    public StackTraceElementData(StackTraceElement element) {
        this(element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber());
    }

    public StackTraceElementData(String declaringClass, String methodName, String fileName, int lineNumber) {
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String declaringClass;
    public String methodName;
    public String fileName;
    public int lineNumber;
}
