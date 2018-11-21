/* Copyright 2016 Google LLC
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
package com.google.api.codegen.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class NameTest {

  @Test
  public void testEmpty() {
    Name name = Name.from();
    assertThat(name.toLowerUnderscore()).isEqualTo("");
    assertThat(name.toUpperUnderscore()).isEqualTo("");
    assertThat(name.toLowerCamel()).isEqualTo("");
    assertThat(name.toUpperCamel()).isEqualTo("");
  }

  @Test
  public void testSingleWord() {
    Name name = Name.from("dog");
    assertThat(name.toLowerUnderscore()).isEqualTo("dog");
    assertThat(name.toUpperUnderscore()).isEqualTo("DOG");
    assertThat(name.toLowerCamel()).isEqualTo("dog");
    assertThat(name.toUpperCamel()).isEqualTo("Dog");
  }

  @Test
  public void testMultipleWords() {
    Name name = Name.from("factory_decorator", "delegate_impl");
    assertThat(name.toLowerUnderscore()).isEqualTo("factory_decorator_delegate_impl");
    assertThat(name.toUpperUnderscore()).isEqualTo("FACTORY_DECORATOR_DELEGATE_IMPL");
    assertThat(name.toLowerCamel()).isEqualTo("factoryDecoratorDelegateImpl");
    assertThat(name.toUpperCamel()).isEqualTo("FactoryDecoratorDelegateImpl");
  }

  @Test
  public void testFromLowerCamel() {
    Name name = Name.lowerCamel("factoryDecorator", "delegateImpl");
    assertThat(name.toLowerUnderscore()).isEqualTo("factory_decorator_delegate_impl");
    assertThat(name.toUpperUnderscore()).isEqualTo("FACTORY_DECORATOR_DELEGATE_IMPL");
    assertThat(name.toLowerCamel()).isEqualTo("factoryDecoratorDelegateImpl");
    assertThat(name.toUpperCamel()).isEqualTo("FactoryDecoratorDelegateImpl");
  }

  @Test
  public void testFromUpperCamel() {
    Name name = Name.upperCamel("FactoryDecorator", "DelegateImpl");
    assertThat(name.toLowerUnderscore()).isEqualTo("factory_decorator_delegate_impl");
    assertThat(name.toUpperUnderscore()).isEqualTo("FACTORY_DECORATOR_DELEGATE_IMPL");
    assertThat(name.toLowerCamel()).isEqualTo("factoryDecoratorDelegateImpl");
    assertThat(name.toUpperCamel()).isEqualTo("FactoryDecoratorDelegateImpl");
  }

  @Test
  public void testWordAndNumber() {
    Name name = Name.from("dog", "2");
    assertThat(name.toLowerUnderscore()).isEqualTo("dog_2");
    assertThat(name.toUpperUnderscore()).isEqualTo("DOG_2");
    assertThat(name.toLowerCamel()).isEqualTo("dog2");
    assertThat(name.toUpperCamel()).isEqualTo("Dog2");
  }

  @Test
  public void testUpperWordAndNumber() {
    Name name = Name.upperCamel("Dog", "V2");
    assertThat(name.toLowerUnderscore()).isEqualTo("dog_v2");
    assertThat(name.toUpperUnderscore()).isEqualTo("DOG_V2");
    assertThat(name.toLowerCamel()).isEqualTo("dogV2");
    assertThat(name.toUpperCamel()).isEqualTo("DogV2");
  }

  @Test
  public void testLowerWordAndNumber() {
    Name name = Name.lowerCamel("dog", "v2");
    assertThat(name.toLowerUnderscore()).isEqualTo("dog_v2");
    assertThat(name.toUpperUnderscore()).isEqualTo("DOG_V2");
    assertThat(name.toLowerCamel()).isEqualTo("dogV2");
    assertThat(name.toUpperCamel()).isEqualTo("DogV2");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalLowerUnderscore() {
    Name.from("factoryDecorator");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalUpperUnderscore() {
    Name.upperCamel("factory_decorator");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalLowerCamel() {
    Name.lowerCamel("FactoryDecorator");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalUpperCamel() {
    Name.upperCamel("factoryDecorator");
  }

  @Test
  public void separateAcronyms() {
    Name name = Name.upperCamel("IAM", "HTTP", "XML", "Dog");
    assertThat(name.toLowerUnderscore()).isEqualTo("iam_http_xml_dog");
    assertThat(name.toUpperUnderscore()).isEqualTo("IAM_HTTP_XML_DOG");
    assertThat(name.toLowerCamel()).isEqualTo("iamHttpXmlDog");
    assertThat(name.toUpperCamel()).isEqualTo("IamHttpXmlDog");
  }

  @Test
  public void combinedAcronyms() {
    Name name = Name.upperCamel("IAMHTTPXML");
    assertThat(name.toLowerUnderscore()).isEqualTo("iam_http_xml");
    assertThat(name.toUpperUnderscore()).isEqualTo("IAM_HTTP_XML");
    assertThat(name.toLowerCamel()).isEqualTo("iamHttpXml");
    assertThat(name.toUpperCamel()).isEqualTo("IamHttpXml");
  }

  @Test(expected = IllegalArgumentException.class)
  public void ambiguousAcronym() {
    System.out.println(Name.upperCamel("APIAMName").toLowerUnderscore());
  }

  @Test(expected = IllegalArgumentException.class)
  public void ambiguousRepeatedAcronym() {
    System.out.println(Name.upperCamel("APIDogAPIAMName").toLowerUnderscore());
  }

  @Test
  public void upperCamelUpperAcronymsSeparate() {
    Name name = Name.upperCamelKeepUpperAcronyms("IAM", "HTTP", "XML", "Dog");
    assertThat(name.toLowerUnderscore()).isEqualTo("iam_http_xml_dog");
    assertThat(name.toUpperUnderscore()).isEqualTo("IAM_HTTP_XML_DOG");
    assertThat(name.toLowerCamel()).isEqualTo("iamHTTPXMLDog");
    assertThat(name.toUpperCamel()).isEqualTo("IAMHTTPXMLDog");
  }

  @Test
  public void upperCamelUpperAcronymsCombined() {
    Name name = Name.upperCamelKeepUpperAcronyms("IAMHTTPXML");
    assertThat(name.toLowerUnderscore()).isEqualTo("iam_http_xml");
    assertThat(name.toUpperUnderscore()).isEqualTo("IAM_HTTP_XML");
    assertThat(name.toLowerCamel()).isEqualTo("iamHTTPXML");
    assertThat(name.toUpperCamel()).isEqualTo("IAMHTTPXML");
  }
}
