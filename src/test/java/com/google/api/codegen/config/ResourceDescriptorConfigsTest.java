/* Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.api.ResourceDescriptor;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.FieldSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.LiteralSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.PathSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.WildcardSegment;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.ImmutableList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ResourceDescriptorConfigsTest {
  @Mock private static ProtoFile protoFile;
  @Mock private static Interface service;
  @Mock private static Method getIamPolicyMethod;
  @Mock private static Method setIamPolicyMethod;
  @Mock private static Method testIamPermissionsMethod;

  @Mock private static HttpAttribute getMethodAttr;
  @Mock private static HttpAttribute getMethodAdditionalAttr;
  @Mock private static HttpAttribute setMethodAttr;
  @Mock private static HttpAttribute setMethodAdditionalAttr;
  @Mock private static HttpAttribute testMethodAttr;
  @Mock private static HttpAttribute testMethodAdditionalAttr;

  private ImmutableList<PathSegment> setMethodBookUri;
  private ImmutableList<PathSegment> getMethodBookUri;
  private ImmutableList<PathSegment> testMethodBookUri;
  private ImmutableList<PathSegment> getMethodShelfUri;
  private ImmutableList<PathSegment> setMethodShelfUri;
  private ImmutableList<PathSegment> testMethodShelfUri;

  @Before
  public void setUp() {

    MockitoAnnotations.initMocks(this);
    when(protoFile.getInterfaces()).thenReturn(ImmutableList.of(service));
    when(service.getMethods())
        .thenReturn(
            ImmutableList.of(getIamPolicyMethod, setIamPolicyMethod, testIamPermissionsMethod));
    when(getIamPolicyMethod.getSimpleName()).thenReturn("GetIamPolicy");
    when(setIamPolicyMethod.getSimpleName()).thenReturn("SetIamPolicy");
    when(testIamPermissionsMethod.getSimpleName()).thenReturn("TestIamPermissions");
    when(getIamPolicyMethod.getAttribute(HttpAttribute.KEY)).thenReturn(getMethodAttr);
    when(setIamPolicyMethod.getAttribute(HttpAttribute.KEY)).thenReturn(setMethodAttr);
    when(testIamPermissionsMethod.getAttribute(HttpAttribute.KEY)).thenReturn(testMethodAttr);

    // v2/resource="shelves/*":setIamPolicy
    setMethodShelfUri = createSegments(setIamPolicyMethod, Resource.SHELF);
    // v2/resource="shelves/*":setIamPolicy
    getMethodShelfUri = createSegments(getIamPolicyMethod, Resource.SHELF);
    // v2/resource="shelves/*":testIamPermissions
    testMethodShelfUri = createSegments(testIamPermissionsMethod, Resource.SHELF);
    // v2/resource="shelves/*/books/*":setIamPolicy
    setMethodBookUri = createSegments(setIamPolicyMethod, Resource.BOOK);
    // v2/resource="shelves/*/books/*":getIamPolicy
    getMethodBookUri = createSegments(getIamPolicyMethod, Resource.BOOK);
    // v2/resource="shelves/*/books/*":testIamPermissions
    testMethodBookUri = createSegments(testIamPermissionsMethod, Resource.BOOK);

    when(getMethodAttr.getPath()).thenReturn(getMethodShelfUri);
    when(setMethodAttr.getPath()).thenReturn(setMethodShelfUri);
    when(testMethodAttr.getPath()).thenReturn(testMethodShelfUri);
    when(getMethodAdditionalAttr.getPath()).thenReturn(getMethodBookUri);
    when(setMethodAdditionalAttr.getPath()).thenReturn(setMethodBookUri);
    when(testMethodAdditionalAttr.getPath()).thenReturn(testMethodBookUri);
  }

  @Test
  public void testCreateIamResourceDescriptorConfigsWithoutAdditionalBindings() {
    when(getMethodAttr.getAdditionalBindings()).thenReturn(ImmutableList.of());
    when(setMethodAttr.getAdditionalBindings()).thenReturn(ImmutableList.of());
    when(testMethodAttr.getAdditionalBindings()).thenReturn(ImmutableList.of());

    Map<String, ResourceDescriptorConfig> configs =
        ResourceDescriptorConfigs.createResourceNameDescriptorsFromIamMethods(
            protoFile, service, "foo.googleapis.com");
    assertThat(configs.size()).isEqualTo(1);
    System.out.println(configs.size());
    configs.keySet().stream().forEach(System.out::println);
    ResourceDescriptorConfig config = configs.get("foo.googleapis.com/Shelf");
    assertThat(config.getUnifiedResourceType()).isEqualTo("foo.googleapis.com/Shelf");
    assertThat(config.getPatterns()).containsExactly("shelves/{shelf}");
    assertThat(config.getNameField()).isEqualTo("resource");
    assertThat(config.getHistory()).isEqualTo(ResourceDescriptor.History.FUTURE_MULTI_PATTERN);
    assertThat(config.getAssignedProtoFile()).isEqualTo(protoFile);
  }

  @Test
  public void testCreateIamResourceDescriptorConfigsWithAdditionalBindings() {
    when(getMethodAttr.getAdditionalBindings())
        .thenReturn(ImmutableList.of(getMethodAdditionalAttr));
    when(setMethodAttr.getAdditionalBindings())
        .thenReturn(ImmutableList.of(setMethodAdditionalAttr));
    when(testMethodAttr.getAdditionalBindings())
        .thenReturn(ImmutableList.of(testMethodAdditionalAttr));

    Map<String, ResourceDescriptorConfig> configs =
        ResourceDescriptorConfigs.createResourceNameDescriptorsFromIamMethods(
            protoFile, service, "foo.googleapis.com");
    assertThat(configs.size()).isEqualTo(1);

    ResourceDescriptorConfig config = configs.get("foo.googleapis.com/IamResource");
    assertThat(config.getUnifiedResourceType()).isEqualTo("foo.googleapis.com/IamResource");
    assertThat(config.getPatterns())
        .containsExactly("shelves/{shelf}/books/{book}", "shelves/{shelf}");
    assertThat(config.getNameField()).isEqualTo("resource");
    assertThat(config.getHistory()).isEqualTo(ResourceDescriptor.History.FUTURE_MULTI_PATTERN);
    assertThat(config.getAssignedProtoFile()).isEqualTo(protoFile);
  }

  private static ImmutableList<PathSegment> createSegments(Method method, Resource type) {
    switch (type) {
      case BOOK:
        return ImmutableList.<PathSegment>of(
            new LiteralSegment("v2"),
            new FieldSegment(
                "resource",
                ImmutableList.of(
                    new LiteralSegment("shelves"),
                    new WildcardSegment(false),
                    new LiteralSegment("books"),
                    new WildcardSegment(false))),
            new LiteralSegment(method.getSimpleName()));
      case SHELF:
        return ImmutableList.of(
            new LiteralSegment("v2"),
            new FieldSegment(
                "resource",
                ImmutableList.of(new LiteralSegment("shelves"), new WildcardSegment(false))),
            new LiteralSegment("setIamPolicy"));
      default:
        throw new IllegalArgumentException("unknown segment type.");
    }
  }

  private static enum Resource {
    SHELF,
    BOOK
  }
}
