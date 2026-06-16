package com.openreport.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataSourceTypeEnum {

    MYSQL("MYSQL", "MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai"),
    POSTGRESQL("POSTGRESQL", "PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s"),
    ORACLE("ORACLE", "Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@%s:%d/%s"),
    SQLSERVER("SQLSERVER", "SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%d;DatabaseName=%s"),
    DM("DM", "达梦", "dm.jdbc.driver.DmDriver", "jdbc:dm://%s:%d/%s"),
    API("API", "API接口", "", ""),
    EXCEL("EXCEL", "Excel文件", "", "");

    private final String code;
    private final String name;
    private final String driver;
    private final String urlPattern;

    public static DataSourceTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataSourceTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
