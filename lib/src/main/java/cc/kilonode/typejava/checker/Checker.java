package cc.kilonode.typejava.checker;

import cc.kilonode.typejava.Environment;
import cc.kilonode.typejava.HostTypes;
import cc.kilonode.typejava.NativeMethod;
import cc.kilonode.typejava.NativeType;
import cc.kilonode.typejava.RecordType;
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
import cc.kilonode.typejava.ast.FunctionType;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.NullLiteral;
import cc.kilonode.typejava.ast.NullType;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.ObjectField;
import cc.kilonode.typejava.ast.ObjectLiteral;
import cc.kilonode.typejava.ast.ObjectType;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.SourceMap;
import cc.kilonode.typejava.ast.SourceSpan;
import cc.kilonode.typejava.ast.Statement;
import cc.kilonode.typejava.ast.StringLiteral;
import cc.kilonode.typejava.ast.TypeAliasDeclaration;
import cc.kilonode.typejava.ast.TypeField;
import cc.kilonode.typejava.ast.TypeReference;
import cc.kilonode.typejava.ast.UnaryExpression;
import cc.kilonode.typejava.ast.UnionType;
import cc.kilonode.typejava.ast.VariableDeclaration;
import cc.kilonode.typejava.ast.VariableKind;
import cc.kilonode.typejava.ast.WhileStatement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Checker {
  private final Program program;
  private final SourceMap sourceMap;
  private final Environment environment;
  private final Map<String, TypeAliasInfo> aliases = new LinkedHashMap<>();
  private final Map<String, ObjectTypeInfo> objectInfos = new LinkedHashMap<>();
  private final List<ObjectTypeInfo> objectTypes = new ArrayList<>();
  private final IdentityHashMap<ObjectType, ObjectTypeInfo> inlineObjects =
      new IdentityHashMap<>();
  private final List<VariableSymbol> topLevelVariables = new ArrayList<>();
  private final Map<String, VariableSymbol> topLevelVariablesByName = new LinkedHashMap<>();
  private final List<FunctionSymbol> functions = new ArrayList<>();
  private final Map<String, FunctionSymbol> functionsByName = new LinkedHashMap<>();
  private final IdentityHashMap<Expression, Type> expressionTypes = new IdentityHashMap<>();
  private final IdentityHashMap<Identifier, Object> identifiers = new IdentityHashMap<>();
  private final IdentityHashMap<MemberExpression, FieldAccess> memberAccesses =
      new IdentityHashMap<>();
  private final IdentityHashMap<CallExpression, CallTarget> calls = new IdentityHashMap<>();
  private final IdentityHashMap<ObjectLiteral, ObjectLiteralInfo> objectLiterals =
      new IdentityHashMap<>();
  private final IdentityHashMap<FunctionExpression, MethodSite> methods =
      new IdentityHashMap<>();
  private final IdentityHashMap<VariableDeclaration, VariableSymbol> variableDeclarations =
      new IdentityHashMap<>();
  private final IdentityHashMap<Parameter, VariableSymbol> parameterSymbols =
      new IdentityHashMap<>();
  private final Deque<Scope> scopes = new ArrayDeque<>();
  private final Set<String> resolvingAliases = new LinkedHashSet<>();
  private int syntheticTypeCounter;

  private Context context;
  private FlowState flow;

  public Checker(Program program, SourceMap sourceMap, Environment environment) {
    this.program = program;
    this.sourceMap = sourceMap;
    this.environment = environment;
  }

  public CheckedProgram check() {
    registerAliases();
    validateAliasDeclarations();
    predeclareFunctions();
    predeclareTopLevelVariables();

    Scope global = new Scope();
    for (VariableSymbol variable : topLevelVariables) {
      global.variables.put(variable.name(), variable);
    }
    scopes.push(global);
    context = new Context(null, false, null);
    flow = new FlowState();

    for (Statement statement : program.statements()) {
      if (statement instanceof FunctionDeclaration function) {
        checkFunctionBody(functionsByName.get(function.name()));
      } else if (statement instanceof VariableDeclaration declaration) {
        checkTopLevelVariableDeclaration(declaration);
      } else {
        checkStatement(statement);
      }
    }

    scopes.pop();
    validateExportInterfaces();
    return new CheckedProgram(
        program,
        sourceMap,
        objectTypes,
        topLevelVariables,
        functions,
        functionsByName,
        topLevelVariablesByName,
        expressionTypes,
        identifiers,
        memberAccesses,
        calls,
        objectLiterals,
        methods,
        variableDeclarations,
        parameterSymbols);
  }

  private void registerAliases() {
    for (TypeAliasDeclaration declaration : program.typeDeclarations()) {
      if (isBuiltinTypeName(declaration.name())) {
        throw error(declaration, "Cannot redeclare built-in type '" + declaration.name() + "'");
      }
      Set<String> names = new LinkedHashSet<>();
      for (String parameter : declaration.typeParameters()) {
        if (isBuiltinTypeName(parameter)) {
          throw error(declaration, "Type parameter '" + parameter + "' uses a built-in name");
        }
        if (!names.add(parameter)) {
          throw error(declaration, "Duplicate type parameter '" + parameter + "'");
        }
      }

      TypeAliasInfo existing = aliases.get(declaration.name());
      if (existing != null) {
        boolean same =
            existing.declaration.typeParameters().equals(declaration.typeParameters())
                && equivalentType(existing.declaration.type(), declaration.type());
        if (!same) {
          throw error(
              declaration,
              "Type '"
                  + declaration.name()
                  + "' is already declared with a different definition");
        }
        continue;
      }
      aliases.put(
          declaration.name(), new TypeAliasInfo(declaration.name(), declaration));
    }
  }

  private boolean equivalentType(TypeReference first, TypeReference second) {
    if (first.equals(second)) {
      return true;
    }
    if (first instanceof ObjectType firstObject && second instanceof ObjectType secondObject) {
      if (firstObject.members().size() != secondObject.members().size()) {
        return false;
      }
      for (TypeField firstField : firstObject.members()) {
        TypeField match = null;
        for (TypeField secondField : secondObject.members()) {
          if (secondField.name().equals(firstField.name())) {
            match = secondField;
            break;
          }
        }
        if (match == null
            || firstField.readonly() != match.readonly()
            || !equivalentType(firstField.type(), match.type())) {
          return false;
        }
      }
      return true;
    }
    if (first instanceof NamedType firstName && second instanceof NamedType secondName) {
      if (!firstName.name().equals(secondName.name())
          || firstName.typeArguments().size() != secondName.typeArguments().size()) {
        return false;
      }
      for (int i = 0; i < firstName.typeArguments().size(); i++) {
        if (!equivalentType(
            firstName.typeArguments().get(i), secondName.typeArguments().get(i))) {
          return false;
        }
      }
      return true;
    }
    if (first instanceof UnionType firstUnion && second instanceof UnionType secondUnion) {
      if (firstUnion.members().size() != secondUnion.members().size()) {
        return false;
      }
      List<TypeReference> remaining = new ArrayList<>(secondUnion.members());
      for (TypeReference member : firstUnion.members()) {
        boolean found = false;
        for (int i = 0; i < remaining.size(); i++) {
          if (equivalentType(member, remaining.get(i))) {
            remaining.remove(i);
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private void validateAliasDeclarations() {
    for (TypeAliasInfo alias : aliases.values()) {
      if (alias.declaration.type() instanceof ObjectType) {
        resolveAlias(alias.name, typeVariables(alias), alias.declaration);
      }
    }
    for (TypeAliasInfo alias : aliases.values()) {
      if (!(alias.declaration.type() instanceof ObjectType)) {
        resolveAlias(alias.name, typeVariables(alias), alias.declaration);
      }
    }
  }

  private List<Type> typeVariables(TypeAliasInfo alias) {
    return alias.declaration.typeParameters().stream()
        .map(name -> (Type) new Type.Variable(name))
        .toList();
  }

  private void predeclareFunctions() {
    for (Statement statement : program.statements()) {
      if (!(statement instanceof FunctionDeclaration declaration)) {
        continue;
      }
      if (isBuiltinValueName(declaration.name())) {
        throw error(declaration, "Cannot use built-in name '" + declaration.name() + "'");
      }
      if (functionsByName.containsKey(declaration.name())) {
        throw error(declaration, "Function '" + declaration.name() + "' is already declared");
      }

      Set<String> parameterNames = new LinkedHashSet<>();
      List<Type> parameterTypes = new ArrayList<>();
      for (Parameter parameter : declaration.parameters()) {
        if ("this".equals(parameter.name())) {
          throw error(parameter, "Top-level functions cannot declare 'this'");
        }
        if (isBuiltinValueName(parameter.name())) {
          throw error(parameter, "Cannot use built-in name '" + parameter.name() + "'");
        }
        if (!parameterNames.add(parameter.name())) {
          throw error(parameter, "Duplicate parameter '" + parameter.name() + "'");
        }
        Type type = resolve(parameter.type(), Map.of());
        requireValueType(type, parameter, "parameter '" + parameter.name() + "'");
        parameterTypes.add(type);
      }

      Type returnType = resolve(declaration.returnType(), Map.of());
      requireReturnType(returnType, declaration.returnType());

      FunctionSymbol symbol =
          new FunctionSymbol(declaration.name(), parameterTypes, returnType, declaration);
      if (declaration.exported()) {
        validateExportedFunction(symbol);
      }
      functions.add(symbol);
      functionsByName.put(symbol.name(), symbol);
    }
  }

  private void validateExportedFunction(FunctionSymbol symbol) {
    for (int i = 0; i < symbol.parameterTypes().size(); i++) {
      Type type = symbol.parameterTypes().get(i);
      if (!HostTypes.isHostBound(type, environment)) {
        throw error(
            symbol.declaration().parameters().get(i),
            "Exported parameter '"
                + symbol.declaration().parameters().get(i).name()
                + "' has type "
                + Types.display(type)
                + ", which cannot cross an export boundary; bind a record or use a native type");
      }
    }
    Type returnType = symbol.returnType();
    if (!Types.isVoid(returnType) && !HostTypes.isHostBound(returnType, environment)) {
      throw error(
          symbol.declaration().returnType(),
          "Exported return type "
              + Types.display(returnType)
              + " cannot cross an export boundary; bind a record or use a native type");
    }
  }

  private void validateExportInterfaces() {
    for (Class<?> api : environment.exportInterfaces()) {
      for (java.lang.reflect.Method method : api.getMethods()) {
        if (method.getDeclaringClass() == Object.class
            || java.lang.reflect.Modifier.isStatic(method.getModifiers())
            || method.isDefault()) {
          continue;
        }
        FunctionSymbol symbol = functionsByName.get(method.getName());
        if (symbol == null || !symbol.declaration().exported()) {
          throw error(
              program,
              "Export contract "
                  + api.getName()
                  + "."
                  + method.getName()
                  + " has no matching exported function");
        }
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length != symbol.parameterTypes().size()) {
          throw error(
              symbol.declaration(),
              "Exported function '"
                  + symbol.name()
                  + "' expects "
                  + symbol.parameterTypes().size()
                  + " parameter(s), but "
                  + api.getName()
                  + "."
                  + method.getName()
                  + " expects "
                  + parameters.length);
        }
        for (int i = 0; i < parameters.length; i++) {
          Class<?> actual = HostTypes.hostClass(symbol.parameterTypes().get(i), environment);
          if (!parameters[i].equals(actual)) {
            throw error(
                symbol.declaration().parameters().get(i),
                "Exported parameter '"
                    + symbol.declaration().parameters().get(i).name()
                    + "' maps to "
                    + actual.getName()
                    + ", but "
                    + api.getName()
                    + "."
                    + method.getName()
                    + " expects "
                    + parameters[i].getName());
          }
        }
        Class<?> actualReturn = HostTypes.hostClass(symbol.returnType(), environment);
        if (!method.getReturnType().equals(actualReturn)) {
          throw error(
              symbol.declaration().returnType(),
              "Exported return type maps to "
                  + actualReturn.getName()
                  + ", but "
                  + api.getName()
                  + "."
                  + method.getName()
                  + " returns "
                  + method.getReturnType().getName());
        }
      }
    }
  }

  private void predeclareTopLevelVariables() {
    for (Statement statement : program.statements()) {
      if (!(statement instanceof VariableDeclaration declaration)) {
        continue;
      }
      if (isBuiltinValueName(declaration.name())) {
        throw error(declaration, "Cannot use built-in name '" + declaration.name() + "'");
      }
      if (topLevelVariablesByName.containsKey(declaration.name())) {
        throw error(declaration, "Variable '" + declaration.name() + "' is already declared");
      }
      if (functionsByName.containsKey(declaration.name())) {
        throw error(
            declaration,
            "Variable '"
                + declaration.name()
                + "' conflicts with a function of the same name");
      }

      Type type = resolve(declaration.type(), Map.of());
      requireValueType(type, declaration.type(), "variable '" + declaration.name() + "'");
      VariableSymbol symbol =
          new VariableSymbol(
              declaration.name(),
              type,
              declaration.kind() == VariableKind.LET,
              true);
      topLevelVariables.add(symbol);
      topLevelVariablesByName.put(symbol.name(), symbol);
      variableDeclarations.put(declaration, symbol);
    }
  }

  private void checkFunctionBody(FunctionSymbol symbol) {
    if (symbol == null) {
      return;
    }
    FunctionDeclaration declaration = symbol.declaration();
    scopes.push(new Scope());
    Set<String> names = new LinkedHashSet<>();
    for (int i = 0; i < declaration.parameters().size(); i++) {
      Parameter parameter = declaration.parameters().get(i);
      if (!names.add(parameter.name())) {
        throw error(parameter, "Duplicate parameter '" + parameter.name() + "'");
      }
      VariableSymbol parameterSymbol =
          new VariableSymbol(parameter.name(), symbol.parameterTypes().get(i), true, false);
      declare(parameterSymbol, parameter);
      parameterSymbols.put(parameter, parameterSymbol);
    }

    Context previousContext = context;
    FlowState previousFlow = flow;
    context = new Context(symbol.returnType(), false, null);
    flow = new FlowState();
    checkBlockStatements(declaration.body());
    if (!Types.isVoid(symbol.returnType()) && !guaranteesReturn(declaration.body())) {
      throw error(declaration, "Not all code paths return a value");
    }
    flow = previousFlow;
    context = previousContext;
    scopes.pop();
  }

  private void checkTopLevelVariableDeclaration(VariableDeclaration declaration) {
    VariableSymbol symbol = variableDeclarations.get(declaration);
    checkExpression(declaration.initializer(), symbol.type());
    flow.invalidate(new AccessKey(symbol, List.of()));
  }

  private void checkStatement(Statement statement) {
    if (statement instanceof VariableDeclaration declaration) {
      checkLocalVariable(declaration);
    } else if (statement instanceof BlockStatement block) {
      checkBlock(block);
    } else if (statement instanceof IfStatement ifStatement) {
      checkIf(ifStatement);
    } else if (statement instanceof WhileStatement whileStatement) {
      checkWhile(whileStatement);
    } else if (statement instanceof BreakStatement) {
      if (!context.inLoop) {
        throw error(statement, "'break' is only allowed inside a loop");
      }
    } else if (statement instanceof ContinueStatement) {
      if (!context.inLoop) {
        throw error(statement, "'continue' is only allowed inside a loop");
      }
    } else if (statement instanceof ReturnStatement returnStatement) {
      checkReturn(returnStatement);
    } else if (statement instanceof ExpressionStatement expressionStatement) {
      checkExpression(expressionStatement.expression(), null);
    } else if (statement instanceof FunctionDeclaration declaration) {
      throw error(
          declaration,
          "Functions may only be declared at the top level; nested functions are not supported");
    } else {
      throw error(statement, "Unsupported statement");
    }
  }

  private void checkLocalVariable(VariableDeclaration declaration) {
    if (isBuiltinValueName(declaration.name())) {
      throw error(declaration, "Cannot use built-in name '" + declaration.name() + "'");
    }
    Type type = resolve(declaration.type(), Map.of());
    requireValueType(type, declaration.type(), "variable '" + declaration.name() + "'");
    if (isDeclaredInCurrentScope(declaration.name())) {
      throw error(declaration, "Variable '" + declaration.name() + "' is already declared");
    }
    VariableSymbol symbol =
        new VariableSymbol(
            declaration.name(), type, declaration.kind() == VariableKind.LET, false);
    variableDeclarations.put(declaration, symbol);
    declare(symbol, declaration);
    checkExpression(declaration.initializer(), type);
    flow.invalidate(new AccessKey(symbol, List.of()));
  }

  private void checkBlock(BlockStatement block) {
    scopes.push(new Scope());
    checkBlockStatements(block);
    scopes.pop();
  }

  private void checkBlockStatements(BlockStatement block) {
    for (Statement statement : block.statements()) {
      checkStatement(statement);
    }
  }

  private void checkIf(IfStatement statement) {
    Type conditionType = checkExpression(statement.condition(), Type.BOOLEAN);
    if (!Type.BOOLEAN.equals(conditionType)) {
      throw error(statement.condition(), "Condition must be boolean");
    }

    FlowState base = flow.copy();
    Refinement refinement = refinement(statement.condition());
    FlowState trueFlow = base.copy();
    FlowState falseFlow = base.copy();
    applyRefinement(trueFlow, refinement, true);
    applyRefinement(falseFlow, refinement, false);

    FlowState thenFlow;
    flow = trueFlow;
    scopes.push(new Scope());
    checkBlockStatements(statement.thenBranch());
    thenFlow = flow;
    scopes.pop();
    boolean thenExits = alwaysExits(statement.thenBranch());

    FlowState elseFlow = null;
    boolean elseExits = false;
    if (statement.elseBranch() != null) {
      flow = falseFlow;
      if (statement.elseBranch() instanceof BlockStatement block) {
        checkBlock(block);
      } else {
        checkStatement(statement.elseBranch());
      }
      elseFlow = flow;
      elseExits = alwaysExits(statement.elseBranch());
    }

    if (elseFlow == null) {
      flow = thenExits ? falseFlow : base;
    } else if (thenExits && elseExits) {
      flow = base;
    } else if (thenExits) {
      flow = elseFlow;
    } else if (elseExits) {
      flow = thenFlow;
    } else {
      flow = FlowState.intersection(thenFlow, elseFlow);
    }
  }

  private void checkWhile(WhileStatement statement) {
    checkExpression(statement.condition(), Type.BOOLEAN);
    FlowState base = flow.copy();
    Refinement refinement = refinement(statement.condition());
    FlowState bodyFlow = base.copy();
    applyRefinement(bodyFlow, refinement, true);

    Context previousContext = context;
    flow = bodyFlow;
    context = new Context(context.returnType, true, context.thisSymbol);
    checkBlock(statement.body());
    context = previousContext;

    flow = base.copy();
    applyRefinement(flow, refinement, false);
  }

  private void checkReturn(ReturnStatement statement) {
    if (context.returnType == null) {
      throw error(statement, "'return' is not allowed at the top level");
    }
    if (statement.value() == null) {
      if (!Types.isVoid(context.returnType)) {
        throw error(statement, "A non-void function must return a value");
      }
      return;
    }
    if (Types.isVoid(context.returnType)) {
      throw error(statement, "A void function cannot return a value");
    }
    checkExpression(statement.value(), context.returnType);
  }

  private Type checkExpression(Expression expression, Type expected) {
    Type actual;
    if (expression instanceof AssignmentExpression assignment) {
      actual = checkAssignment(assignment);
    } else if (expression instanceof BinaryExpression binary) {
      actual = checkBinary(binary);
    } else if (expression instanceof BooleanLiteral) {
      actual = Type.BOOLEAN;
    } else if (expression instanceof CallExpression call) {
      actual = checkCall(call, expected);
    } else if (expression instanceof FunctionExpression functionExpression) {
      throw error(
          functionExpression,
          "Function expressions are only allowed as object literal method implementations");
    } else if (expression instanceof Identifier identifier) {
      actual = checkIdentifier(identifier);
    } else if (expression instanceof MemberExpression member) {
      actual = checkMember(member);
    } else if (expression instanceof NullLiteral) {
      actual = Type.NULL;
    } else if (expression instanceof NumericLiteral) {
      actual = Type.NUMBER;
    } else if (expression instanceof ObjectLiteral objectLiteral) {
      actual = checkObjectLiteral(objectLiteral, expected);
    } else if (expression instanceof StringLiteral) {
      actual = Type.STRING;
    } else if (expression instanceof UnaryExpression unary) {
      actual = checkUnary(unary);
    } else {
      throw error(expression, "Unsupported expression");
    }

    expressionTypes.put(expression, actual);
    if (expected != null && !Types.isAssignable(expected, actual)) {
      throw error(
          expression,
          "Expected "
              + Types.display(expected)
              + " but found "
              + Types.display(actual));
    }
    return actual;
  }

  private Type checkIdentifier(Identifier identifier) {
    if ("this".equals(identifier.name())) {
      if (context.thisSymbol == null) {
        throw error(identifier, "'this' is only available inside object methods");
      }
      identifiers.put(identifier, context.thisSymbol);
      return context.thisSymbol.type();
    }

    VariableSymbol symbol = findVariable(identifier.name());
    if (symbol == null) {
      if (functionsByName.containsKey(identifier.name())) {
        throw error(
            identifier,
            "Function '"
                + identifier.name()
                + "' is not a value; call it directly");
      }
      throw error(identifier, "Unknown variable '" + identifier.name() + "'");
    }
    identifiers.put(identifier, symbol);
    return refinedType(symbol.type(), new AccessKey(symbol, List.of()));
  }

  private Type checkMember(MemberExpression member) {
    Type objectType = checkExpression(member.object(), null);
    ObjectTypeInfo owner = objectTypeInfo(objectType, member.object());
    FieldInfo field = findField(owner, member.property(), member);
    if (field.isMethod()) {
      throw error(
          member,
          "Method '"
              + member.property()
              + "' must be called directly; function values are not supported");
    }

    Type storageType = owner.fieldType(field, nominalArguments(objectType, member.object()));
    AccessKey key = keyOf(member);
    Type actual = refinedType(storageType, key);
    memberAccesses.put(member, new FieldAccess(objectType, owner, field, storageType));
    return actual;
  }

  private Type checkAssignment(AssignmentExpression assignment) {
    if (assignment.target() instanceof Identifier identifier) {
      if ("this".equals(identifier.name())) {
        throw error(assignment, "Cannot assign to 'this'");
      }
      VariableSymbol symbol = findVariable(identifier.name());
      if (symbol == null) {
        throw error(identifier, "Unknown variable '" + identifier.name() + "'");
      }
      if (!symbol.mutable()) {
        throw error(assignment, "Cannot assign to constant '" + symbol.name() + "'");
      }
      identifiers.put(identifier, symbol);
      checkExpression(assignment.value(), symbol.type());
      flow.invalidate(new AccessKey(symbol, List.of()));
      return symbol.type();
    }

    if (assignment.target() instanceof MemberExpression member) {
      Type objectType = checkExpression(member.object(), null);
      ObjectTypeInfo owner = objectTypeInfo(objectType, member.object());
      FieldInfo field = findField(owner, member.property(), member);
      if (field.isMethod()) {
        throw error(member, "Cannot assign to method '" + member.property() + "'");
      }
      if (field.readonly()) {
        throw error(member, "Cannot assign to readonly field '" + member.property() + "'");
      }
      Type storageType = owner.fieldType(field, nominalArguments(objectType, member.object()));
      checkExpression(assignment.value(), storageType);
      memberAccesses.put(member, new FieldAccess(objectType, owner, field, storageType));
      flow.invalidate(keyOf(member));
      return storageType;
    }

    throw error(assignment, "Invalid assignment target");
  }

  private Type checkBinary(BinaryExpression binary) {
    String operator = binary.operator();
    if ("&&".equals(operator) || "||".equals(operator)) {
      checkExpression(binary.left(), Type.BOOLEAN);
      checkExpression(binary.right(), Type.BOOLEAN);
      return Type.BOOLEAN;
    }
    if ("==".equals(operator) || "!=".equals(operator)) {
      Type left = checkExpression(binary.left(), null);
      Type right = checkExpression(binary.right(), null);
      checkEqualityOperands(left, right, binary);
      return Type.BOOLEAN;
    }

    Type left = checkExpression(binary.left(), null);
    Type right = checkExpression(binary.right(), null);

    if ("+".equals(operator)) {
      if (Type.NUMBER.equals(left) && Type.NUMBER.equals(right)) {
        return Type.NUMBER;
      }
      if (Type.STRING.equals(left) || Type.STRING.equals(right)) {
        if (!isConcatenable(left) || !isConcatenable(right)) {
          throw error(binary, "Cannot concatenate " + Types.display(left) + " and " + Types.display(right));
        }
        return Type.STRING;
      }
      throw error(binary, "Operator '+' requires two numbers or a string operand");
    }

    if ("-".equals(operator)
        || "*".equals(operator)
        || "/".equals(operator)
        || "%".equals(operator)) {
      requireNumber(left, binary.left(), operator);
      requireNumber(right, binary.right(), operator);
      return Type.NUMBER;
    }

    if ("<".equals(operator)
        || "<=".equals(operator)
        || ">".equals(operator)
        || ">=".equals(operator)) {
      requireNumber(left, binary.left(), operator);
      requireNumber(right, binary.right(), operator);
      return Type.BOOLEAN;
    }

    throw error(binary, "Unsupported operator '" + operator + "'");
  }

  private Type checkUnary(UnaryExpression unary) {
    if ("!".equals(unary.operator())) {
      checkExpression(unary.operand(), Type.BOOLEAN);
      return Type.BOOLEAN;
    }
    if ("-".equals(unary.operator())) {
      checkExpression(unary.operand(), Type.NUMBER);
      return Type.NUMBER;
    }
    throw error(unary, "Unsupported unary operator '" + unary.operator() + "'");
  }

  private Type checkCall(CallExpression call, Type expected) {
    CallTarget target;
    Type returned;

    if (call.callee() instanceof Identifier identifier) {
      FunctionSymbol function = functionsByName.get(identifier.name());
      if (function == null) {
        if (findVariable(identifier.name()) != null) {
          throw error(
              call,
              "Only named functions and methods can be called; function values are not supported");
        }
        throw error(call, "Unknown function '" + identifier.name() + "'");
      }
      checkArguments(call.arguments(), function.parameterTypes(), function.declaration());
      target = new CallTarget.StaticFunction(function);
      returned = function.returnType();
      flow.invalidateMembers();
    } else if (call.callee() instanceof MemberExpression member) {
      if (isBuiltinTypeReference(member.object())) {
        CallTarget.NativeCall nativeCall = checkNativeStaticCall(call, member, expected);
        target = nativeCall;
        returned = nativeCall.returnType();
      } else if (isConsoleLog(member)) {
        if (!call.arguments().isEmpty()) {
          for (Expression argument : call.arguments()) {
            Type type = checkExpression(argument, null);
            if (Types.isVoid(type)) {
              throw error(argument, "Cannot pass a void expression to console.log");
            }
          }
        }
        target = new CallTarget.ConsoleLog();
        returned = Type.VOID;
      } else {
        Type receiverType = checkExpression(member.object(), null);
        target = checkInstanceCall(call, member, receiverType);
        if (target instanceof CallTarget.ObjectMethod method) {
          returned = method.resolvedType().returnType();
        } else if (target instanceof CallTarget.NativeCall method) {
          returned = method.returnType();
        } else {
          throw new IllegalStateException("Unexpected call target");
        }
        flow.invalidateMembers();
      }
    } else {
      throw error(
          call,
          "Only named functions and methods can be called; function values are not supported");
    }

    calls.put(call, target);
    return returned;
  }

  private CallTarget.NativeCall checkNativeStaticCall(
      CallExpression call, MemberExpression member, Type expected) {
    String typeName = ((Identifier) member.object()).name();
    NativeType type = environment.findType(typeName).orElseThrow();
    NativeMethod method = type.method(member.property());
    if (method == null || !method.isStatic()) {
      throw error(member, "Unknown static method '" + member.property() + "' on " + typeName);
    }

    Map<String, Type> substitutions = new LinkedHashMap<>();
    if (method.isFactory()) {
      if (!call.arguments().isEmpty()) {
        throw error(call, typeName + "." + method.name() + "() does not take arguments");
      }
      Type inferred = expected == null ? null : Types.stripNullable(expected);
      if (!(inferred instanceof Type.Nominal nominal) || !nominal.name().equals(typeName)) {
        throw error(
            call,
            "Cannot infer the type of "
                + typeName
                + "."
                + method.name()
                + "(); annotate the destination as "
                + signature(type));
      }
      requireArity(call, nominal, type);
      for (int i = 0; i < type.parameterNames().size(); i++) {
        substitutions.put(type.parameterNames().get(i), nominal.typeArguments().get(i));
      }
    }

    List<Type> parameterTypes = substituteAll(method.parameterTypes(), substitutions);
    checkArguments(call.arguments(), parameterTypes, member);
    Type returnType = Types.substitute(method.returnType(), substitutions);
    return new CallTarget.NativeCall(type, method, null, returnType);
  }

  private void requireArity(Object node, Type.Nominal nominal, NativeType type) {
    int arity = type.parameterNames().size();
    if (nominal.typeArguments().size() != arity) {
      throw error(
          node,
          type.name()
              + " requires "
              + arity
              + " type argument"
              + (arity == 1 ? "" : "s"));
    }
  }

  private String signature(NativeType type) {
    if (type.parameterNames().isEmpty()) {
      return type.name();
    }
    return type.name() + "<" + String.join(", ", type.parameterNames()) + ">";
  }

  private static List<Type> substituteAll(
      List<Type> types, Map<String, Type> substitutions) {
    return types.stream().map(type -> Types.substitute(type, substitutions)).toList();
  }

  private CallTarget checkInstanceCall(
      CallExpression call, MemberExpression member, Type receiverType) {
    if (receiverType instanceof Type.Nullable) {
      throw error(
          member.object(),
          "Cannot call a method on a nullable value; check for null first");
    }
    if (receiverType instanceof Type.Nominal nominal) {
      NativeType nativeType = environment.findType(nominal.name()).orElse(null);
      if (nativeType != null) {
        return checkNativeMethod(call, member, nominal, nativeType);
      }
      ObjectTypeInfo owner = objectInfos.get(nominal.name());
      if (owner == null) {
        throw error(member.object(), "Unknown object type '" + nominal.name() + "'");
      }
      FieldInfo field = findField(owner, member.property(), member);
      if (!(field.type() instanceof Type.Function declaredFunction)) {
        throw error(member, "'" + member.property() + "' is not callable");
      }
      Type.Function resolved =
          (Type.Function) owner.fieldType(field, nominal.typeArguments());
      checkArguments(call.arguments(), resolved.parameters(), member);
      return new CallTarget.ObjectMethod(
          receiverType, owner, field, declaredFunction, resolved);
    }
    throw error(
        member.object(),
        "Cannot call a method on a value of type " + Types.display(receiverType));
  }

  private CallTarget.NativeCall checkNativeMethod(
      CallExpression call,
      MemberExpression member,
      Type.Nominal receiver,
      NativeType type) {
    NativeMethod method = type.method(member.property());
    if (method == null || method.isStatic()) {
      throw error(member, "Unknown method '" + member.property() + "' on " + type.name());
    }

    Map<String, Type> substitutions = new LinkedHashMap<>();
    for (int i = 0; i < type.parameterNames().size(); i++) {
      substitutions.put(type.parameterNames().get(i), receiver.typeArguments().get(i));
    }
    List<Type> parameterTypes = substituteAll(method.parameterTypes(), substitutions);
    checkArguments(call.arguments(), parameterTypes, member);
    Type returnType = Types.substitute(method.returnType(), substitutions);
    return new CallTarget.NativeCall(type, method, receiver, returnType);
  }

  private void checkArguments(List<Expression> arguments, List<Type> parameterTypes, Object node) {
    if (arguments.size() != parameterTypes.size()) {
      throw error(
          node,
          "Expected "
              + parameterTypes.size()
              + " argument"
              + (parameterTypes.size() == 1 ? "" : "s")
              + " but found "
              + arguments.size());
    }
    for (int i = 0; i < arguments.size(); i++) {
      checkExpression(arguments.get(i), parameterTypes.get(i));
    }
  }

  private Type checkObjectLiteral(ObjectLiteral literal, Type expected) {
    if (expected == null) {
      throw error(
          literal,
          "Object literals require a nominal contextual type, for example a variable annotation");
    }
    Type target = Types.stripNullable(expected);
    if (!(target instanceof Type.Nominal nominal)) {
      throw error(
          literal,
          "Object literals require a nominal object type, not " + Types.display(target));
    }
    ObjectTypeInfo owner = objectInfos.get(nominal.name());
    if (owner == null) {
      throw error(
          literal,
          "Object literals require a nominal object type, not " + Types.display(target));
    }

    ObjectLiteralInfo info = new ObjectLiteralInfo(nominal, owner, nominal.typeArguments());
    Map<String, ObjectField> provided = new LinkedHashMap<>();
    for (ObjectField field : literal.fields()) {
      if (provided.put(field.name(), field) != null) {
        throw error(field, "Duplicate object literal field '" + field.name() + "'");
      }
    }

    for (FieldInfo field : owner.fields()) {
      ObjectField value = provided.remove(field.name());
      if (value == null) {
        throw error(literal, "Missing field '" + field.name() + "' for " + nominal.name());
      }
      Type fieldType = owner.fieldType(field, nominal.typeArguments());
      if (field.isMethod()) {
        if (!(value.value() instanceof FunctionExpression function)) {
          throw error(
              value,
              "Field '"
                  + field.name()
                  + "' is a function type and must use method syntax");
        }
        checkMethod(function, info, field, nominal, (Type.Function) fieldType);
        info.methods().put(field.name(), function);
      } else {
        if (value.value() instanceof FunctionExpression) {
          throw error(
              value,
              "Field '" + field.name() + "' is not a function type and cannot use method syntax");
        }
        checkExpression(value.value(), fieldType);
        info.values().put(field.name(), value.value());
      }
    }

    if (!provided.isEmpty()) {
      ObjectField extra = provided.values().iterator().next();
      throw error(extra, "Unknown field '" + extra.name() + "' for " + nominal.name());
    }

    objectLiterals.put(literal, info);
    return nominal;
  }

  private void checkMethod(
      FunctionExpression expression,
      ObjectLiteralInfo literal,
      FieldInfo field,
      Type.Nominal nominal,
      Type.Function expected) {
    List<Parameter> parameters = expression.parameters();
    int offset = 0;
    Type receiver = null;
    if (!parameters.isEmpty() && "this".equals(parameters.get(0).name())) {
      Parameter thisParameter = parameters.get(0);
      receiver = resolve(thisParameter.type(), Map.of());
      if (!(receiver instanceof Type.Nominal)) {
        throw error(thisParameter, "The 'this' parameter must have an object type");
      }
      if (expected.receiver() == null) {
        throw error(
            thisParameter,
            "This method's type does not declare 'this', so the implementation must not either");
      }
      if (!receiver.equals(nominal)) {
        throw error(
            thisParameter,
            "The 'this' type must be " + Types.display(nominal));
      }
      offset = 1;
    }

    if (expected.receiver() != null && receiver == null) {
      throw error(
          expression,
          "Method for '" + field.name() + "' must declare this: " + Types.display(expected.receiver()));
    }

    List<Type> declaredParameters = new ArrayList<>();
    Set<String> names = new LinkedHashSet<>();
    for (int i = offset; i < parameters.size(); i++) {
      Parameter parameter = parameters.get(i);
      if ("this".equals(parameter.name())) {
        throw error(parameter, "'this' must be the first parameter");
      }
      if (isBuiltinValueName(parameter.name())) {
        throw error(parameter, "Cannot use built-in name '" + parameter.name() + "'");
      }
      if (!names.add(parameter.name())) {
        throw error(parameter, "Duplicate parameter '" + parameter.name() + "'");
      }
      Type declared = resolve(parameter.type(), Map.of());
      requireValueType(declared, parameter, "parameter '" + parameter.name() + "'");
      if (i - offset >= expected.parameters().size()) {
        throw error(expression, "Too many method parameters");
      }
      Type expectedParameter = expected.parameters().get(i - offset);
      if (!Types.isAssignable(declared, expectedParameter)) {
        throw error(
            parameter,
            "Parameter type "
                + Types.display(declared)
                + " is not compatible with "
                + Types.display(expectedParameter));
      }
      declaredParameters.add(declared);
    }
    if (declaredParameters.size() != expected.parameters().size()) {
      throw error(
          expression,
          "Expected "
              + expected.parameters().size()
              + " method parameter"
              + (expected.parameters().size() == 1 ? "" : "s")
              + " but found "
              + declaredParameters.size());
    }

    Type declaredReturn = resolve(expression.returnType(), Map.of());
    requireReturnType(declaredReturn, expression.returnType());
    if (!Types.isAssignable(expected.returnType(), declaredReturn)) {
      throw error(
          expression,
          "Method returns "
              + Types.display(declaredReturn)
              + " but "
              + Types.display(expected.returnType())
              + " is required");
    }

    ThisSymbol thisSymbol = receiver == null ? null : new ThisSymbol(receiver);
    scopes.push(new Scope());
    for (int i = offset; i < parameters.size(); i++) {
      Parameter parameter = parameters.get(i);
      VariableSymbol parameterSymbol =
          new VariableSymbol(
              parameter.name(), declaredParameters.get(i - offset), true, false);
      declare(parameterSymbol, parameter);
      parameterSymbols.put(parameter, parameterSymbol);
    }

    Context previousContext = context;
    FlowState previousFlow = flow;
    context = new Context(declaredReturn, false, thisSymbol);
    flow = new FlowState();
    checkBlockStatements(expression.body());
    if (!Types.isVoid(declaredReturn) && !guaranteesReturn(expression.body())) {
      throw error(expression, "Not all code paths return a value");
    }
    flow = previousFlow;
    context = previousContext;
    scopes.pop();

    Type.Function declaredType =
        new Type.Function(receiver, declaredParameters, declaredReturn);
    methods.put(
        expression,
        new MethodSite(
            literal,
            literal.objectType(),
            field,
            expected,
            declaredType,
            (Type.Function) field.type(),
            expression));
  }

  private ObjectTypeInfo objectTypeInfo(Type type, Object node) {
    if (type instanceof Type.Nullable) {
      throw error(node, "Cannot access a member on a nullable value; check for null first");
    }
    if (type instanceof Type.Nominal nominal) {
      ObjectTypeInfo info = objectInfos.get(nominal.name());
      if (info != null) {
        return info;
      }
      if (nominal.name().equals("TjArray") || nominal.name().equals("TjMap")) {
        throw error(node, "Methods on " + nominal.name() + " must be called directly");
      }
    }
    throw error(node, "Cannot access a member on a value of type " + Types.display(type));
  }

  private FieldInfo findField(ObjectTypeInfo owner, String name, Object node) {
    for (FieldInfo field : owner.fields()) {
      if (field.name().equals(name)) {
        return field;
      }
    }
    throw error(node, "Type '" + owner.name() + "' has no field '" + name + "'");
  }

  private List<Type> nominalArguments(Type type, Object node) {
    if (type instanceof Type.Nominal nominal) {
      return nominal.typeArguments();
    }
    throw error(node, "Expected a nominal object type");
  }

  private Type refinedType(Type type, AccessKey key) {
    if (type instanceof Type.Nullable nullable && key != null) {
      Type refinement = flow.get(key);
      if (refinement != null) {
        return refinement;
      }
    }
    return type;
  }

  private AccessKey keyOf(Expression expression) {
    if (expression instanceof Identifier identifier) {
      Object symbol = identifiers.get(identifier);
      if (symbol instanceof ThisSymbol thisSymbol) {
        return new AccessKey(thisSymbol.identity(), List.of());
      }
      if (symbol instanceof VariableSymbol variable) {
        return new AccessKey(variable, List.of());
      }
      return null;
    }
    if (expression instanceof MemberExpression member) {
      AccessKey base = keyOf(member.object());
      if (base == null) {
        return null;
      }
      return base.child(member.property());
    }
    return null;
  }

  private Refinement refinement(Expression condition) {
    if (condition instanceof UnaryExpression unary && "!".equals(unary.operator())) {
      Refinement inner = refinement(unary.operand());
      return inner.negated();
    }
    if (condition instanceof BinaryExpression binary
        && ("==".equals(binary.operator()) || "!=".equals(binary.operator()))) {
      Expression other = null;
      if (binary.left() instanceof NullLiteral) {
        other = binary.right();
      } else if (binary.right() instanceof NullLiteral) {
        other = binary.left();
      }
      if (other == null) {
        return Refinement.NONE;
      }
      AccessKey key = keyOf(other);
      if (key == null) {
        return Refinement.NONE;
      }
      Type type = Types.stripNullable(expressionTypes.get(other));
      boolean nonNullWhenTrue = "!=".equals(binary.operator());
      return new Refinement(key, type, nonNullWhenTrue);
    }
    return Refinement.NONE;
  }

  private void applyRefinement(FlowState state, Refinement refinement, boolean truth) {
    if (refinement == Refinement.NONE) {
      return;
    }
    if (truth == refinement.nonNullWhenTrue) {
      state.put(refinement.key, refinement.type);
    } else {
      state.remove(refinement.key);
    }
  }

  private boolean guaranteesReturn(Statement statement) {
    if (statement instanceof ReturnStatement) {
      return true;
    }
    if (statement instanceof BlockStatement block) {
      for (Statement child : block.statements()) {
        if (guaranteesReturn(child)) {
          return true;
        }
      }
      return false;
    }
    if (statement instanceof IfStatement ifStatement) {
      return ifStatement.elseBranch() != null
          && guaranteesReturn(ifStatement.thenBranch())
          && guaranteesReturn(ifStatement.elseBranch());
    }
    return false;
  }

  private boolean alwaysExits(Statement statement) {
    if (statement instanceof ReturnStatement
        || statement instanceof BreakStatement
        || statement instanceof ContinueStatement) {
      return true;
    }
    if (statement instanceof BlockStatement block) {
      for (Statement child : block.statements()) {
        if (alwaysExits(child)) {
          return true;
        }
      }
      return false;
    }
    if (statement instanceof IfStatement ifStatement) {
      return ifStatement.elseBranch() != null
          && alwaysExits(ifStatement.thenBranch())
          && alwaysExits(ifStatement.elseBranch());
    }
    return false;
  }

  private void checkEqualityOperands(Type left, Type right, Object node) {
    if (left instanceof Type.Null || right instanceof Type.Null) {
      Type other = left instanceof Type.Null ? right : left;
      if (!(other instanceof Type.Nullable) && !Types.isReference(other)) {
        throw error(
            node,
            "Cannot compare "
                + Types.display(left)
                + " and "
                + Types.display(right));
      }
      return;
    }
    if (Types.isVoid(left) || Types.isVoid(right)) {
      throw error(node, "Cannot compare void expressions");
    }
    if (!Types.stripNullable(left).equals(Types.stripNullable(right))) {
      throw error(
          node,
          "Cannot compare "
              + Types.display(left)
              + " and "
              + Types.display(right));
    }
  }

  private boolean isConcatenable(Type type) {
    if (type instanceof Type.Nullable nullable) {
      return isConcatenable(nullable.inner());
    }
    return type instanceof Type.Primitive || type instanceof Type.Null;
  }

  private void requireNumber(Type type, Object node, String operator) {
    if (!Type.NUMBER.equals(type)) {
      throw error(
          node,
          "Operator '" + operator + "' requires numbers, but found " + Types.display(type));
    }
  }

  private void requireValueType(Type type, Object node, String description) {
    if (type instanceof Type.Variable) {
      return;
    }
    if (Types.isVoid(type)) {
      throw error(node, description + " cannot have type void");
    }
    if (type instanceof Type.Null) {
      throw error(node, description + " cannot have the standalone null type");
    }
    if (type instanceof Type.Function) {
      throw error(
          node,
          description
              + " cannot have a function type; function types are only allowed on object fields");
    }
    if (type instanceof Type.Nullable nullable) {
      requireNonNullableInner(nullable.inner(), node, description);
    }
  }

  private void requireReturnType(Type type, Object node) {
    if (Types.isVoid(type)) {
      return;
    }
    if (type instanceof Type.Null) {
      throw error(node, "A function cannot return the standalone null type");
    }
    if (type instanceof Type.Function) {
      throw error(node, "Returning function values is not supported");
    }
    if (type instanceof Type.Nullable nullable) {
      requireNonNullableInner(nullable.inner(), node, "return type");
    }
  }

  private void requireNonNullableInner(Type type, Object node, String description) {
    if (Types.isVoid(type) || type instanceof Type.Null || type instanceof Type.Function) {
      throw error(node, description + " cannot contain " + Types.display(type));
    }
  }

  private Type resolveAlias(String name, List<Type> arguments, Object node) {
    TypeAliasInfo alias = aliases.get(name);
    if (alias == null) {
      throw error(node, "Unknown type '" + name + "'");
    }
    List<String> parameters = alias.declaration.typeParameters();
    if (arguments.size() != parameters.size()) {
      throw error(
          node,
          "Type '"
              + name
              + "' expects "
              + parameters.size()
              + " type argument"
              + (parameters.size() == 1 ? "" : "s")
              + " but found "
              + arguments.size());
    }

    if (alias.declaration.type() instanceof ObjectType type) {
      ensureObjectInfo(alias, type);
      return new Type.Nominal(name, arguments);
    }

    if (environment.findRecord(name).isPresent()) {
      throw error(
          node,
          "Record-bound type '" + name + "' must be declared as an object type with readonly fields");
    }

    if (!resolvingAliases.add(name)) {
      throw error(node, "Type alias cycle involving '" + name + "'");
    }
    try {
      Map<String, Type> scope = new LinkedHashMap<>();
      for (int i = 0; i < parameters.size(); i++) {
        scope.put(parameters.get(i), new Type.Variable(parameters.get(i)));
      }
      Type template = resolve(alias.declaration.type(), scope);
      Map<String, Type> substitutions = new LinkedHashMap<>();
      for (int i = 0; i < parameters.size(); i++) {
        substitutions.put(parameters.get(i), arguments.get(i));
      }
      return Types.substitute(template, substitutions);
    } finally {
      resolvingAliases.remove(name);
    }
  }

  private void ensureObjectInfo(TypeAliasInfo alias, ObjectType type) {
    if (objectInfos.containsKey(alias.name)) {
      return;
    }
    List<String> parameters = alias.declaration.typeParameters();
    ObjectTypeInfo placeholder =
        new ObjectTypeInfo(alias.name, parameters, List.of());
    objectInfos.put(alias.name, placeholder);

    Map<String, Type> scope = new LinkedHashMap<>();
    List<Type> selfArguments = new ArrayList<>();
    for (String parameter : parameters) {
      Type.Variable variable = new Type.Variable(parameter);
      scope.put(parameter, variable);
      selfArguments.add(variable);
    }
    Type.Nominal self = new Type.Nominal(alias.name, selfArguments);
    List<FieldInfo> fields = resolveFields(type.members(), scope, self);
    ObjectTypeInfo complete = new ObjectTypeInfo(alias.name, parameters, fields);
    objectInfos.put(alias.name, complete);
    objectTypes.add(complete);
    RecordType record = environment.findRecord(alias.name).orElse(null);
    if (record != null) {
      validateRecordBinding(alias, complete, record);
    }
  }

  private void ensureRecordInfo(RecordType record) {
    if (objectInfos.containsKey(record.name())) {
      return;
    }
    ObjectTypeInfo placeholder = new ObjectTypeInfo(record.name(), List.of(), List.of());
    objectInfos.put(record.name(), placeholder);
    List<FieldInfo> fields = new ArrayList<>();
    for (RecordType.Field field : record.fields()) {
      fields.add(new FieldInfo(field.name(), true, field.type()));
    }
    ObjectTypeInfo complete = new ObjectTypeInfo(record.name(), List.of(), fields);
    objectInfos.put(record.name(), complete);
    objectTypes.add(complete);
    for (FieldInfo field : complete.fields()) {
      if (field.type() instanceof Type.Nominal nominal) {
        environment
            .findRecord(nominal.name())
            .ifPresent(this::ensureRecordInfo);
      }
    }
  }

  private void validateRecordBinding(
      TypeAliasInfo alias, ObjectTypeInfo source, RecordType record) {
    if (!alias.declaration.typeParameters().isEmpty()) {
      throw error(
          alias.declaration,
          "Record-bound type '" + alias.name + "' must not declare type parameters");
    }
    if (source.fields().size() != record.fields().size()) {
      throw error(
          alias.declaration,
          "Record-bound type '"
              + alias.name
              + "' has "
              + source.fields().size()
              + " field(s), but "
              + record.javaClass().getName()
              + " has "
              + record.fields().size()
              + " record component(s)");
    }
    for (RecordType.Field expected : record.fields()) {
      FieldInfo actual = null;
      for (FieldInfo field : source.fields()) {
        if (field.name().equals(expected.name())) {
          actual = field;
          break;
        }
      }
      if (actual == null) {
        throw error(
            alias.declaration,
            "Record-bound type '"
                + alias.name
                + "' is missing field '"
                + expected.name()
                + "' from "
                + record.javaClass().getName());
      }
      if (!actual.readonly()) {
        throw error(
            alias.declaration,
            "Record-bound field '"
                + alias.name
                + "."
                + actual.name()
                + "' must be readonly");
      }
      if (!actual.type().equals(expected.type())) {
        throw error(
            alias.declaration,
            "Record-bound field '"
                + alias.name
                + "."
                + actual.name()
                + "' has type "
                + Types.display(actual.type())
                + ", but "
                + record.javaClass().getName()
                + " maps it to "
                + Types.display(expected.type()));
      }
    }
    for (FieldInfo actual : source.fields()) {
      if (record.field(actual.name()) == null) {
        throw error(
            alias.declaration,
            "Record-bound type '"
                + alias.name
                + "' declares field '"
                + actual.name()
                + "', but "
                + record.javaClass().getName()
                + " has no matching record component");
      }
    }
  }

  private Type.Nominal inlineObjectInfo(ObjectType type, Map<String, Type> scope) {
    ObjectTypeInfo existing = inlineObjects.get(type);
    if (existing != null) {
      return new Type.Nominal(existing.name(), new ArrayList<>(scope.values()));
    }

    String name = "<inline:" + (++syntheticTypeCounter) + ">";
    List<String> parameters = new ArrayList<>(scope.keySet());
    List<Type> arguments = new ArrayList<>(scope.values());
    ObjectTypeInfo placeholder = new ObjectTypeInfo(name, parameters, List.of());
    objectInfos.put(name, placeholder);
    Type.Nominal self = new Type.Nominal(name, arguments);
    List<FieldInfo> fields = resolveFields(type.members(), scope, self);
    ObjectTypeInfo complete = new ObjectTypeInfo(name, parameters, fields);
    objectInfos.put(name, complete);
    objectTypes.add(complete);
    inlineObjects.put(type, complete);
    return new Type.Nominal(name, arguments);
  }

  private List<FieldInfo> resolveFields(
      List<TypeField> syntax, Map<String, Type> scope, Type.Nominal self) {
    List<FieldInfo> fields = new ArrayList<>();
    Set<String> names = new LinkedHashSet<>();
    for (TypeField field : syntax) {
      if ("this".equals(field.name())) {
        throw error(field, "A field cannot be named 'this'");
      }
      if (!names.add(field.name())) {
        throw error(field, "Duplicate field '" + field.name() + "'");
      }
      Type type = resolve(field.type(), scope);
      if (type instanceof Type.Function function
          && function.receiver() != null
          && !function.receiver().equals(self)) {
        throw error(
            field,
            "Field '"
                + field.name()
                + "' declares this: "
                + Types.display(function.receiver())
                + " but must declare this: "
                + Types.display(self));
      }
      validateFieldType(type, field);
      fields.add(new FieldInfo(field.name(), field.readonly(), type));
    }
    return fields;
  }

  private void validateFieldType(Type type, Object node) {
    if (type instanceof Type.Variable) {
      return;
    }
    if (Types.isVoid(type) || type instanceof Type.Null) {
      throw error(node, "Fields cannot have type " + Types.display(type));
    }
    if (type instanceof Type.Nullable nullable) {
      if (Types.isVoid(nullable.inner())
          || nullable.inner() instanceof Type.Null
          || nullable.inner() instanceof Type.Function) {
        throw error(node, "Fields cannot have type " + Types.display(type));
      }
    }
    if (type instanceof Type.Function function) {
      for (Type parameter : function.parameters()) {
        if (Types.isVoid(parameter)
            || parameter instanceof Type.Null
            || parameter instanceof Type.Function) {
          throw error(node, "Function fields cannot take " + Types.display(parameter));
        }
      }
      if (function.returnType() instanceof Type.Null
          || function.returnType() instanceof Type.Function) {
        throw error(
            node,
            "Function fields cannot return " + Types.display(function.returnType()));
      }
    }
  }

  private Type resolve(TypeReference reference, Map<String, Type> scope) {
    if (reference instanceof NamedType named) {
      List<Type> arguments =
          named.typeArguments().stream().map(argument -> resolve(argument, scope)).toList();
      if (scope.containsKey(named.name())) {
        if (!arguments.isEmpty()) {
          throw error(reference, "Type parameter '" + named.name() + "' cannot take arguments");
        }
        return scope.get(named.name());
      }
      switch (named.name()) {
        case "number" -> {
          requireNoTypeArguments(named, arguments);
          return Type.NUMBER;
        }
        case "string" -> {
          requireNoTypeArguments(named, arguments);
          return Type.STRING;
        }
        case "boolean" -> {
          requireNoTypeArguments(named, arguments);
          return Type.BOOLEAN;
        }
        case "void" -> {
          requireNoTypeArguments(named, arguments);
          return Type.VOID;
        }
        default -> {
          NativeType nativeType = environment.findType(named.name()).orElse(null);
          if (nativeType != null) {
            requireArity(named, arguments, nativeType.parameterNames().size());
            return new Type.Nominal(named.name(), arguments);
          }
          if (aliases.containsKey(named.name())) {
            return resolveAlias(named.name(), arguments, reference);
          }
          RecordType record = environment.findRecord(named.name()).orElse(null);
          if (record != null) {
            requireNoTypeArguments(named, arguments);
            ensureRecordInfo(record);
            return new Type.Nominal(record.name(), List.of());
          }
          return resolveAlias(named.name(), arguments, reference);
        }
      }
    }

    if (reference instanceof NullType) {
      return Type.NULL;
    }

    if (reference instanceof UnionType union) {
      if (union.members().size() != 2) {
        throw error(reference, "Only nullable unions are allowed; use 'T | null'");
      }
      Type first = resolve(union.members().get(0), scope);
      Type second = resolve(union.members().get(1), scope);
      if (first instanceof Type.Null && second instanceof Type.Null) {
        throw error(reference, "A nullable type must have a non-null member");
      }
      if (first instanceof Type.Null) {
        return new Type.Nullable(second);
      }
      if (second instanceof Type.Null) {
        return new Type.Nullable(first);
      }
      throw error(reference, "Only nullable unions are allowed; use 'T | null'");
    }

    if (reference instanceof ObjectType objectType) {
      return inlineObjectInfo(objectType, scope);
    }

    if (reference instanceof FunctionType functionType) {
      Type receiver = null;
      int offset = 0;
      if (!functionType.parameters().isEmpty()
          && "this".equals(functionType.parameters().get(0).name())) {
        Parameter thisParameter = functionType.parameters().get(0);
        receiver = resolve(thisParameter.type(), scope);
        if (!(receiver instanceof Type.Nominal)) {
          throw error(thisParameter, "The 'this' parameter must have an object type");
        }
        offset = 1;
      }
      List<Type> parameters = new ArrayList<>();
      for (int i = offset; i < functionType.parameters().size(); i++) {
        Parameter parameter = functionType.parameters().get(i);
        if ("this".equals(parameter.name())) {
          throw error(parameter, "'this' must be the first parameter");
        }
        Type type = resolve(parameter.type(), scope);
        if (Types.isVoid(type)
            || type instanceof Type.Null
            || type instanceof Type.Function) {
          throw error(parameter, "Function parameter cannot have type " + Types.display(type));
        }
        parameters.add(type);
      }
      Type returnType = resolve(functionType.returnType(), scope);
      if (returnType instanceof Type.Null || returnType instanceof Type.Function) {
        throw error(
            functionType.returnType(),
            "Function return type cannot be " + Types.display(returnType));
      }
      return new Type.Function(receiver, parameters, returnType);
    }

    throw error(reference, "Unsupported type");
  }

  private void requireNoTypeArguments(NamedType named, List<Type> arguments) {
    if (!arguments.isEmpty()) {
      throw error(named, "Type '" + named.name() + "' does not take type arguments");
    }
  }

  private void requireArity(NamedType named, List<Type> arguments, int arity) {
    if (arguments.size() != arity) {
      throw error(
          named,
          "Type '"
              + named.name()
              + "' expects "
              + arity
              + " type argument"
              + (arity == 1 ? "" : "s"));
    }
  }

  private boolean isBuiltinTypeName(String name) {
    return switch (name) {
      case "number", "string", "boolean", "void", "null" -> true;
      default -> environment.findType(name).isPresent();
    };
  }

  private boolean isBuiltinValueName(String name) {
    return "console".equals(name) || environment.findType(name).isPresent();
  }

  private boolean isBuiltinTypeReference(Expression expression) {
    return expression instanceof Identifier identifier
        && environment.findType(identifier.name()).isPresent()
        && findVariable(identifier.name()) == null;
  }

  private boolean isConsoleLog(MemberExpression member) {
    return member.object() instanceof Identifier identifier
        && "console".equals(identifier.name())
        && "log".equals(member.property())
        && findVariable("console") == null;
  }

  private VariableSymbol findVariable(String name) {
    for (Scope scope : scopes) {
      VariableSymbol variable = scope.variables.get(name);
      if (variable != null) {
        return variable;
      }
    }
    return null;
  }

  private boolean isDeclaredInCurrentScope(String name) {
    return !scopes.isEmpty() && scopes.peek().variables.containsKey(name);
  }

  private void declare(VariableSymbol symbol, Object node) {
    if (isDeclaredInCurrentScope(symbol.name())) {
      throw error(node, "Variable '" + symbol.name() + "' is already declared");
    }
    scopes.peek().variables.put(symbol.name(), symbol);
  }

  private CheckException error(Object node, String message) {
    SourceSpan span = sourceMap.spanOf(node);
    if (span == null) {
      return new CheckException(message, 1, 1);
    }
    return new CheckException(message, span.startLine(), span.startColumn());
  }

  private static final class Scope {
    private final Map<String, VariableSymbol> variables = new LinkedHashMap<>();
  }

  private record Context(Type returnType, boolean inLoop, ThisSymbol thisSymbol) {}

  private record TypeAliasInfo(String name, TypeAliasDeclaration declaration) {}

  private record Refinement(AccessKey key, Type type, boolean nonNullWhenTrue) {
    private static final Refinement NONE = new Refinement(null, null, false);

    private Refinement negated() {
      if (this == NONE) {
        return NONE;
      }
      return new Refinement(key, type, !nonNullWhenTrue);
    }
  }

  private static final class AccessKey {
    private final Object root;
    private final List<String> path;

    private AccessKey(Object root, List<String> path) {
      this.root = root;
      this.path = List.copyOf(path);
    }

    private AccessKey child(String property) {
      List<String> child = new ArrayList<>(path);
      child.add(property);
      return new AccessKey(root, child);
    }

    private boolean isPrefixOf(AccessKey other) {
      if (root != other.root || path.size() > other.path.size()) {
        return false;
      }
      return path.equals(other.path.subList(0, path.size()));
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof AccessKey key && root.equals(key.root) && path.equals(key.path);
    }

    @Override
    public int hashCode() {
      return 31 * root.hashCode() + path.hashCode();
    }
  }

  private static final class FlowState {
    private final Map<AccessKey, Type> refinements = new LinkedHashMap<>();

    private FlowState copy() {
      FlowState copy = new FlowState();
      copy.refinements.putAll(refinements);
      return copy;
    }

    private Type get(AccessKey key) {
      return refinements.get(key);
    }

    private void put(AccessKey key, Type type) {
      refinements.put(key, type);
    }

    private void remove(AccessKey key) {
      refinements.remove(key);
    }

    private void invalidate(AccessKey target) {
      if (target == null) {
        return;
      }
      refinements.keySet().removeIf(target::isPrefixOf);
    }

    private void invalidateMembers() {
      refinements.keySet().removeIf(key -> !key.path.isEmpty());
    }

    private static FlowState intersection(FlowState first, FlowState second) {
      FlowState result = new FlowState();
      for (Map.Entry<AccessKey, Type> entry : first.refinements.entrySet()) {
        Type other = second.refinements.get(entry.getKey());
        if (entry.getValue().equals(other)) {
          result.refinements.put(entry.getKey(), entry.getValue());
        }
      }
      return result;
    }
  }
}
