package dev.testpilot.domain;

import java.util.List;

public record MethodSignature(
        String name,
        String returnType,
        boolean isStatic,
        List<Param> params) {

    public record Param(String type, String name) {}

    public boolean returnsVoid() {
        return "void".equals(returnType);
    }
}
