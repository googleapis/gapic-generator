/* Copyright 2017 Google LLC
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
package com.google.api.codegen.util.py;

import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.CommentTransformer;
import com.google.api.codegen.util.LinkPattern;
import com.google.common.io.CharStreams;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;

/** Reformats a proto markdown comment to the Python reST format. */
public class PythonCommentReformatter implements CommentReformatter {

  private static final CommentTransformer TRANSFORMER =
      CommentTransformer.newBuilder()
          .transform(LinkPattern.PROTO.toFormat("`$TITLE`"))
          .transform(
              LinkPattern.RELATIVE
                  .withUrlPrefix(CommentTransformer.CLOUD_URL_PREFIX)
                  .toFormat("[$TITLE]($URL)"))
          .build();

  @Override
  public String reformat(String comment) {
    String transformedComment = TRANSFORMER.transform(comment);
    ProcessBuilder builder = new ProcessBuilder("pandoc", "-f", "markdown", "-t", "rst");
    builder.redirectErrorStream(true);
    try {
      Process process = builder.start();
      redirectFromString(process.getOutputStream(), transformedComment);
      String output = redirectToString(process.getInputStream());
      int status = process.waitFor();
      if (status != 0) {
        throw new RuntimeException(
            String.format("pandoc failed with status code %s and error%n\"%s\"", status, output));
      }

      return output.trim();
    } catch (InterruptedException e) {
      throw new RuntimeException("Could not execute pandoc", e);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not execute pandoc. Check if pandoc is in PATH.", e);
    }
  }

  private void redirectFromString(OutputStream outputStream, String input) {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
      writer.write(input);
      writer.flush();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not execute pandoc", e);
    }
  }

  private String redirectToString(InputStream inputStream) {
    try (Reader reader = new InputStreamReader(inputStream)) {
      return CharStreams.toString(reader);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not execute pandoc", e);
    }
  }
}
