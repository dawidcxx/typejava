package cc.kilonode.typejava;

import java.lang.reflect.Method;

record ExportedFunction(String name, Method method, ExportSignature signature) {}
