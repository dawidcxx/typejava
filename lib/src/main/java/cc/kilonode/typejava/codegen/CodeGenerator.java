package cc.kilonode.typejava.codegen;

import static org.objectweb.asm.Opcodes.*;

import cc.kilonode.typejava.ast.AssignmentExpression;
import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.BooleanLiteral;
import cc.kilonode.typejava.ast.BreakStatement;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.ContinueStatement;
import cc.kilonode.typejava.ast.Expression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.FunctionExpression;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NullLiteral;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.ObjectLiteral;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.Statement;
import cc.kilonode.typejava.ast.StringLiteral;
import cc.kilonode.typejava.ast.UnaryExpression;
import cc.kilonode.typejava.ast.VariableDeclaration;
import cc.kilonode.typejava.ast.WhileStatement;
import cc.kilonode.typejava.checker.CallTarget;
import cc.kilonode.typejava.checker.CheckedProgram;
import cc.kilonode.typejava.checker.FieldAccess;
import cc.kilonode.typejava.checker.FieldInfo;
import cc.kilonode.typejava.checker.FunctionSymbol;
import cc.kilonode.typejava.checker.MethodSite;
import cc.kilonode.typejava.checker.ObjectLiteralInfo;
import cc.kilonode.typejava.checker.ObjectTypeInfo;
import cc.kilonode.typejava.checker.ThisSymbol;
import cc.kilonode.typejava.checker.Type;
import cc.kilonode.typejava.checker.Types;
import cc.kilonode.typejava.checker.VariableSymbol;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public final class CodeGenerator {
  private static final String RUNTIME = "cc/kilonode/typejava/runtime/";
  private static final String CONSOLE = RUNTIME + "Console";
  private static final String VALUES = RUNTIME + "Values";
  private static final String ARRAY = RUNTIME + "TjArray";
  private static final String MAP = RUNTIME + "TjMap";
  private static final AtomicLong RUN_IDS = new AtomicLong();

  private final CheckedProgram checked;
  private final String prefix;
  private final String mainInternal;
  private final Map<String, byte[]> classes = new LinkedHashMap<>();
  private final Map<ObjectTypeInfo, String> baseNames = new IdentityHashMap<>();
  private final Map<String, ObjectTypeInfo> objectInfosByName = new HashMap<>();
  private final Map<FieldInfo, String> fieldNames = new IdentityHashMap<>();
  private final Map<FieldInfo, String> methodNames = new IdentityHashMap<>();
  private final Map<VariableSymbol, String> topLevelFieldNames = new IdentityHashMap<>();
  private final Map<FunctionSymbol, String> functionNames = new IdentityHashMap<>();
  private final Map<ObjectLiteralInfo, String> literalNames = new IdentityHashMap<>();
  private int literalCounter;

  public CodeGenerator(CheckedProgram checked) {
    this.checked = checked;
    long id = RUN_IDS.incrementAndGet();
    this.prefix = "cc/kilonode/typejava/generated/TypeJava$" + id + "$";
    this.mainInternal = prefix + "Program";

    int typeIndex = 0;
    for (ObjectTypeInfo info : checked.objectTypes) {
      objectInfosByName.put(info.name(), info);
      baseNames.put(info, prefix + "Object" + typeIndex++);
      int fieldIndex = 0;
      for (FieldInfo field : info.fields()) {
        fieldNames.put(field, "field$" + fieldIndex);
        if (field.isMethod()) {
          methodNames.put(field, "method$" + fieldIndex);
        }
        fieldIndex++;
      }
    }

    int variableIndex = 0;
    for (VariableSymbol variable : checked.topLevelVariables) {
      topLevelFieldNames.put(variable, "var$" + variableIndex++);
    }
    int functionIndex = 0;
    for (FunctionSymbol function : checked.functions) {
      functionNames.put(function, "fn$" + functionIndex++);
    }
  }

  public Map<String, byte[]> generate() {
    for (ObjectTypeInfo info : checked.objectTypes) {
      generateBaseClass(info);
    }
    generateMainClass();
    return Map.copyOf(classes);
  }

  public String mainClassName() {
    return mainInternal.replace('/', '.');
  }

  private void generateBaseClass(ObjectTypeInfo info) {
    String internalName = baseNames.get(info);
    ClassWriter writer = writer();
    int access = ACC_PUBLIC | ACC_SUPER | (info.hasMethods() ? ACC_ABSTRACT : 0);
    writer.visit(V1_5, access, internalName, null, "java/lang/Object", null);

    for (FieldInfo field : info.fields()) {
      if (!field.isMethod()) {
        writer
            .visitField(ACC_PUBLIC, fieldNames.get(field), descriptor(field.type()), null, null)
            .visitEnd();
      }
    }

    for (FieldInfo field : info.fields()) {
      if (field.isMethod()) {
        writer
            .visitMethod(
                ACC_PUBLIC | ACC_ABSTRACT,
                methodNames.get(field),
                functionDescriptor((Type.Function) field.type()),
                null,
                null)
            .visitEnd();
      }
    }

    String constructorDescriptor = constructorDescriptor(info);
    MethodVisitor constructor =
        writer.visitMethod(ACC_PUBLIC, "<init>", constructorDescriptor, null, null);
    constructor.visitCode();
    constructor.visitVarInsn(ALOAD, 0);
    constructor.visitMethodInsn(
        INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    int slot = 1;
    for (FieldInfo field : info.fields()) {
      if (field.isMethod()) {
        continue;
      }
      String fieldDescriptor = descriptor(field.type());
      constructor.visitVarInsn(ALOAD, 0);
      constructor.visitVarInsn(loadOpcode(fieldDescriptor), slot);
      constructor.visitFieldInsn(
          PUTFIELD, internalName, fieldNames.get(field), fieldDescriptor);
      slot += slotSize(fieldDescriptor);
    }
    constructor.visitInsn(RETURN);
    constructor.visitMaxs(0, 0);
    constructor.visitEnd();

    writer.visitEnd();
    classes.put(internalName, writer.toByteArray());
  }

  private void generateMainClass() {
    ClassWriter writer = writer();
    writer.visit(V1_5, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, mainInternal, null, "java/lang/Object", null);

    for (VariableSymbol variable : checked.topLevelVariables) {
      writer
          .visitField(
              ACC_STATIC,
              topLevelFieldNames.get(variable),
              descriptor(variable.type()),
              null,
              null)
          .visitEnd();
    }

    for (FunctionSymbol function : checked.functions) {
      generateStaticFunction(writer, function);
    }

    MethodVisitor main =
        writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
    main.visitCode();
    MethodContext context = new MethodContext(main, "V", null);
    context.nextSlot = 1;
    for (Statement statement : checked.program.statements()) {
      if (statement instanceof FunctionDeclaration) {
        continue;
      }
      compileStatement(statement, context);
    }
    main.visitInsn(RETURN);
    main.visitMaxs(0, 0);
    main.visitEnd();

    writer.visitEnd();
    classes.put(mainInternal, writer.toByteArray());
  }

  private void generateStaticFunction(ClassWriter writer, FunctionSymbol function) {
    FunctionDeclaration declaration = function.declaration();
    String descriptor = functionDescriptor(function);
    MethodVisitor method =
        writer.visitMethod(
            ACC_PUBLIC | ACC_STATIC, functionNames.get(function), descriptor, null, null);
    method.visitCode();
    MethodContext context = new MethodContext(method, descriptor(function.returnType()), function.returnType());
    int slot = 0;
    for (int i = 0; i < declaration.parameters().size(); i++) {
      Parameter parameter = declaration.parameters().get(i);
      VariableSymbol symbol = checked.parameterSymbols.get(parameter);
      context.slots.put(symbol, slot);
      slot += slotSize(descriptor(function.parameterTypes().get(i)));
    }
    context.nextSlot = slot;
    compileBlockStatements(declaration.body(), context);
    if (Types.isVoid(function.returnType())) {
      method.visitInsn(RETURN);
    }
    method.visitMaxs(0, 0);
    method.visitEnd();
  }

  private void generateLiteralClass(ObjectLiteralInfo literal) {
    ObjectTypeInfo owner = literal.objectType();
    String internalName = literalNames.get(literal);
    ClassWriter writer = writer();
    writer.visit(V1_5, ACC_PUBLIC | ACC_SUPER, internalName, null, baseNames.get(owner), null);

    String constructorDescriptor = constructorDescriptor(owner);
    MethodVisitor constructor =
        writer.visitMethod(ACC_PUBLIC, "<init>", constructorDescriptor, null, null);
    constructor.visitCode();
    constructor.visitVarInsn(ALOAD, 0);
    int slot = 1;
    for (FieldInfo field : owner.fields()) {
      if (field.isMethod()) {
        continue;
      }
      String fieldDescriptor = descriptor(field.type());
      constructor.visitVarInsn(loadOpcode(fieldDescriptor), slot);
      slot += slotSize(fieldDescriptor);
    }
    constructor.visitMethodInsn(
        INVOKESPECIAL, baseNames.get(owner), "<init>", constructorDescriptor, false);
    constructor.visitInsn(RETURN);
    constructor.visitMaxs(0, 0);
    constructor.visitEnd();

    for (FieldInfo field : owner.fields()) {
      if (!field.isMethod()) {
        continue;
      }
      FunctionExpression expression = literal.methods().get(field.name());
      MethodSite site = checked.methods.get(expression);
      generateLiteralMethod(writer, internalName, field, site);
    }

    writer.visitEnd();
    classes.put(internalName, writer.toByteArray());
  }

  private void generateLiteralMethod(
      ClassWriter writer, String internalName, FieldInfo field, MethodSite site) {
    Type.Function erased = site.erasedType();
    Type.Function declared = site.declaredType();
    String descriptor = functionDescriptor(erased);
    MethodVisitor method =
        writer.visitMethod(ACC_PUBLIC, methodNames.get(field), descriptor, null, null);
    method.visitCode();

    int baseSlot = 1;
    for (Type parameter : erased.parameters()) {
      baseSlot += slotSize(descriptor(parameter));
    }
    MethodContext context =
        new MethodContext(method, descriptor(erased.returnType()), declared.returnType());
    context.nextSlot = baseSlot;

    int offset = declared.receiver() == null ? 0 : 1;
    int erasedSlot = 1;
    for (int i = 0; i < erased.parameters().size(); i++) {
      Type erasedParameter = erased.parameters().get(i);
      Type declaredParameter = declared.parameters().get(i);
      Parameter syntax = site.expression().parameters().get(i + offset);
      VariableSymbol symbol = checked.parameterSymbols.get(syntax);
      int semanticSlot = context.allocate(declaredParameter);
      context.slots.put(symbol, semanticSlot);

      String erasedDescriptor = descriptor(erasedParameter);
      method.visitVarInsn(loadOpcode(erasedDescriptor), erasedSlot);
      emitDescriptorToType(erasedDescriptor, declaredParameter, context);
      method.visitVarInsn(storeOpcode(descriptor(declaredParameter)), semanticSlot);
      erasedSlot += slotSize(erasedDescriptor);
    }

    compileBlockStatements(site.expression().body(), context);
    if (Types.isVoid(declared.returnType())) {
      method.visitInsn(RETURN);
    }
    method.visitMaxs(0, 0);
    method.visitEnd();
  }

  private void compileStatement(Statement statement, MethodContext context) {
    if (statement instanceof VariableDeclaration declaration) {
      compileVariable(declaration, context);
    } else if (statement instanceof BlockStatement block) {
      compileBlockStatements(block, context);
    } else if (statement instanceof IfStatement ifStatement) {
      compileIf(ifStatement, context);
    } else if (statement instanceof WhileStatement whileStatement) {
      compileWhile(whileStatement, context);
    } else if (statement instanceof BreakStatement) {
      context.method.visitJumpInsn(GOTO, context.loops.peek().breakLabel);
    } else if (statement instanceof ContinueStatement) {
      context.method.visitJumpInsn(GOTO, context.loops.peek().continueLabel);
    } else if (statement instanceof ReturnStatement returnStatement) {
      compileReturn(returnStatement, context);
    } else if (statement instanceof ExpressionStatement expressionStatement) {
      Type type = compileExpression(expressionStatement.expression(), context);
      if (!Types.isVoid(type)) {
        pop(type, context);
      }
    } else if (statement instanceof FunctionDeclaration) {
      throw new IllegalStateException("Nested function reached code generation");
    } else {
      throw new IllegalStateException("Unsupported statement " + statement.getClass());
    }
  }

  private void compileBlockStatements(BlockStatement block, MethodContext context) {
    for (Statement statement : block.statements()) {
      compileStatement(statement, context);
    }
  }

  private void compileVariable(VariableDeclaration declaration, MethodContext context) {
    VariableSymbol symbol = checked.variableDeclarations.get(declaration);
    Type actual = compileExpression(declaration.initializer(), context);
    if (symbol.topLevelField()) {
      emitCoerceToDescriptor(actual, descriptor(symbol.type()), context);
      context.method.visitFieldInsn(
          PUTSTATIC, mainInternal, topLevelFieldNames.get(symbol), descriptor(symbol.type()));
      return;
    }

    int slot = context.allocate(symbol.type());
    context.slots.put(symbol, slot);
    emitCoerce(actual, symbol.type(), context);
    context.method.visitVarInsn(storeOpcode(descriptor(symbol.type())), slot);
  }

  private void compileIf(IfStatement statement, MethodContext context) {
    Label elseLabel = new Label();
    Label end = new Label();
    compileExpression(statement.condition(), context);
    context.method.visitJumpInsn(IFEQ, elseLabel);
    compileBlockStatements(statement.thenBranch(), context);
    if (statement.elseBranch() != null) {
      context.method.visitJumpInsn(GOTO, end);
      context.method.visitLabel(elseLabel);
      if (statement.elseBranch() instanceof BlockStatement block) {
        compileBlockStatements(block, context);
      } else {
        compileStatement(statement.elseBranch(), context);
      }
      context.method.visitLabel(end);
    } else {
      context.method.visitLabel(elseLabel);
    }
  }

  private void compileWhile(WhileStatement statement, MethodContext context) {
    Label condition = new Label();
    Label end = new Label();
    context.method.visitLabel(condition);
    compileExpression(statement.condition(), context);
    context.method.visitJumpInsn(IFEQ, end);
    context.loops.push(new LoopLabels(condition, end));
    compileBlockStatements(statement.body(), context);
    context.loops.pop();
    context.method.visitJumpInsn(GOTO, condition);
    context.method.visitLabel(end);
  }

  private void compileReturn(ReturnStatement statement, MethodContext context) {
    if (statement.value() == null) {
      context.method.visitInsn(RETURN);
      return;
    }
    Type actual = compileExpression(statement.value(), context);
    emitCoerce(actual, context.declaredReturnType, context);
    emitCoerceToDescriptor(context.declaredReturnType, context.returnDescriptor, context);
    context.method.visitInsn(returnOpcode(context.returnDescriptor));
  }

  private Type compileExpression(Expression expression, MethodContext context) {
    Type type = checked.expressionTypes.get(expression);
    if (expression instanceof AssignmentExpression assignment) {
      compileAssignment(assignment, context);
    } else if (expression instanceof BinaryExpression binary) {
      compileBinary(binary, context);
    } else if (expression instanceof BooleanLiteral literal) {
      context.method.visitInsn(literal.value() ? ICONST_1 : ICONST_0);
    } else if (expression instanceof CallExpression call) {
      compileCall(call, context);
    } else if (expression instanceof Identifier identifier) {
      compileIdentifier(identifier, context, type);
    } else if (expression instanceof MemberExpression member) {
      compileMember(member, context, type);
    } else if (expression instanceof NullLiteral) {
      context.method.visitInsn(ACONST_NULL);
    } else if (expression instanceof NumericLiteral literal) {
      context.method.visitLdcInsn(Double.parseDouble(literal.lexeme()));
    } else if (expression instanceof ObjectLiteral literal) {
      compileObjectLiteral(literal, context);
    } else if (expression instanceof StringLiteral literal) {
      context.method.visitLdcInsn(literal.value());
    } else if (expression instanceof UnaryExpression unary) {
      compileUnary(unary, context);
    } else {
      throw new IllegalStateException("Unsupported expression " + expression.getClass());
    }
    return type;
  }

  private void compileIdentifier(Identifier identifier, MethodContext context, Type expected) {
    Object symbol = checked.identifiers.get(identifier);
    if (symbol instanceof ThisSymbol) {
      context.method.visitVarInsn(ALOAD, 0);
      return;
    }
    if (!(symbol instanceof VariableSymbol variable)) {
      throw new IllegalStateException("Identifier was not resolved");
    }
    String storageDescriptor = descriptor(variable.type());
    if (variable.topLevelField()) {
      context.method.visitFieldInsn(
          GETSTATIC, mainInternal, topLevelFieldNames.get(variable), storageDescriptor);
    } else {
      Integer slot = context.slots.get(variable);
      if (slot == null) {
        throw new IllegalStateException("No slot for variable " + variable.name());
      }
      context.method.visitVarInsn(loadOpcode(storageDescriptor), slot);
    }
    emitDescriptorToType(storageDescriptor, variable.type(), context);
    emitCoerce(variable.type(), expected, context);
  }

  private void compileMember(MemberExpression member, MethodContext context, Type expected) {
    FieldAccess access = checked.memberAccesses.get(member);
    Type objectType = compileExpression(member.object(), context);
    emitCoerce(objectType, access.objectType(), context);
    String fieldDescriptor = descriptor(access.field().type());
    context.method.visitFieldInsn(
        GETFIELD,
        baseNames.get(access.owner()),
        fieldNames.get(access.field()),
        fieldDescriptor);
    emitDescriptorToType(fieldDescriptor, access.storageType(), context);
    emitCoerce(access.storageType(), expected, context);
  }

  private void compileAssignment(AssignmentExpression assignment, MethodContext context) {
    if (assignment.target() instanceof Identifier identifier) {
      VariableSymbol symbol = (VariableSymbol) checked.identifiers.get(identifier);
      Type valueType = compileExpression(assignment.value(), context);
      emitCoerce(valueType, symbol.type(), context);
      String storageDescriptor = descriptor(symbol.type());
      duplicate(storageDescriptor, context);
      if (symbol.topLevelField()) {
        context.method.visitFieldInsn(
            PUTSTATIC, mainInternal, topLevelFieldNames.get(symbol), storageDescriptor);
      } else {
        Integer slot = context.slots.get(symbol);
        context.method.visitVarInsn(storeOpcode(storageDescriptor), slot);
      }
      return;
    }

    MemberExpression member = (MemberExpression) assignment.target();
    FieldAccess access = checked.memberAccesses.get(member);
    String objectDescriptor = descriptor(access.objectType());
    int objectSlot = context.allocateDescriptor(objectDescriptor);
    Type objectType = compileExpression(member.object(), context);
    emitCoerce(objectType, access.objectType(), context);
    context.method.visitVarInsn(storeOpcode(objectDescriptor), objectSlot);

    String fieldDescriptor = descriptor(access.field().type());
    int valueSlot = context.allocateDescriptor(fieldDescriptor);
    Type valueType = compileExpression(assignment.value(), context);
    emitCoerceToDescriptor(valueType, fieldDescriptor, context);
    context.method.visitVarInsn(storeOpcode(fieldDescriptor), valueSlot);

    context.method.visitVarInsn(loadOpcode(objectDescriptor), objectSlot);
    context.method.visitVarInsn(loadOpcode(fieldDescriptor), valueSlot);
    context.method.visitFieldInsn(
        PUTFIELD,
        baseNames.get(access.owner()),
        fieldNames.get(access.field()),
        fieldDescriptor);

    context.method.visitVarInsn(loadOpcode(fieldDescriptor), valueSlot);
    emitDescriptorToType(fieldDescriptor, access.storageType(), context);
  }

  private void compileBinary(BinaryExpression binary, MethodContext context) {
    String operator = binary.operator();
    if ("&&".equals(operator) || "||".equals(operator)) {
      compileLogical(binary, context);
      return;
    }
    if ("==".equals(operator) || "!=".equals(operator)) {
      compileEquality(binary, context);
      return;
    }

    if ("+".equals(operator) && Type.STRING.equals(checked.expressionTypes.get(binary))) {
      compileConcatenation(binary, context);
      return;
    }

    Type leftType = compileExpression(binary.left(), context);
    Type rightType = compileExpression(binary.right(), context);
    if ("+".equals(operator)) {
      context.method.visitInsn(DADD);
      return;
    }
    if ("-".equals(operator)) {
      context.method.visitInsn(DSUB);
      return;
    }
    if ("*".equals(operator)) {
      context.method.visitInsn(DMUL);
      return;
    }
    if ("/".equals(operator)) {
      context.method.visitInsn(DDIV);
      return;
    }
    if ("%".equals(operator)) {
      context.method.visitInsn(DREM);
      return;
    }
    if ("<".equals(operator)
        || "<=".equals(operator)
        || ">".equals(operator)
        || ">=".equals(operator)) {
      compileComparison(operator, context);
      return;
    }
    throw new IllegalStateException("Unsupported binary operator " + operator);
  }

  private void compileConcatenation(BinaryExpression binary, MethodContext context) {
    context.method.visitTypeInsn(NEW, "java/lang/StringBuilder");
    context.method.visitInsn(DUP);
    context.method.visitMethodInsn(
        INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
    Type leftType = compileExpression(binary.left(), context);
    emitToStringValue(leftType, context);
    appendString(context);
    Type rightType = compileExpression(binary.right(), context);
    emitToStringValue(rightType, context);
    appendString(context);
    context.method.visitMethodInsn(
        INVOKEVIRTUAL,
        "java/lang/StringBuilder",
        "toString",
        "()Ljava/lang/String;",
        false);
  }

  private void appendString(MethodContext context) {
    context.method.visitMethodInsn(
        INVOKEVIRTUAL,
        "java/lang/StringBuilder",
        "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
        false);
  }

  private void emitToStringValue(Type type, MethodContext context) {
    emitCoerceToDescriptor(type, "Ljava/lang/Object;", context);
    context.method.visitMethodInsn(
        INVOKESTATIC,
        VALUES,
        "toDisplayString",
        "(Ljava/lang/Object;)Ljava/lang/String;",
        false);
  }

  private void compileEquality(BinaryExpression binary, MethodContext context) {
    Type leftType = checked.expressionTypes.get(binary.left());
    Type rightType = checked.expressionTypes.get(binary.right());
    boolean equal = "==".equals(binary.operator());
    Label yes = new Label();
    Label end = new Label();

    if (Type.NUMBER.equals(leftType) && Type.NUMBER.equals(rightType)) {
      compileExpression(binary.left(), context);
      compileExpression(binary.right(), context);
      context.method.visitInsn(DCMPL);
      context.method.visitJumpInsn(equal ? IFEQ : IFNE, yes);
    } else if (Type.BOOLEAN.equals(leftType) && Type.BOOLEAN.equals(rightType)) {
      compileExpression(binary.left(), context);
      compileExpression(binary.right(), context);
      context.method.visitJumpInsn(equal ? IF_ICMPEQ : IF_ICMPNE, yes);
    } else {
      compileExpression(binary.left(), context);
      emitCoerceToDescriptor(leftType, "Ljava/lang/Object;", context);
      compileExpression(binary.right(), context);
      emitCoerceToDescriptor(rightType, "Ljava/lang/Object;", context);
      context.method.visitMethodInsn(
          INVOKESTATIC,
          "java/util/Objects",
          "equals",
          "(Ljava/lang/Object;Ljava/lang/Object;)Z",
          false);
      context.method.visitJumpInsn(equal ? IFNE : IFEQ, yes);
    }

    context.method.visitInsn(ICONST_0);
    context.method.visitJumpInsn(GOTO, end);
    context.method.visitLabel(yes);
    context.method.visitInsn(ICONST_1);
    context.method.visitLabel(end);
  }

  private void compileComparison(String operator, MethodContext context) {
    Label yes = new Label();
    Label end = new Label();
    context.method.visitInsn(">".equals(operator) || ">=".equals(operator) ? DCMPL : DCMPG);
    switch (operator) {
      case "<" -> context.method.visitJumpInsn(IFLT, yes);
      case "<=" -> context.method.visitJumpInsn(IFLE, yes);
      case ">" -> context.method.visitJumpInsn(IFGT, yes);
      case ">=" -> context.method.visitJumpInsn(IFGE, yes);
      default -> throw new IllegalStateException(operator);
    }
    context.method.visitInsn(ICONST_0);
    context.method.visitJumpInsn(GOTO, end);
    context.method.visitLabel(yes);
    context.method.visitInsn(ICONST_1);
    context.method.visitLabel(end);
  }

  private void compileLogical(BinaryExpression binary, MethodContext context) {
    boolean and = "&&".equals(binary.operator());
    Label shortcut = new Label();
    Label end = new Label();
    compileExpression(binary.left(), context);
    context.method.visitJumpInsn(and ? IFEQ : IFNE, shortcut);
    compileExpression(binary.right(), context);
    context.method.visitJumpInsn(and ? IFEQ : IFNE, shortcut);
    context.method.visitInsn(and ? ICONST_1 : ICONST_0);
    context.method.visitJumpInsn(GOTO, end);
    context.method.visitLabel(shortcut);
    context.method.visitInsn(and ? ICONST_0 : ICONST_1);
    context.method.visitLabel(end);
  }

  private void compileUnary(UnaryExpression unary, MethodContext context) {
    if ("-".equals(unary.operator())) {
      compileExpression(unary.operand(), context);
      context.method.visitInsn(DNEG);
      return;
    }
    if ("!".equals(unary.operator())) {
      Label yes = new Label();
      Label end = new Label();
      compileExpression(unary.operand(), context);
      context.method.visitJumpInsn(IFEQ, yes);
      context.method.visitInsn(ICONST_0);
      context.method.visitJumpInsn(GOTO, end);
      context.method.visitLabel(yes);
      context.method.visitInsn(ICONST_1);
      context.method.visitLabel(end);
      return;
    }
    throw new IllegalStateException("Unsupported unary operator " + unary.operator());
  }

  private void compileCall(CallExpression call, MethodContext context) {
    CallTarget target = checked.calls.get(call);
    if (target instanceof CallTarget.StaticFunction staticCall) {
      FunctionSymbol function = staticCall.symbol();
      for (int i = 0; i < call.arguments().size(); i++) {
        Type argumentType = compileExpression(call.arguments().get(i), context);
        emitCoerceToDescriptor(
            argumentType, descriptor(function.parameterTypes().get(i)), context);
      }
      String descriptor = functionDescriptor(function);
      context.method.visitMethodInsn(
          INVOKESTATIC, mainInternal, functionNames.get(function), descriptor, false);
      emitDescriptorToType(descriptor(function.returnType()), function.returnType(), context);
      return;
    }

    if (target instanceof CallTarget.ObjectMethod methodCall) {
      MemberExpression member = (MemberExpression) call.callee();
      Type receiverType = compileExpression(member.object(), context);
      emitCoerce(receiverType, methodCall.receiverType(), context);
      String descriptor = functionDescriptor(methodCall.declaredType());
      for (int i = 0; i < call.arguments().size(); i++) {
        Type argumentType = compileExpression(call.arguments().get(i), context);
        emitCoerceToDescriptor(
            argumentType,
            descriptor(methodCall.declaredType().parameters().get(i)),
            context);
      }
      context.method.visitMethodInsn(
          INVOKEVIRTUAL,
          baseNames.get(methodCall.owner()),
          methodNames.get(methodCall.field()),
          descriptor,
          false);
      emitDescriptorToType(
          descriptor(methodCall.declaredType().returnType()),
          methodCall.resolvedType().returnType(),
          context);
      return;
    }

    if (target instanceof CallTarget.ConsoleLog) {
      pushInt(call.arguments().size(), context);
      context.method.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      for (int i = 0; i < call.arguments().size(); i++) {
        context.method.visitInsn(DUP);
        pushInt(i, context);
        Type argumentType = compileExpression(call.arguments().get(i), context);
        emitCoerceToDescriptor(argumentType, "Ljava/lang/Object;", context);
        context.method.visitInsn(AASTORE);
      }
      context.method.visitMethodInsn(
          INVOKESTATIC,
          CONSOLE,
          "log",
          "([Ljava/lang/Object;)V",
          false);
      return;
    }

    if (target instanceof CallTarget.BuiltinCreate create) {
      String runtime =
          create.builtin() == CallTarget.Builtin.ARRAY ? ARRAY : MAP;
      context.method.visitTypeInsn(NEW, runtime);
      context.method.visitInsn(DUP);
      context.method.visitMethodInsn(INVOKESPECIAL, runtime, "<init>", "()V", false);
      return;
    }

    if (target instanceof CallTarget.BuiltinMethod methodCall) {
      MemberExpression member = (MemberExpression) call.callee();
      compileExpression(member.object(), context);
      String runtime = methodCall.builtin() == CallTarget.Builtin.ARRAY ? ARRAY : MAP;
      String name = methodCall.name();
      String descriptor;
      if (methodCall.builtin() == CallTarget.Builtin.ARRAY) {
        descriptor =
            switch (name) {
              case "size" -> "()I";
              case "get" -> "(I)Ljava/lang/Object;";
              case "add" -> "(Ljava/lang/Object;)V";
              default -> throw new IllegalStateException(name);
            };
        if (name.equals("get")) {
          Type argumentType = compileExpression(call.arguments().get(0), context);
          emitCoerceToDescriptor(argumentType, "I", context);
        } else if (name.equals("add")) {
          Type argumentType = compileExpression(call.arguments().get(0), context);
          emitCoerceToDescriptor(argumentType, "Ljava/lang/Object;", context);
        }
      } else {
        descriptor =
            switch (name) {
              case "size" -> "()I";
              case "get" -> "(Ljava/lang/Object;)Ljava/lang/Object;";
              case "put" -> "(Ljava/lang/Object;Ljava/lang/Object;)V";
              case "containsKey" -> "(Ljava/lang/Object;)Z";
              default -> throw new IllegalStateException(name);
            };
        if (name.equals("get") || name.equals("containsKey")) {
          Type argumentType = compileExpression(call.arguments().get(0), context);
          emitCoerceToDescriptor(argumentType, "Ljava/lang/Object;", context);
        } else if (name.equals("put")) {
          Type keyType = compileExpression(call.arguments().get(0), context);
          emitCoerceToDescriptor(keyType, "Ljava/lang/Object;", context);
          Type valueType = compileExpression(call.arguments().get(1), context);
          emitCoerceToDescriptor(valueType, "Ljava/lang/Object;", context);
        }
      }
      context.method.visitMethodInsn(INVOKEVIRTUAL, runtime, name, descriptor, false);
      emitDescriptorToType(returnDescriptor(descriptor), methodCall.returnType(), context);
      return;
    }

    throw new IllegalStateException("Unsupported call target " + target);
  }

  private void compileObjectLiteral(ObjectLiteral literal, MethodContext context) {
    ObjectLiteralInfo info = checked.objectLiterals.get(literal);
    ObjectTypeInfo owner = info.objectType();
    String className;
    if (owner.hasMethods()) {
      className = literalName(info);
    } else {
      className = baseNames.get(owner);
    }

    context.method.visitTypeInsn(NEW, className);
    context.method.visitInsn(DUP);
    for (FieldInfo field : owner.fields()) {
      if (field.isMethod()) {
        continue;
      }
      Expression value = info.values().get(field.name());
      Type valueType = compileExpression(value, context);
      emitCoerceToDescriptor(valueType, descriptor(field.type()), context);
    }
    context.method.visitMethodInsn(
        INVOKESPECIAL, className, "<init>", constructorDescriptor(owner), false);
  }

  private String literalName(ObjectLiteralInfo literal) {
    String existing = literalNames.get(literal);
    if (existing != null) {
      return existing;
    }
    String name = prefix + "Literal" + (literalCounter++);
    literalNames.put(literal, name);
    generateLiteralClass(literal);
    return name;
  }

  private String constructorDescriptor(ObjectTypeInfo info) {
    StringBuilder descriptor = new StringBuilder("(");
    for (FieldInfo field : info.fields()) {
      if (!field.isMethod()) {
        descriptor.append(descriptor(field.type()));
      }
    }
    descriptor.append(")V");
    return descriptor.toString();
  }

  private String functionDescriptor(FunctionSymbol function) {
    StringBuilder descriptor = new StringBuilder("(");
    for (Type parameter : function.parameterTypes()) {
      descriptor.append(descriptor(parameter));
    }
    descriptor.append(')').append(descriptor(function.returnType()));
    return descriptor.toString();
  }

  private String functionDescriptor(Type.Function function) {
    StringBuilder descriptor = new StringBuilder("(");
    for (Type parameter : function.parameters()) {
      descriptor.append(descriptor(parameter));
    }
    descriptor.append(')').append(descriptor(function.returnType()));
    return descriptor.toString();
  }

  private String descriptor(Type type) {
    if (type instanceof Type.Primitive primitive) {
      return switch (primitive.kind()) {
        case NUMBER -> "D";
        case STRING -> "Ljava/lang/String;";
        case BOOLEAN -> "Z";
        case VOID -> "V";
      };
    }
    if (type instanceof Type.Null) {
      return "Ljava/lang/Object;";
    }
    if (type instanceof Type.Nominal nominal) {
      return switch (nominal.name()) {
        case "TjArray" -> "L" + ARRAY + ";";
        case "TjMap" -> "L" + MAP + ";";
        default -> {
          ObjectTypeInfo info = objectInfosByName.get(nominal.name());
          if (info == null) {
            throw new IllegalStateException("No class for type " + nominal.name());
          }
          yield "L" + baseNames.get(info) + ";";
        }
      };
    }
    if (type instanceof Type.Nullable nullable) {
      Type inner = nullable.inner();
      if (Type.NUMBER.equals(inner)) {
        return "Ljava/lang/Double;";
      }
      if (Type.BOOLEAN.equals(inner)) {
        return "Ljava/lang/Boolean;";
      }
      return descriptor(inner);
    }
    if (type instanceof Type.Variable) {
      return "Ljava/lang/Object;";
    }
    if (type instanceof Type.Function) {
      throw new IllegalStateException("Function values have no JVM descriptor");
    }
    throw new IllegalStateException("Unsupported type " + type);
  }

  private void emitCoerce(Type from, Type to, MethodContext context) {
    if (from == null || to == null || from.equals(to)) {
      return;
    }
    String fromDescriptor = descriptor(from);
    String toDescriptor = descriptor(to);
    if (fromDescriptor.equals(toDescriptor)) {
      return;
    }
    emitCoerceToDescriptor(from, toDescriptor, context);
  }

  private void emitCoerceToDescriptor(Type from, String target, MethodContext context) {
    MethodVisitor method = context.method;
    if (from instanceof Type.Null) {
      return;
    }
    String source = descriptor(from);
    if (source.equals(target)) {
      return;
    }

    if (isPrimitive(target)) {
      switch (target) {
        case "D" -> {
          if (source.equals("I")) {
            method.visitInsn(I2D);
          } else if (source.equals("Ljava/lang/Object;")
              || source.equals("Ljava/lang/Double;")) {
            if (source.equals("Ljava/lang/Object;")) {
              method.visitTypeInsn(CHECKCAST, "java/lang/Double");
            }
            method.visitMethodInsn(
                INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
          } else if (!source.equals("D")) {
            throw new IllegalStateException("Cannot coerce " + source + " to D");
          }
        }
        case "Z" -> {
          if (source.equals("Ljava/lang/Object;")
              || source.equals("Ljava/lang/Boolean;")) {
            if (source.equals("Ljava/lang/Object;")) {
              method.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            }
            method.visitMethodInsn(
                INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
          } else if (!source.equals("Z")) {
            throw new IllegalStateException("Cannot coerce " + source + " to Z");
          }
        }
        case "I" -> {
          if (source.equals("D")) {
            method.visitInsn(D2I);
          } else if (source.equals("Ljava/lang/Object;")
              || source.equals("Ljava/lang/Integer;")) {
            if (source.equals("Ljava/lang/Object;")) {
              method.visitTypeInsn(CHECKCAST, "java/lang/Integer");
            }
            method.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
          } else if (!source.equals("I")) {
            throw new IllegalStateException("Cannot coerce " + source + " to I");
          }
        }
        default -> throw new IllegalStateException("Unsupported primitive descriptor " + target);
      }
      return;
    }

    if (source.equals("D")) {
      method.visitMethodInsn(
          INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
      source = "Ljava/lang/Double;";
    } else if (source.equals("Z")) {
      method.visitMethodInsn(
          INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
      source = "Ljava/lang/Boolean;";
    } else if (source.equals("I")) {
      method.visitMethodInsn(
          INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
      source = "Ljava/lang/Integer;";
    }

    if (target.equals("Ljava/lang/Object;")) {
      return;
    }
    if (source.equals("Ljava/lang/Object;") || !source.equals(target)) {
      method.visitTypeInsn(CHECKCAST, typeInternal(target));
    }
  }

  private void emitDescriptorToType(String source, Type target, MethodContext context) {
    if (source.equals("V")) {
      return;
    }
    MethodVisitor method = context.method;
    String targetDescriptor = descriptor(target);
    if (source.equals(targetDescriptor)) {
      return;
    }

    if (isPrimitive(targetDescriptor)) {
      switch (targetDescriptor) {
        case "D" -> {
          if (source.equals("I")) {
            method.visitInsn(I2D);
          } else if (source.equals("Ljava/lang/Object;")
              || source.equals("Ljava/lang/Double;")) {
            if (source.equals("Ljava/lang/Object;")) {
              method.visitTypeInsn(CHECKCAST, "java/lang/Double");
            }
            method.visitMethodInsn(
                INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
          }
        }
        case "Z" -> {
          if (source.equals("Ljava/lang/Object;")
              || source.equals("Ljava/lang/Boolean;")) {
            if (source.equals("Ljava/lang/Object;")) {
              method.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            }
            method.visitMethodInsn(
                INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
          }
        }
        case "I" -> {
          if (source.equals("D")) {
            method.visitInsn(D2I);
          }
        }
        default -> throw new IllegalStateException("Unsupported primitive descriptor " + targetDescriptor);
      }
      return;
    }

    if (source.equals("D")) {
      method.visitMethodInsn(
          INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
      source = "Ljava/lang/Double;";
    } else if (source.equals("Z")) {
      method.visitMethodInsn(
          INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
      source = "Ljava/lang/Boolean;";
    } else if (source.equals("I")) {
      method.visitMethodInsn(
          INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
      source = "Ljava/lang/Integer;";
    }

    if (targetDescriptor.equals("Ljava/lang/Object;")) {
      return;
    }
    if (source.equals("Ljava/lang/Object;") || !source.equals(targetDescriptor)) {
      method.visitTypeInsn(CHECKCAST, typeInternal(targetDescriptor));
    }
  }

  private static String typeInternal(String descriptor) {
    if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
      return descriptor.substring(1, descriptor.length() - 1);
    }
    return descriptor;
  }

  private void duplicate(String descriptor, MethodContext context) {
    context.method.visitInsn(slotSize(descriptor) == 2 ? DUP2 : DUP);
  }

  private void pop(Type type, MethodContext context) {
    context.method.visitInsn(slotSize(descriptor(type)) == 2 ? POP2 : POP);
  }

  private void pushInt(int value, MethodContext context) {
    if (value >= -1 && value <= 5) {
      context.method.visitInsn(ICONST_0 + value);
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      context.method.visitIntInsn(BIPUSH, value);
    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      context.method.visitIntInsn(SIPUSH, value);
    } else {
      context.method.visitLdcInsn(value);
    }
  }

  private static String returnDescriptor(String methodDescriptor) {
    return methodDescriptor.substring(methodDescriptor.indexOf(')') + 1);
  }

  private static boolean isPrimitive(String descriptor) {
    return descriptor.length() == 1 && "DZIVBJSCF".contains(descriptor);
  }

  private static int slotSize(String descriptor) {
    return descriptor.equals("D") || descriptor.equals("J") ? 2 : 1;
  }

  private static int loadOpcode(String descriptor) {
    return switch (descriptor.charAt(0)) {
      case 'D' -> DLOAD;
      case 'J' -> LLOAD;
      case 'F' -> FLOAD;
      case 'I', 'Z', 'S', 'B', 'C' -> ILOAD;
      default -> ALOAD;
    };
  }

  private static int storeOpcode(String descriptor) {
    return switch (descriptor.charAt(0)) {
      case 'D' -> DSTORE;
      case 'J' -> LSTORE;
      case 'F' -> FSTORE;
      case 'I', 'Z', 'S', 'B', 'C' -> ISTORE;
      default -> ASTORE;
    };
  }

  private static int returnOpcode(String descriptor) {
    return switch (descriptor.charAt(0)) {
      case 'V' -> RETURN;
      case 'D' -> DRETURN;
      case 'J' -> LRETURN;
      case 'F' -> FRETURN;
      case 'I', 'Z', 'S', 'B', 'C' -> IRETURN;
      default -> ARETURN;
    };
  }

  private static ClassWriter writer() {
    return new ClassWriter(ClassWriter.COMPUTE_MAXS);
  }

  private final class MethodContext {
    private final MethodVisitor method;
    private final String returnDescriptor;
    private final Type declaredReturnType;
    private final Map<VariableSymbol, Integer> slots = new IdentityHashMap<>();
    private final Deque<LoopLabels> loops = new ArrayDeque<>();
    private int nextSlot;

    private MethodContext(
        MethodVisitor method, String returnDescriptor, Type declaredReturnType) {
      this.method = method;
      this.returnDescriptor = returnDescriptor;
      this.declaredReturnType = declaredReturnType;
    }

    private int allocate(Type type) {
      return allocateDescriptor(CodeGenerator.this.descriptor(type));
    }

    private int allocateDescriptor(String descriptor) {
      int slot = nextSlot;
      nextSlot += slotSize(descriptor);
      return slot;
    }
  }

  private record LoopLabels(Label continueLabel, Label breakLabel) {}
}
