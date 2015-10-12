package io.gapi.fx.snippet;

import io.gapi.fx.snippet.Doc.GroupKind;
import com.google.auto.value.AutoValue;

/**
 * Represents layout information
 */
@AutoValue
abstract class Layout {

  static final Layout DEFAULT = create(Doc.BREAK, GroupKind.VERTICAL, 0);

  abstract Doc separator();
  abstract Doc.GroupKind groupKind();
  abstract int nest();

  static Layout create(Doc separator, Doc.GroupKind groupKind, int nest) {
    return new AutoValue_Layout(separator, groupKind, nest);
  }
}
