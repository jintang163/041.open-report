package com.openreport.engine.function;

import java.util.List;
import java.util.Map;

public interface CustomFunctionLoader {
    List<Map<String, Object>> loadEnabledCustomFunctions();
}
