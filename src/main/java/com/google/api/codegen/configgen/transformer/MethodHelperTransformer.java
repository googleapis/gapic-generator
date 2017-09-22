package com.google.api.codegen.configgen.transformer;

import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.configgen.PagingParameters;
import com.google.api.codegen.configgen.viewmodel.PageStreamingResponseView;
import javax.annotation.Nullable;

/** A transformer that does implementations that vary by API source. */
public interface MethodHelperTransformer {

  PagingParameters getPagingParameters();

  /** Get the ResourceNameTreatment for a method. */
  @Nullable
  ResourceNameTreatment getResourceNameTreatment(MethodModel methodModel);

  /** Make the page streaming response view for a method. */
  @Nullable
  PageStreamingResponseView generatePageStreamingResponse(MethodModel method);
}
