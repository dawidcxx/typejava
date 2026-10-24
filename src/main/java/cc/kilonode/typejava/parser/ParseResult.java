package cc.kilonode.typejava.parser;

import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.SourceMap;

public record ParseResult(Program program, SourceMap sourceMap) {}
