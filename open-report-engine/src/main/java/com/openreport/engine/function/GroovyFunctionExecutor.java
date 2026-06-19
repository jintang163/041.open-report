package com.openreport.engine.function;

import com.openreport.common.exception.BusinessException;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GroovyFunctionExecutor implements FunctionExecutor {

    private static final GroovyClassLoader GROOVY_CLASS_LOADER = new GroovyClassLoader();

    private static final ConcurrentHashMap<String, Class<?>> SCRIPT_CACHE = new ConcurrentHashMap<>();

    private final String scriptContent;

    public GroovyFunctionExecutor(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    @Override
    public Object execute(List<Object> args, Map<String, List<Map<String, Object>>> dataSets,
                          int currentRow, Map<String, Object> parameters) {
        try {
            String cacheKey = Integer.toHexString(scriptContent.hashCode());
            Class<?> scriptClass = SCRIPT_CACHE.computeIfAbsent(cacheKey, k -> {
                String wrappedScript = wrapScript(scriptContent);
                return GROOVY_CLASS_LOADER.parseClass(wrappedScript);
            });

            Script script = (Script) scriptClass.getDeclaredConstructor().newInstance();
            Binding binding = new Binding();
            binding.setVariable("args", args);
            binding.setVariable("dataSets", dataSets);
            binding.setVariable("currentRow", currentRow);
            binding.setVariable("parameters", parameters);
            script.setBinding(binding);
            return script.run();
        } catch (Exception e) {
            log.error("Groovy script execution failed", e);
            throw new BusinessException("自定义函数执行失败: " + e.getMessage());
        }
    }

    private String wrapScript(String userScript) {
        return "import java.util.*;\n" +
                "import com.openreport.engine.parser.CellExpressionParser;\n" +
                "def call() {\n" +
                userScript + "\n" +
                "}\n" +
                "return call()";
    }

    public static void validateScript(String scriptContent) {
        try {
            String wrapped = "import java.util.*;\ndef call() {\n" + scriptContent + "\n}\nreturn call()";
            GROOVY_CLASS_LOADER.parseClass(wrapped);
        } catch (Exception e) {
            throw new BusinessException("Groovy脚本语法错误: " + e.getMessage());
        }
    }

    public static void clearCache() {
        SCRIPT_CACHE.clear();
    }
}
