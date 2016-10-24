/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.configgen;

import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.MethodKind;
import com.google.api.tools.framework.model.Method;
import io.grpc.Status;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Config generator for retry codes and retry params. Static methods are used to generate retry
 * definitions.
 */
public class RetryGenerator implements MethodConfigGenerator {

  private static final String CONFIG_KEY_NAME = "name";

  private static final String CONFIG_KEY_RETRY_CODES_DEF = "retry_codes_def";
  private static final String CONFIG_KEY_RETRY_CODES_NAME = "retry_codes_name";
  private static final String CONFIG_KEY_RETRY_CODES = "retry_codes";
  private static final String RETRY_CODES_IDEMPOTENT_NAME = "idempotent";
  private static final String RETRY_CODES_NON_IDEMPOTENT_NAME = "non_idempotent";

  private static final String CONFIG_KEY_RETRY_PARAMS_DEF = "retry_params_def";
  private static final String CONFIG_KEY_RETRY_PARAMS_NAME = "retry_params_name";
  private static final String RETRY_PARAMS_DEFAULT_NAME = "default";

  private static List<Object> generateRetryCodes() {
    Map<String, Object> retryCodesDefault = new LinkedHashMap<>();
    retryCodesDefault.put(CONFIG_KEY_NAME, RETRY_CODES_NON_IDEMPOTENT_NAME);
    retryCodesDefault.put(CONFIG_KEY_RETRY_CODES, new LinkedList<Object>());

    Map<String, Object> retryCodesIdempotent = new LinkedHashMap<>();
    retryCodesIdempotent.put(CONFIG_KEY_NAME, RETRY_CODES_IDEMPOTENT_NAME);
    List<Object> idempotentList = new LinkedList<Object>();
    idempotentList.add(Status.Code.UNAVAILABLE.name());
    idempotentList.add(Status.Code.DEADLINE_EXCEEDED.name());
    retryCodesIdempotent.put(CONFIG_KEY_RETRY_CODES, idempotentList);

    List<Object> output = new LinkedList<Object>();
    output.add(retryCodesIdempotent);
    output.add(retryCodesDefault);

    return output;
  }

  private static List<Object> generateRetryParams() {
    Map<String, Object> retryParamsDefault = new LinkedHashMap<String, Object>();
    retryParamsDefault.put(CONFIG_KEY_NAME, RETRY_PARAMS_DEFAULT_NAME);
    retryParamsDefault.put("initial_retry_delay_millis", 100);
    retryParamsDefault.put("retry_delay_multiplier", 1.3);
    retryParamsDefault.put("max_retry_delay_millis", 60000);
    retryParamsDefault.put("initial_rpc_timeout_millis", 20000);
    retryParamsDefault.put("rpc_timeout_multiplier", 1);
    retryParamsDefault.put("max_rpc_timeout_millis", 20000);
    retryParamsDefault.put("total_timeout_millis", 600000);

    List<Object> output = new LinkedList<Object>();
    output.add(new LinkedHashMap<String, Object>(retryParamsDefault));
    return output;
  }

  public static Map<String, Object> generateRetryDefinitions() {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    result.put(CONFIG_KEY_RETRY_CODES_DEF, generateRetryCodes());
    result.put(CONFIG_KEY_RETRY_PARAMS_DEF, generateRetryParams());
    return result;
  }

  @Override
  public Map<String, Object> generate(Method method) {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    if (isIdempotent(method)) {
      result.put(CONFIG_KEY_RETRY_CODES_NAME, RETRY_CODES_IDEMPOTENT_NAME);
    } else {
      result.put(CONFIG_KEY_RETRY_CODES_NAME, RETRY_CODES_NON_IDEMPOTENT_NAME);
    }
    result.put(CONFIG_KEY_RETRY_PARAMS_NAME, RETRY_PARAMS_DEFAULT_NAME);
    return result;
  }

  /**
   * Returns true if the method is idempotent according to the http method kind (GET, PUT, DELETE).
   */
  private static boolean isIdempotent(Method method) {
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    if (httpAttr == null) {
      return false;
    }
    MethodKind methodKind = httpAttr.getMethodKind();
    return methodKind.isIdempotent();
  }
}
