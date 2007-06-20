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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.jsio.client.impl.Extractor;
import com.google.gwt.user.rebind.SourceWriter;

class PeeringFragmentGenerator extends FragmentGenerator {

  protected JMethod findConstructor(TypeOracle oracle, JType type) {
    JClassType asClass = type.isClassOrInterface();
    if (asClass == null) {
      return null;
    }

    JMethod m =
        asClass.findMethod(JSFlyweightWrapperGenerator.CONSTRUCTOR,
            new JType[] {oracle.findType(JavaScriptObject.class.getName())});

    if (m == null || !m.isStatic() || !type.equals(m.getReturnType())) {
      return null;
    }

    return m;
  }

  protected JField findExtractor(TypeOracle oracle, JType type) {
    JClassType asClass = type.isClassOrInterface();
    if (asClass == null) {
      return null;
    }

    JField f = asClass.findField(JSFlyweightWrapperGenerator.EXTRACTOR);
    if (f == null
        || !isAssignable(oracle, f.getType().isClassOrInterface(),
            Extractor.class)) {
      return null;
    }

    return f;
  }
  
  protected JField findPeer(TypeOracle oracle, JType type) {
    JClassType asClass = type.isClassOrInterface();
    if (asClass == null) {
      return null;
    }

    JField f = asClass.findField(JSFlyweightWrapperGenerator.OBJ);
    if (f == null
        || !isAssignable(oracle, f.getType().isClassOrInterface(),
            JavaScriptObject.class)) {
      return null;
    }

    return f;
  }

  boolean accepts(TypeOracle oracle, JType type) {
    return (findConstructor(oracle, type) != null)
        || (findExtractor(oracle, type) != null)
        || (findPeer(oracle, type) != null);
  }

  void fromJS(FragmentGeneratorContext context)
      throws UnableToCompleteException {
    if (findConstructor(context.typeOracle, context.returnType) == null) {
      context.parentLogger.log(TreeLogger.ERROR, "The type " +
          context.returnType.getQualifiedSourceName() + " must possess a "
          + JSFlyweightWrapperGenerator.CONSTRUCTOR
          + " field to be used as a return type.", null);
      throw new UnableToCompleteException();
    }
    SourceWriter sw = context.sw;

    sw.print(context.parameterName);
    sw.print(".");
    sw.print(JSWrapperGenerator.BACKREF);
    sw.print(" || ");
    sw.print("@");
    sw.print(context.returnType.getQualifiedSourceName());
    sw.print("::");
    sw.print(JSFlyweightWrapperGenerator.CONSTRUCTOR);
    sw.print("(Lcom/google/gwt/core/client/JavaScriptObject;)(");
    sw.print(context.parameterName);
    sw.print(")");
  }

  void toJS(FragmentGeneratorContext context) throws UnableToCompleteException {
    if (findPeer(context.typeOracle, context.returnType) == null) {
      context.parentLogger.log(TreeLogger.ERROR, "The type " +
          context.returnType.getQualifiedSourceName() + " must possess a "
          + JSFlyweightWrapperGenerator.OBJ
          + " field to be used as a parameter type.", null);
      throw new UnableToCompleteException();
    }
    SourceWriter sw = context.sw;

    sw.print("@");
    sw.print(context.returnType.getQualifiedSourceName());
    sw.print("::");
    sw.print(JSFlyweightWrapperGenerator.OBJ);
  }

  void writeExtractorJSNIReference(FragmentGeneratorContext context)
      throws UnableToCompleteException {

    if (findExtractor(context.typeOracle, context.returnType) == null) {
      context.parentLogger.branch(TreeLogger.ERROR, "The type " +
          context.returnType.getQualifiedSourceName() + " must possess an "
          + JSFlyweightWrapperGenerator.EXTRACTOR
          + " field to be used with a JSList", null);
      throw new UnableToCompleteException();
    }

    SourceWriter sw = context.sw;

    sw.print("@");
    sw.print(context.returnType.getQualifiedSourceName());
    sw.print("::");
    sw.print(JSFlyweightWrapperGenerator.EXTRACTOR);
  }
}
