/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.jsio.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.jsio.client.JSFunction;
import com.google.gwt.user.rebind.SourceWriter;

/**
 * Exports a Java function as a JavaScript function object. If the JSFunction's
 * exported method is static and has parameter and return types that require no
 * wrapping or unwrapping can be used as-is, otherwise a linkage function will
 * be created to perform the necessary type conversions.
 */
class JSFunctionFragmentGenerator extends FragmentGenerator {
  /**
   * Write out the JavaScript wrapper around a Java method.
   */
  static void writeFunctionForMethod(FragmentGeneratorContext context, JMethod m)
      throws UnableToCompleteException {
    context.parentLogger.branch(TreeLogger.DEBUG, "Writing function() for "
        + m.getName(), null);

    if (isIdentityFunction(context, m)) {
      writeIdentityInvocation(context, m);
    } else {
      writeLinkageInvocation(context, m);
    }
  }

  /**
   * Determines if the exported method can be used as-is.
   */
  private static boolean isIdentityFunction(FragmentGeneratorContext context,
      JMethod m) throws UnableToCompleteException {
    TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG,
        "Determining identity status of " + m.getName(), null);

    boolean identityOnly = m.isStatic();
    JParameter[] parameters = m.getParameters();

    identityOnly &= context.fragmentGeneratorOracle.findFragmentGenerator(
        logger, context.typeOracle, m.getReturnType()).isIdentity();

    for (int i = 0; i < parameters.length && identityOnly; i++) {
      FragmentGenerator fragmentGenerator = context.fragmentGeneratorOracle.findFragmentGenerator(
          logger, context.typeOracle, parameters[i].getType());
      identityOnly &= fragmentGenerator.isIdentity();
    }

    return identityOnly;
  }

  /**
   * Simply prints a JSNI reference to the exported function.
   */
  private static void writeIdentityInvocation(FragmentGeneratorContext context,
      JMethod m) throws UnableToCompleteException {
    SourceWriter sw = context.sw;
    JParameter[] parameters = m.getParameters();

    sw.print("@");
    sw.print(m.getEnclosingType().getQualifiedSourceName());
    sw.print("::");
    sw.print(m.getName());
    sw.print("(");

    // Argument list for the Java invocation
    for (int i = 0; i < parameters.length; i++) {
      sw.print(parameters[i].getType().getJNISignature());
    }

    sw.print(")");
  }

  /**
   * Writes a linkage function object that will invoke the exported function.
   */
  private static void writeLinkageInvocation(FragmentGeneratorContext context,
      JMethod m) throws UnableToCompleteException {
    TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG,
        "Writing function() for " + m.getName(), null);

    SourceWriter sw = context.sw;
    JParameter[] parameters = m.getParameters();

    sw.print("function(");
    for (int i = 0; i < parameters.length; i++) {
      sw.print("arg");
      sw.print(String.valueOf(i));
      if (i < parameters.length - 1) {
        sw.print(", ");
      }
    }
    sw.println(") {");
    sw.indent();

    // Invoke the Java function
    sw.print("return ");

    // Don't need to reference the instance on a static method
    if (!m.isStatic()) {
      sw.print(context.parameterName);
      sw.print(".");
    }

    sw.print("@");
    sw.print(m.getEnclosingType().getQualifiedSourceName());
    sw.print("::");
    sw.print(m.getName());
    sw.print("(");

    // Argument list for the Java invocation
    for (int i = 0; i < parameters.length; i++) {
      sw.print(parameters[i].getType().getJNISignature());
    }

    sw.println(")(");
    // Indent the parameters, each on its own like to improve readability
    sw.indent();
    sw.indent();
    
    for (int i = 0; i < parameters.length; i++) {
      // Create a sub-context to generate the wrap/unwrap logic
      JType returnType = parameters[i].getType();
      FragmentGeneratorContext subParams = new FragmentGeneratorContext(context);
      subParams.returnType = returnType;
      subParams.parameterName = "arg" + i;

      FragmentGenerator fragmentGenerator = context.fragmentGeneratorOracle.findFragmentGenerator(
          logger, context.typeOracle, returnType);
      if (fragmentGenerator == null) {
        logger.log(TreeLogger.ERROR, "No fragment generator for "
            + returnType.getQualifiedSourceName(), null);
        throw new UnableToCompleteException();
      }

      fragmentGenerator.fromJS(subParams);

      if (i < parameters.length - 1) {
        sw.println(", ");
      }
    }

    sw.outdent();
    sw.outdent();
    sw.println(");");
    sw.outdent();
    sw.print("}");
  }

  boolean accepts(TypeOracle typeOracle, JType type) {
    JClassType asClass = type.isClassOrInterface();

    if (asClass == null) {
      return false;
    }

    return isAssignable(typeOracle, asClass, JSFunction.class);
  }

  String defaultValue(TypeOracle typeOracle, JType type)
      throws UnableToCompleteException {
    return "null";
  }

  void fromJS(FragmentGeneratorContext context)
      throws UnableToCompleteException {
    context.parentLogger.branch(TreeLogger.ERROR,
        "JavaScript functions may not be imported via JSFunction.", null);

    throw new UnableToCompleteException();
  }

  void toJS(FragmentGeneratorContext context) throws UnableToCompleteException {
    TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG,
        "Writing function() wrapper for JSFunction", null);

    SourceWriter sw = context.sw;
    TypeOracle typeOracle = context.typeOracle;
    JClassType functionClass = context.returnType.isClassOrInterface();

    if (functionClass.equals(typeOracle.findType(JSFunction.class.getName()))) {
      logger.log(TreeLogger.ERROR, "You must use a subinterface of JSFunction"
          + " so that the generator can extract a method signature.", null);
      throw new UnableToCompleteException();
    }

    // XXX this is a hack to support the JSFunction having the same
    // lifetime as the JSFunction object without having to use GWT.create
    // on every JSFunction object as that would discourage anonymous classes.

    sw.print("(");
    sw.print(context.parameterName);
    sw.print(".@com.google.gwt.jsio.client.JSFunction::exportedFunction || (");
    sw.print(context.parameterName);
    sw.print(".@com.google.gwt.jsio.client.JSFunction::exportedFunction = ");
    writeFunctionForMethod(context, findExportedMethod(logger, functionClass));
    sw.print("))");
  }

  void writeExtractorJSNIReference(FragmentGeneratorContext context)
      throws UnableToCompleteException {
    context.parentLogger.branch(TreeLogger.ERROR,
        "JSFunctions should never need extraction", null);
    throw new UnableToCompleteException();
  }

  /**
   * Find the to-be-exported method within a class.
   */
  private JMethod findExportedMethod(TreeLogger logger, JClassType clazz)
      throws UnableToCompleteException {

    // Look for a gwt.export annotation on the enclosing class.
    String[][] exportMeta = clazz.getMetaData(JSWrapperGenerator.EXPORTED);
    String exported;
    if (exportMeta.length == 1 && exportMeta[0].length == 1) {
      exported = exportMeta[0][0];
      logger.log(TreeLogger.DEBUG, "Using export annotation", null);
    } else {
      exported = null;
    }

    // If there's no explicit annotation, we look for the presence of a single
    // function.
    JMethod[] methods = clazz.getMethods();
    if (exported == null && methods.length > 1) {
      logger.log(TreeLogger.ERROR, "JSFunctions with multiple methods must "
          + " specify a " + JSWrapperGenerator.EXPORTED + " annotation.", null);
      throw new UnableToCompleteException();
    }

    if (methods.length == 0) {
      logger.log(TreeLogger.ERROR, "The JSFunction interface did not "
          + "declare any functions.", null);
      throw new UnableToCompleteException();

      // If no value is specified, take the one method that was found.
    } else if ((exported == null) && (methods.length == 1)) {
      return methods[0];

      // Find the matching function
    } else {
      for (int i = 0; i < methods.length; i++) {
        JMethod m = methods[i];
        if (exported.equals(m.getName())) {
          return m;
        }
      }
    }

    logger.log(TreeLogger.ERROR, "Did not find exported function " + exported
        + " in type " + clazz.getQualifiedSourceName(), null);
    throw new UnableToCompleteException();
  }
}
