/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.jsio.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests native invocation capabilities of the wrapper classes.
 */
public class JSExporterTest extends GWTTestCase {

  /**
   * Demonstrates exporting methods from a JSWrapper.
   */
  abstract static class ExportedMethods implements JSWrapper<ExportedMethods> {
    @Exported
    public int add(int a, int b) {
      return a + b;
    }

    /**
     * This is final to ensure that we aren't excluding non-overridable methods.
     */
    @Exported
    @FieldName("sub")
    public final int subtract(int a, int b) {
      return a - b;
    }

    @Exported
    public int sum(JSList<Integer> numbers) {
      int toReturn = 0;

      for (int i : numbers) {
        toReturn += i;
      }

      return toReturn;
    }
  }

  /**
   * Demonstrates how Java objects of a type with gwt.exported annotations can
   * have methods bound to a JavaScriptObject.
   */
  static interface MathFlyweightWrapper extends JSFlyweightWrapper {
    /**
     * Indicates that the method should be used to export methods from a Java
     * object to a JSO.
     */
    @Binding
    void bind(JavaScriptObject obj, MathMethods m);

    @Constructor("$wnd.Object")
    JavaScriptObject construct();
  }

  static class MathImpl implements MathMethods {
    public int add(int a, int b) {
      return a + b;
    }

    /**
     * Final to ensure that we're not excluding non-overridable methods.
     */
    public final int subtract(int a, int b) {
      return a - b;
    }

    public int sum(JSList<Integer> numbers) {
      int toReturn = 0;
      
      for (int i : numbers) {
        toReturn += i;
      }

      return toReturn;
    }
  }

  /**
   * Demonstrates how gwt.exported interfaces allow objects provided by third
   * parties to be attached to a JSO. The only thing special about this
   * interface is the presence of gwt.exported tags.
   */
  static interface MathMethods {
    @Exported
    int add(int a, int b);

    @Exported
    @FieldName("sub")
    int subtract(int a, int b);

    @Exported
    int sum(JSList<Integer> numbers);
  }

  static interface MissingMethodInterface extends JSFlyweightWrapper {
    @Binding
    void bind(JavaScriptObject jso);

    void missingMethod(JavaScriptObject jso);
  }

  /**
   * Used to test exported methods that are declared to return a peering Java
   * object.
   */
  static class SimpleList {
    protected static interface SimpleListFlyweight extends JSFlyweightWrapper {
      @Binding
      void bind(JavaScriptObject obj, SimpleList list);

      @Constructor("$wnd.Object")
      JavaScriptObject create();
    }

    private static final SimpleListFlyweight flyweight = (SimpleListFlyweight) GWT.create(SimpleListFlyweight.class);
    private JavaScriptObject jsoPeer;

    private final SimpleList next;

    private final int value;

    SimpleList(SimpleList next, int value) {
      this.next = next;
      this.value = value;
      jsoPeer = flyweight.create();
      flyweight.bind(jsoPeer, this);
    }

    @Exported
    public SimpleList getNext() {
      return next;
    }

    @Exported
    public int getValue() {
      return value;
    }
  }

  /**
   * Used to test exported methods that return a JSWrapper subclass.
   */
  abstract static class SimpleListWrapper implements JSWrapper<SimpleListWrapper> {
    private SimpleListWrapper next;
    private int value;

    @Exported
    public SimpleListWrapper getNext() {
      return next;
    }

    @Exported
    public int getValue() {
      return value;
    }

    public void setNext(SimpleListWrapper next) {
      this.next = next;
    }

    public void setValue(int value) {
      this.value = value;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.jsio.JSIOTest";
  }

  public void testAdd() {
    ExportedMethods export = (ExportedMethods) GWT.create(ExportedMethods.class);
    JavaScriptObject obj = export.getJavaScriptObject();

    testAddJSO(obj);
  }

  public void testAddFlyweight() {
    MathFlyweightWrapper wrapper = (MathFlyweightWrapper) GWT.create(MathFlyweightWrapper.class);
    MathImpl impl = new MathImpl();

    JavaScriptObject jso = wrapper.construct();
    wrapper.bind(jso, impl);
    testAddJSO(jso);
  }

  public void testAddJSO(JavaScriptObject obj) {
    assertTrue(testMethod(obj, "add"));
    assertEquals(10, invokeAdd(obj, 3, 7));
  }

  public void testExportedReturningObjectsFlyweight() {
    SimpleList start = null;

    for (int i = 0; i < 6; i++) {
      start = new SimpleList(start, i);
      assertTrue(testMethod(start.jsoPeer, "getNext"));
      assertTrue(testMethod(start.jsoPeer, "getValue"));
    }

    assertEquals(15, foldSum(start.jsoPeer));
  }

  public void testExportedReturningObjectsWrapper() {
    SimpleListWrapper start = null;

    for (int i = 0; i < 6; i++) {
      SimpleListWrapper previous = (SimpleListWrapper) GWT.create(SimpleListWrapper.class);
      previous.setNext(start);
      previous.setValue(i);
      start = previous;

      assertTrue(testMethod(start.getJavaScriptObject(), "getNext"));
      assertTrue(testMethod(start.getJavaScriptObject(), "getValue"));
      assertFalse(testMethod(start.getJavaScriptObject(), "setNext"));
      assertFalse(testMethod(start.getJavaScriptObject(), "setValue"));
    }

    assertEquals(15, foldSum(start.getJavaScriptObject()));
  }

  public void testMissingMethod() {
    if (GWT.isScript()) {
      // This test does not make sense in web mode because there are no asserts
      return;
    }

    MissingMethodInterface i = (MissingMethodInterface) GWT.create(MissingMethodInterface.class);
    try {
      i.bind(JavaScriptObject.createObject());
      fail("Should have failed on an assertion.  Did you run with -ea?");
    } catch (AssertionError e) {
      // Expected
    }
  }

  public void testSub() {
    ExportedMethods export = (ExportedMethods) GWT.create(ExportedMethods.class);
    JavaScriptObject obj = export.getJavaScriptObject();

    assertTrue(testMethod(obj, "sub"));
    assertEquals(-4, export.subtract(3, 7));
    assertEquals(-4, invokeSub(obj, 3, 7));
  }

  public void testSum() {
    ExportedMethods export = (ExportedMethods) GWT.create(ExportedMethods.class);
    assertEquals(15, invokeSum(export.getJavaScriptObject()));
  }

  public void testSumFlyweight() {
    MathFlyweightWrapper wrapper = (MathFlyweightWrapper) GWT.create(MathFlyweightWrapper.class);
    MathImpl impl = new MathImpl();

    JavaScriptObject jso = wrapper.construct();
    wrapper.bind(jso, impl);
    assertEquals(15, invokeSum(jso));
  }

  private native int foldSum(JavaScriptObject obj) /*-{
    var toReturn = 0;
    while (obj) {
      toReturn = toReturn + obj.getValue();
      obj = obj.getNext();
    }
    return toReturn;
  }-*/;

  private native int invokeAdd(JavaScriptObject jso, int a, int b) /*-{
    return jso.add(a, b);
  }-*/;

  private native int invokeSub(JavaScriptObject obj, int a, int b) /*-{
    return obj.sub(a, b);
  }-*/;

  private native int invokeSum(JavaScriptObject obj) /*-{
    return obj.sum([1, 2, 3, 4, 5]);
  }-*/;

  private native boolean testMethod(JavaScriptObject obj, String methodName) /*-{
    return methodName in obj;
  }-*/;
}
