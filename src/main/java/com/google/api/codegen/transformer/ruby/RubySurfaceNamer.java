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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.ruby.RubyCommentReformatter;
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** The SurfaceNamer for Ruby. */
public class RubySurfaceNamer extends SurfaceNamer {
  public RubySurfaceNamer(String packageName) {
    super(
        new RubyNameFormatter(),
        new ModelTypeFormatterImpl(new RubyModelTypeNameConverter(packageName)),
        new RubyTypeTable(packageName),
        new RubyCommentReformatter(),
        packageName);
  }

  @Override
  /** The name of the class that implements snippets for a particular proto interface. */
  public String getApiSnippetsClassName(Interface interfaze) {
    return publicClassName(Name.upperCamel(interfaze.getSimpleName(), "ClientSnippets"));
  }

  /** The function name to set a field having the given type and name. */
  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    return publicMethodName(identifier);
  }

  /** The function name to format the entity for the given collection. */
  @Override
  public String getFormatFunctionName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return staticFunctionName(Name.from(resourceNameConfig.getEntityName(), "path"));
  }

  /**
   * The type name of the Grpc client class. This needs to match what Grpc generates for the
   * particular language.
   */
  @Override
  public String getGrpcClientTypeName(Interface service) {
    return getModelTypeFormatter().getFullNameFor(service);
  }

  @Override
  public String getFullyQualifiedStubType(Interface service) {
    NamePath namePath =
        getTypeNameConverter().getNamePath(getModelTypeFormatter().getFullNameFor(service));
    return qualifiedName(namePath.append("Stub"));
  }

  /** The file name for an API service. */
  @Override
  public String getServiceFileName(Interface service) {
    String[] names = getPackageName().split("::");
    List<String> newNames = new ArrayList<>();
    for (String name : names) {
      newNames.add(packageFilePathPiece(Name.upperCamel(name)));
    }
    newNames.add(classFileNameBase(Name.upperCamel(getApiWrapperClassName(service))));
    return Joiner.on("/").join(newNames.toArray());
  }

  @Override
  public String getSourceFilePath(String path, String publicClassName) {
    return path + File.separator + Name.upperCamel(publicClassName).toLowerUnderscore() + ".rb";
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(Interface service) {
    return getPackageName() + "::" + getApiWrapperClassName(service);
  }

  @Override
  public String getServiceFileImportName(String filename) {
    return filename.replace(".proto", "_services_pb");
  }

  @Override
  public String getProtoFileImportName(String filename) {
    return filename.replace(".proto", "_pb");
  }

  @Override
  public String injectRandomStringGeneratorCode(String randomString) {
    String delimiter = ",";
    String[] split =
        CommonRenderingUtil.stripQuotes(randomString)
            .replace(
                InitFieldConfig.RANDOM_TOKEN, delimiter + InitFieldConfig.RANDOM_TOKEN + delimiter)
            .split(delimiter);
    ArrayList<String> stringParts = new ArrayList<>();
    for (String token : split) {
      if (token.length() > 0) {
        if (token.equals(InitFieldConfig.RANDOM_TOKEN)) {
          stringParts.add("Time.new.to_i.to_s");
        } else {
          stringParts.add("\"" + token + "\"");
        }
      }
    }
    return Joiner.on(" + ").join(stringParts);
  }
}
