package com.openreport.engine.function;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.engine.parser.CellExpressionParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
public class FunctionInitializer {

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private CellExpressionParser cellExpressionParser;

    @Autowired(required = false)
    private CustomFunctionLoader customFunctionLoader;

    @PostConstruct
    public void init() {
        BuiltinFunctions.setCellExpressionParser(cellExpressionParser);
        registerBuiltinFunctions();
        log.info("Built-in functions initialized, total: {}", functionRegistry.getAllMeta().size());
        loadCustomFunctionsOnStartup();
    }

    private void loadCustomFunctionsOnStartup() {
        if (customFunctionLoader == null) {
            log.info("No CustomFunctionLoader found, skip loading custom functions");
            return;
        }
        try {
            List<Map<String, Object>> functions = customFunctionLoader.loadEnabledCustomFunctions();
            loadCustomFunctions(functions);
            log.info("Custom functions loaded on startup, total: {}", functions != null ? functions.size() : 0);
        } catch (Exception e) {
            log.warn("Failed to load custom functions on startup, will retry later", e);
        }
    }

    private void registerBuiltinFunctions() {
        registerFunction("SUM", "SUM - 求和", "MATH",
                "对数据集字段进行求和计算",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("field").type("String").required(true).description("数据集字段引用，如 dataset.fieldName").build()
                ),
                "Number", "${SUM(sales.amount)}", BuiltinFunctions.sum());

        registerFunction("AVG", "AVG - 平均值", "MATH",
                "对数据集字段计算平均值",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("field").type("String").required(true).description("数据集字段引用，如 dataset.fieldName").build()
                ),
                "Number", "${AVG(sales.amount)}", BuiltinFunctions.avg());

        registerFunction("COUNT", "COUNT - 计数", "MATH",
                "统计数据集字段非空值的数量",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("field").type("String").required(true).description("数据集字段引用，如 dataset.fieldName").build()
                ),
                "Number", "${COUNT(sales.id)}", BuiltinFunctions.count());

        registerFunction("MAX", "MAX - 最大值", "MATH",
                "获取数据集字段的最大值",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("field").type("String").required(true).description("数据集字段引用，如 dataset.fieldName").build()
                ),
                "Number", "${MAX(sales.amount)}", BuiltinFunctions.max());

        registerFunction("MIN", "MIN - 最小值", "MATH",
                "获取数据集字段的最小值",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("field").type("String").required(true).description("数据集字段引用，如 dataset.fieldName").build()
                ),
                "Number", "${MIN(sales.amount)}", BuiltinFunctions.min());

        registerFunction("IF", "IF - 条件判断", "LOGIC",
                "根据条件返回不同的值",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("condition").type("Expression").required(true).description("条件表达式").build(),
                        FunctionMeta.FunctionParam.builder().name("trueValue").type("Object").required(true).description("条件为真时返回的值").build(),
                        FunctionMeta.FunctionParam.builder().name("falseValue").type("Object").required(false).description("条件为假时返回的值，默认为null").build()
                ),
                "Object", "${IF(amount>100, \"高\", \"低\")}", BuiltinFunctions.ifFunc());

        registerFunction("CONCAT", "CONCAT - 字符串拼接", "STRING",
                "将多个值拼接为一个字符串",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("values").type("Object[]").required(true).description("要拼接的值列表，支持多个参数").build()
                ),
                "String", "${CONCAT(firstName, \" \", lastName)}", BuiltinFunctions.concat());

        registerFunction("ROUND", "ROUND - 四舍五入", "MATH",
                "对数值进行四舍五入",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("value").type("Number").required(true).description("要四舍五入的数值").build(),
                        FunctionMeta.FunctionParam.builder().name("scale").type("Integer").required(false).description("保留小数位数，默认0").build()
                ),
                "Number", "${ROUND(3.14159, 2)}", BuiltinFunctions.round());

        registerFunction("DATE_FORMAT", "DATE_FORMAT - 日期格式化", "DATE",
                "将日期格式化为指定格式的字符串",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("date").type("Date").required(true).description("日期值").build(),
                        FunctionMeta.FunctionParam.builder().name("pattern").type("String").required(true).description("日期格式，如 yyyy-MM-dd").build()
                ),
                "String", "${DATE_FORMAT(createTime, \"yyyy-MM-dd\")}", BuiltinFunctions.dateFormat());

        registerFunction("TODAY", "TODAY - 当前日期", "DATE",
                "获取当前日期，返回 yyyy-MM-dd 格式的字符串",
                Collections.emptyList(),
                "String", "${TODAY()}", BuiltinFunctions.today());

        registerFunction("NOW", "NOW - 当前时间", "DATE",
                "获取当前日期时间，返回 yyyy-MM-dd HH:mm:ss 格式的字符串",
                Collections.emptyList(),
                "String", "${NOW()}", BuiltinFunctions.now());

        registerFunction("UPPER", "UPPER - 转大写", "STRING",
                "将字符串转换为大写",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("str").type("String").required(true).description("要转换的字符串").build()
                ),
                "String", "${UPPER(name)}", BuiltinFunctions.upper());

        registerFunction("LOWER", "LOWER - 转小写", "STRING",
                "将字符串转换为小写",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("str").type("String").required(true).description("要转换的字符串").build()
                ),
                "String", "${LOWER(name)}", BuiltinFunctions.lower());

        registerFunction("SUBSTRING", "SUBSTRING - 截取子串", "STRING",
                "截取字符串的子串",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("str").type("String").required(true).description("原字符串").build(),
                        FunctionMeta.FunctionParam.builder().name("start").type("Integer").required(true).description("起始位置（从1开始）").build(),
                        FunctionMeta.FunctionParam.builder().name("length").type("Integer").required(false).description("截取长度，默认截取到末尾").build()
                ),
                "String", "${SUBSTRING(name, 1, 3)}", BuiltinFunctions.substring());

        registerFunction("ABS", "ABS - 绝对值", "MATH",
                "计算数值的绝对值",
                Arrays.asList(
                        FunctionMeta.FunctionParam.builder().name("value").type("Number").required(true).description("数值").build()
                ),
                "Number", "${ABS(-10)}", BuiltinFunctions.abs());
    }

    private void registerFunction(String name, String label, String category, String description,
                                   List<FunctionMeta.FunctionParam> params, String returnType,
                                   String example, FunctionExecutor executor) {
        FunctionMeta meta = FunctionMeta.builder()
                .name(name)
                .label(label)
                .category(category)
                .description(description)
                .params(params)
                .returnType(returnType)
                .example(example)
                .build();
        functionRegistry.register(name, executor, meta);
    }

    public void loadCustomFunctions(List<Map<String, Object>> functions) {
        functionRegistry.clearCustomFunctions();
        if (functions == null || functions.isEmpty()) {
            return;
        }
        for (Map<String, Object> func : functions) {
            try {
                String name = (String) func.get("funcName");
                String label = (String) func.get("funcLabel");
                String category = (String) func.get("funcCategory");
                String description = (String) func.get("description");
                String paramConfig = (String) func.get("paramConfig");
                String returnType = (String) func.get("returnType");
                String example = (String) func.get("example");
                String scriptContent = (String) func.get("scriptContent");

                List<FunctionMeta.FunctionParam> params = Collections.emptyList();
                if (paramConfig != null && !paramConfig.trim().isEmpty()) {
                    params = JSON.parseObject(paramConfig, new TypeReference<List<FunctionMeta.FunctionParam>>() {});
                }

                FunctionMeta meta = FunctionMeta.builder()
                        .name(name)
                        .label(label)
                        .category(category)
                        .description(description)
                        .params(params)
                        .returnType(returnType)
                        .example(example)
                        .build();

                FunctionExecutor executor = new GroovyFunctionExecutor(scriptContent);
                functionRegistry.register(name, executor, meta);
                log.info("Loaded custom function: {}", name);
            } catch (Exception e) {
                log.error("Failed to load custom function: {}", func.get("funcName"), e);
            }
        }
    }
}
