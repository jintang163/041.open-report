package com.openreport.engine.function;

import java.util.List;
import java.util.Map;

public interface FunctionExecutor {

    Object execute(List<Object> args, Map<String, List<Map<String, Object>>> dataSets,
                   int currentRow, Map<String, Object> parameters);
}
