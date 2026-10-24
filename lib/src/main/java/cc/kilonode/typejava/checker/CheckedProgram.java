package cc.kilonode.typejava.checker;

import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.Expression;
import cc.kilonode.typejava.ast.FunctionExpression;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.ObjectLiteral;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.SourceMap;
import cc.kilonode.typejava.ast.VariableDeclaration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class CheckedProgram {
  public final Program program;
  public final SourceMap sourceMap;
  public final List<ObjectTypeInfo> objectTypes;
  public final List<VariableSymbol> topLevelVariables;
  public final List<FunctionSymbol> functions;
  public final Map<String, FunctionSymbol> functionsByName;
  public final Map<String, VariableSymbol> topLevelVariablesByName;
  public final IdentityHashMap<Expression, Type> expressionTypes;
  public final IdentityHashMap<Identifier, Object> identifiers;
  public final IdentityHashMap<MemberExpression, FieldAccess> memberAccesses;
  public final IdentityHashMap<CallExpression, CallTarget> calls;
  public final IdentityHashMap<ObjectLiteral, ObjectLiteralInfo> objectLiterals;
  public final IdentityHashMap<FunctionExpression, MethodSite> methods;
  public final IdentityHashMap<VariableDeclaration, VariableSymbol> variableDeclarations;
  public final IdentityHashMap<Parameter, VariableSymbol> parameterSymbols;

  public CheckedProgram(
      Program program,
      SourceMap sourceMap,
      List<ObjectTypeInfo> objectTypes,
      List<VariableSymbol> topLevelVariables,
      List<FunctionSymbol> functions,
      Map<String, FunctionSymbol> functionsByName,
      Map<String, VariableSymbol> topLevelVariablesByName,
      IdentityHashMap<Expression, Type> expressionTypes,
      IdentityHashMap<Identifier, Object> identifiers,
      IdentityHashMap<MemberExpression, FieldAccess> memberAccesses,
      IdentityHashMap<CallExpression, CallTarget> calls,
      IdentityHashMap<ObjectLiteral, ObjectLiteralInfo> objectLiterals,
      IdentityHashMap<FunctionExpression, MethodSite> methods,
      IdentityHashMap<VariableDeclaration, VariableSymbol> variableDeclarations,
      IdentityHashMap<Parameter, VariableSymbol> parameterSymbols) {
    this.program = program;
    this.sourceMap = sourceMap;
    this.objectTypes = List.copyOf(objectTypes);
    this.topLevelVariables = List.copyOf(topLevelVariables);
    this.functions = List.copyOf(functions);
    this.functionsByName = Map.copyOf(functionsByName);
    this.topLevelVariablesByName = Map.copyOf(topLevelVariablesByName);
    this.expressionTypes = new IdentityHashMap<>(expressionTypes);
    this.identifiers = new IdentityHashMap<>(identifiers);
    this.memberAccesses = new IdentityHashMap<>(memberAccesses);
    this.calls = new IdentityHashMap<>(calls);
    this.objectLiterals = new IdentityHashMap<>(objectLiterals);
    this.methods = new IdentityHashMap<>(methods);
    this.variableDeclarations = new IdentityHashMap<>(variableDeclarations);
    this.parameterSymbols = new IdentityHashMap<>(parameterSymbols);
  }
}
