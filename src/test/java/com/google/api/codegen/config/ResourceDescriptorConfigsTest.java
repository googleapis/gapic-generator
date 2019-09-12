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

// import static com.google.common.truth.Truth.assertThat;
// import static org.mockito.Mockito.when;

// import com.google.api.ResourceDescriptor;
// import com.google.api.codegen.configgen.CollectionPattern;
// import com.google.api.tools.framework.model.Interface;
// import com.google.api.tools.framework.model.Method;
// import com.google.api.tools.framework.model.ProtoFile;
// import com.google.common.collect.ImmutableList;
// import com.google.common.collect.ImmutableSet;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import org.junit.Test;
// import org.mockito.Mockito;
// import org.mockito.Mock;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ResourceDescriptorConfigsTest {
  @Mock private static ProtoFile protoFile;
  @Mock private static Interface apiInterface;
  @Mock private static Method getIamPolicyMethod;
  @Mock private static Method setIamPolicyMethod;
  @Mock private static Method testIamPermissionsMethod;

  @Mock private static HttpAttribute getMethodAttr;
  @Mock private static HttpAttribute getMethodAdditionalAttr;
  @Mock private static HttpAttribute setMethodAttr;
  @Mock private static HttpAttribute setMethodAdditionalAttr;
  @Mock private static HttpAttribute testMethodAttr;
  @Mock private static HttpAttribute testMethodAdditionalAttr;

  private final ImmutableList<PathSegment>
      setMethodBook; // v2/resource="shelves/*/books/*":setIamPolicy
  private final ImmutableList<PathSegment>
      getMethodBook; // v2/resource="shelves/*/books/*":getIamPolicy
  private final ImmutableList<PathSegment>
      testMethodBook; // v2/resource="shelves/*/books/*":testIamPermissions
  private final ImmutableList<PathSegment> getMethodShelf; // v2/resource="shelves/*":getIamPolicy
  private final ImmutableList<PathSegment> setMethodShelf; // v2/resource="shelves/*":setIamPolicy
  private final ImmutableList<PathSegment>
      testMethodShelf; // v2/resource="shelves/*":testIamPermissions

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(protoFile.getInterfaces()).thenReturn(ImmutableList.of(apiInterface));
    when(apiInterface.getMethods())
        .thenReturn(
            ImmutableList.of(getIamPolicyMethod, setIamPolicyMethod, testIamPermissionsMethod));
    when(getIamPolicyMethod.getSimpleName()).thenReturn("GetIamPolicy");
    when(setIamPolicyMethod.getSimpleName()).thenReturn("SetIamPolicy");
    when(testIamPermissionsMethod.getSimpleName()).thenReturn("TestIamPermissions");
    when(getIamPolicyMethod.getAttribute(HttpAttribute.KEY)).thenReturn(getMethodAttr);
    when(setIamPolicyMethod.getAttribute(HttpAttribute.KEY)).thenReturn(setMethodAttr);
    when(testIamPolicyMethod.getAttribute(HttpAttribute.KEY)).thenReturn(testMethodAttr);

    when(getMethodAttr.getPath()).thenReturn(getMethodShelf);
    when(setMethodAttr.getPath()).thenReturn(setMethodShelf);
    when(testMethodAttr.getPath()).thenReturn(testMethodShelf);
    when(getMethodAdditionalAttr.getPath()).thenReturn(getMethodBook);
    when(setMethodAdditionalAttr.getPath()).thenReturn(setMethodBook);
    when(testMethodAdditionalAttr.getPath()).thenReturn(testMethodBook);
  }

  @Test
  public void testCreateIamResourceDescriptorConfigsWithoutAdditionalBindings() {
    Map<String, ResourceDescriptorConfig> configs =
        ResourceDescriptorConfigs.createResourceNameDescriptorsFromIamMethods(protoFile);
    assertThat(configs.size()).isEqualTo(1);

    ResourceDescriptorConfig config = configs.get("*/Book");
    assertThat(config.getUnifiedResourceType()).isEqualTo("*/Book");
    assertThat(config.getPatterns()).containsExactly("shelves/{shelf}/books/{book}");
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
        ResourceDescriptorConfigs.createResourceNameDescriptorsFromIamMethods(protoFile);
    assertThat(configs.size()).isEqualTo(1);

    ResourceDescriptorConfig config = configs.get("*/IamResource");
    assertThat(config.getUnifiedResourceType()).isEqualTo("*/IamResource");
    assertThat(config.getPatterns())
        .containsExactly("shelves/{shelf}/books/{book}", "shelves/{shelf}");
    assertThat(config.getNameField()).isEqualTo("resource");
    assertThat(config.getHistory()).isEqualTo(ResourceDescriptor.History.FUTURE_MULTI_PATTERN);
    assertThat(config.getAssignedProtoFile()).isEqualTo(protoFile);
  }

  @Test
  public void testMultipleIamMethodsWithDifferentURIsFail() {
    when(getMethodAttr.getAdditionalBindings())
        .thenReturn(ImmutableList.of(getMethodAdditionalAttr));

    try {
      Map<String, ResourceDescriptorConfig> configs =
          ResourceDescriptorConfigs.createResourceNameDescriptorsFromIamMethods(protoFile);
      fail();
    } catch (IllegalArgumentException expection) {

    }
  }

  private static ImmutableList<FieldSegement> createSegments(Method method, SegmentType type) {
    switch (type) {
      case BOOK:
        return ImmutableList.of(
            new LiteralSegment("v2"),
            new FieldSegement(
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
            new FieldSegement(
                "resource",
                ImmutableList.of(new LiteralSegment("shelves"), new WildcardSegment(false))),
            new LiteralSegment("setIamPolicy"));
      default:
        throw new IllegalArgumentException("unknown segment type.");
    }
  }

  private static enum SegmentType {
    SHELF,
    BOOK
  }
}
