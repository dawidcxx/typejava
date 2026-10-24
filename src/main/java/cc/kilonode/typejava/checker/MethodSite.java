package cc.kilonode.typejava.checker;

import cc.kilonode.typejava.ast.FunctionExpression;

public record MethodSite(
    ObjectLiteralInfo literal,
    ObjectTypeInfo owner,
    FieldInfo field,
    Type.Function resolvedType,
    Type.Function declaredType,
    Type.Function erasedType,
    FunctionExpression expression) {}
