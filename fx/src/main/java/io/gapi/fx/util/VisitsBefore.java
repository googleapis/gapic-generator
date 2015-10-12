// Copyright 2012 Google Inc. All Rights Reserved.

package io.gapi.fx.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking a visitor method which is called before the main
 * visitor is.
 *
 * @see GenericVisitor
 * @author wgg@google.com (Wolfgang Grieskamp)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VisitsBefore {}
