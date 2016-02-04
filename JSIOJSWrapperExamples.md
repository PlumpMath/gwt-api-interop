# JSIO JSWrapper Example code #

The following examples demonstrate how to write an interface that maps onto a JavaScript object to access it from GWT code.


## Hello World using JSWrapper ##

This simple example demonstrates how to access a property in a JavaScript Object (JSO).

```
  /*
   * HelloJSIO.java
   * A simple wrapper around a single JavaScript object property named 'hello'
   */

  @BeanProperties
  interface HelloWrapper extends JSWrapper {
    public String getHello();
  }

  HelloWrapper hello = (HelloWrapper)GWT.create(HelloWrapper.class);
  hello.setJSONData("{hello:'Hello World!'}");
  Window.alert(hello.getHello());
```



### Mixing Access to JavaScript Object with other methods ###

The following example uses several JSIO features:

  * Some bean accessor methods are created.
  * A concrete Java method is defined in the JSO.
  * A JavaScript function is imported from JavaScript to Java.

```
@BeanProperties
abstract class MixedWrapper implements JSWrapper {

  // Property accessor - read the property named 'a'
  public abstract int getA();

  // Property accessor - read the property named 'b'
  public abstract int getB();

  // Property accessor - write to the property named 'b'
  public abstract int setB();

  // Method that is implemented in Java that will be translated to 
  // JavaScript and set in the JavaScript Object.
  public int multiply() {
    return getA() * getB();
  }

  // This method would be imported at some point. JSIO supports 
  // "duck-typing" - if no function with the name 'importedFunction()' 
  // is defined in the object at the time the method is invoked, 
  // you will get a run-time error.
  public abstract int importedFunction(String s);

}
```

The following demonstrates how to use the new wrapper:

```
  MixedWrapper wrapper = (MixedWrapper)GWT.create(MixedWrapper.class);
  wrapper.setJSONData("{a:2, b:5}");
  Window.alert(wrapper.multiply());
  wrapper.setB(10);
  Window.alert(wrapper.multiply());
```

Running the code would show you the value `10` in the first window, then `20` in the second window.

## Instantiating a JavaScript object using a constructor ##

If you have a JavaScript library that is written in an object oriented fashion, you can map a function in a JSWrapper class to the JavaScript constructor instead of using the `setJSONData()` or `setJavaScriptObject()`.

Suppose you wanted to access the following JavaScript API:

```

// Create a new TextThingo object
// e - bounding box element to attach to
// value - a string to display
function TextThingo (e, value) {
  this.textValue = value;
  this.text = document.createElement("span");
  e.appendChild(this.text);
}

// Display the current value in the TextThingo object
TextThingo.prototype.show = function () {
  // remove all children
  while (this.text.FirstChild) {
    this.text.removeChild(text.firstChild());
  }
  this.text.appendChild(document.createTextNode(this.textValue));
}
```

This API would be used in JavaScript as follows:

```
  var t = new TextThingo (document.getElementById("elementId"), "Hello World!");
  t.textValue = "Goodbye Cruel World!";
  t.show();
```

And the resulting web page would display the text, "Goodbye Cruel World" in an element declared with ID "elementId" in the host HTML page.

Now, we will wrap the API using JSIO so we can access it from GWT.  The following code shows how to wrap the constructor, the start() method, and create bean accessors to the textValue property:

```
@BeanProperties
public interface TextThingo extends JSWrapper {

  @Constructor("$wnd.TextThingo")
  public TextThingo construct(Element container, String value);

  /* Automatically created bean method to access option property */
  public void setTextValue(String value); 

  /* Automatically created bean method to access option property */
  public String getTextValue();

  /* Maps to the JavaScript start() function on the TextThingo object */
  public void show();

}
```

You will need to add the JSIO `<inherits>` line to your 

&lt;module&gt;

_`.gwt.xml` file._

```
<module>

  <!-- Inherit the core Web Toolkit stuff.                  -->
  <inherits name='com.google.gwt.user.User' />
  <inherits name='com.google.gwt.jsio.JSIO' />
  
  <!-- Specify the app entry point class.                   -->
  <entry-point class='com.example.gwt.client.JSWrapperExample' />

</module>

```

Now to use the wrapper, you need to use `GWT.create()` to invoke the JSIO generators, then run the `construct()` method to instantiate the JavaScript object:

```
  private TextThingo textThingo;

  void onModuleLoad () {
    Panel s = new SimplePanel();
    textThingo = GWT.create(TextThingo.class);
 
    // Don't call any other methods in the new object until the 'construct' method is called.
    textThingo.construct(s.getElement(), "Hello World!");

    textThingo.setTextValue("Goodbye Cruel World!");
    textThingo.show();

    // Add the panel containing the text thingo to the interface.
    RootPanel.get("elementId").add(s);
  }

```

## Invoking a method defined in Java from a JavaScript API ##

Consider the following API defined in JavaScript:

```
function A () {
  this.dummy = "foo";
}

A.prototype.myMethod = function (b) {
  window.alert(b.retrieveMessage());
}

//  Example invocation using the above API.
//
var count = 0;

function invokeApi () {
  var a = new A();
  var b = new Object();
  b.retrieveMessage = function () { 
     count++; 
     return "Hello World! "+count; 
  };
       
  a.myMethod(b);
}

```


The developer is expected to add a function to 'B' to return a string used in the `myMethod()` function.  In GWT, the developer will want to write the definition of `retrieveMessage()` in Java.  For this to work correctly in hosted mode, JSIO must generate special interface code.  For this to work correctly in Web mode, the compiler must be told to preserve the name of the `retrieveMessage()` method.

Wrapping the `A` object is straightforward with JSWrapper.  Note that in this case since our constructor takes no argument, we can annotate with `@Constructor` at the class level:

```
import com.google.gwt.jsio.client.JSWrapper;

@Constructor("$wnd.A")
public interface A extends JSWrapper {
    
    /* A function in the JavaScript API we want to call and pass an instance
     * containing a function defined in Java to.
     */
    void myMethod (B bObject);
    
}
```

The `B` object needs to be wrapped in a different way.  Since we are extending it by adding a new method, we cannot represent it with a Java interface.  Instead, we use an abstract class.  The class extends JSWrapper and makes use of the `@Exported` annotation on the functions that are inteded to be called from within JavaScript:

```
import com.google.gwt.jsio.client.JSWrapper;

@Constructor("Object")
public abstract class B implements JSWrapper {
  static int numInvocations = 0;

  /**
   * A JavaScript API expects the function myMethod() as a property in
   * this object, so we need to export it.
   */ 
  @Exported
  public String retrieveMessage() {
    numInvocations++;
    return "Hello World! "+numInvocations;
  }
}
```

### Instantiating the interface ###

Below is an example of how to invoke the API from a GWT project:

```

 public void onModuleLoad() {
    Panel s = new SimplePanel();
    Button button = new Button("Click to show window");
    s.add(button);
    
    button.addClickListener(new ClickListener() {

      public void onClick(Widget sender) {
        A a = GWT.create(A.class);
        B b = GWT.create(B.class);
        a.myMethod(b);
      }
      
    });
   
    // Add the panel button to the interface.
    RootPanel.get("elementId").add(s);
  }
```