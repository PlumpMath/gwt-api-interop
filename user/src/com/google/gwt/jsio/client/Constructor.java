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

import com.google.gwt.jsio.client.impl.MetaDataName;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation may be applied to a class or method to specify a JavaScript
 * function to evaluate. The return value of the function will be used as the
 * initial backing object when constructing a JSWrapper or used as-is when
 * applied to a JSFlyweightWrapper. A JavaScript Date object could be created by
 * using the value <code>$wnd.Date</code>.
 */
@Documented
@MetaDataName("gwt.constructor")
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface Constructor {
  String value();
}
