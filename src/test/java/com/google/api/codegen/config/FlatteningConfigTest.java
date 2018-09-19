package com.google.api.codegen.config;

import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Method;
import org.junit.Test;
import org.mockito.Mockito;

public class FlatteningConfigTest {
  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static final Method httpGetMethod = Mockito.mock(Method.class);
  private static final Method cancelledMethod = Mockito.mock(Method.class);

  @Test
  public void testCreateFlatteningFromProtoFile() {

    DiagCollector diagCollector = new BoundedDiagCollector();
    FlatteningConfig flatteningConfig = FlatteningConfig.createFlattening(diagCollector)
  }

  @Test
  public void testCreateFlatteningFromProtoFileAndGapicConfig() {

    MethodConfig.createFlattening();
    DiagCollector diagCollector = new BoundedDiagCollector();
    FlatteningConfig flatteningConfig = FlatteningConfig.createFlattening(diagCollector)
  }
}
