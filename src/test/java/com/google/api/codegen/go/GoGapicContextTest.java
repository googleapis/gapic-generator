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
package com.google.api.codegen.go;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;

import io.grpc.Status;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.TreeSet;

public class GoGapicContextTest {
  @Test
  public void testGetImportsWithRetry() {
    Interface service = Mockito.mock(Interface.class);
    ApiConfig apiConf = Mockito.mock(ApiConfig.class);
    InterfaceConfig serviceConf = Mockito.mock(InterfaceConfig.class);
    GoGapicContext context = Mockito.mock(GoGapicContext.class);

    TreeSet<GoGapicContext.RetryConfigName> retryConfigNames = new TreeSet<>();
    retryConfigNames.add(GoGapicContext.RetryConfigName.create("default", "idempotent"));

    Mockito.when(context.getRetryConfigNames(service)).thenReturn(retryConfigNames);
    Mockito.when(apiConf.getInterfaceConfig(service)).thenReturn(serviceConf);
    Mockito.when(context.getApiConfig()).thenReturn(apiConf);
    Mockito.when(context.getImports(service)).thenCallRealMethod();
    Mockito.when(serviceConf.getRetryCodesDefinition())
        .thenReturn(ImmutableMap.of("idempotent", ImmutableSet.of(Status.Code.UNAVAILABLE)));

    Truth.assertThat(context.getImports(service))
        .containsAllOf("    \"time\"", "    \"google.golang.org/grpc/codes\"");
  }

  @Test
  public void testGetImportsNoRetry() {
    Interface service = Mockito.mock(Interface.class);
    ApiConfig apiConf = Mockito.mock(ApiConfig.class);
    InterfaceConfig serviceConf = Mockito.mock(InterfaceConfig.class);
    GoGapicContext context = Mockito.mock(GoGapicContext.class);

    Mockito.when(context.getRetryConfigNames(service))
        .thenReturn(new TreeSet<GoGapicContext.RetryConfigName>());
    Mockito.when(apiConf.getInterfaceConfig(service)).thenReturn(serviceConf);
    Mockito.when(context.getApiConfig()).thenReturn(apiConf);
    Mockito.when(context.getImports(service)).thenCallRealMethod();
    Mockito.when(serviceConf.getRetryCodesDefinition())
        .thenReturn(ImmutableMap.of("idempotent", ImmutableSet.of(Status.Code.UNAVAILABLE)));

    Truth.assertThat(context.getImports(service))
        .containsNoneOf("    \"time\"", "    \"google.golang.org/grpc/codes\"");
  }
}
