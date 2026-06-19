package com.openreport.engine.function;

import com.openreport.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FunctionRegistry {

    private final ConcurrentHashMap<String, FunctionExecutor> executorMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, com.openreport.engine.function.FunctionMeta> metaMap = new ConcurrentHashMap<>();

    public void register(String functionName, FunctionExecutor executor) {
        register(functionName, executor, null);
    }

    public void register(String functionName, FunctionExecutor executor, com.openreport.engine.function.FunctionMeta meta) {
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Function name cannot be empty");
        }
        String key = functionName.toLowerCase();
        executorMap.put(key, executor);
        if (meta != null) {
            metaMap.put(key, meta);
        }
        log.info("Registered function: {}", functionName);
    }

    public void unregister(String functionName) {
        if (functionName == null) return;
        String key = functionName.toLowerCase();
        executorMap.remove(key);
        metaMap.remove(key);
        log.info("Unregistered function: {}", functionName);
    }

    public boolean isRegistered(String functionName) {
        if (functionName == null) return false;
        return executorMap.containsKey(functionName.toLowerCase());
    }

    public Object execute(String functionName, List<Object> args,
                          Map<String, List<Map<String, Object>>> dataSets,
                          int currentRow, Map<String, Object> parameters) {
        if (functionName == null) {
            throw new BusinessException("函数名不能为空");
        }
        String key = functionName.toLowerCase();
        FunctionExecutor executor = executorMap.get(key);
        if (executor == null) {
            throw new BusinessException("不支持的函数: " + functionName);
        }
        return executor.execute(args, dataSets, currentRow, parameters);
    }

    public com.openreport.engine.function.FunctionMeta getMeta(String functionName) {
        if (functionName == null) return null;
        return metaMap.get(functionName.toLowerCase());
    }

    public Map<String, com.openreport.engine.function.FunctionMeta> getAllMeta() {
        return new ConcurrentHashMap<>(metaMap);
    }

    public void clearCustomFunctions() {
        List<String> customKeys = new ArrayList<>();
        for (Map.Entry<String, com.openreport.engine.function.FunctionMeta> entry : metaMap.entrySet()) {
            if ("CUSTOM".equals(entry.getValue().getCategory())) {
                customKeys.add(entry.getKey());
            }
        }
        for (String key : customKeys) {
            executorMap.remove(key);
            metaMap.remove(key);
            log.info("Cleared custom function: {}", key);
        }
    }

    public void clearAll() {
        executorMap.clear();
        metaMap.clear();
    }
}
