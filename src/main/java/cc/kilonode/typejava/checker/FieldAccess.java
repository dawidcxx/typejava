package cc.kilonode.typejava.checker;

public record FieldAccess(
    Type objectType, ObjectTypeInfo owner, FieldInfo field, Type storageType) {}
