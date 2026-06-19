package com.openreport.admin.service.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SnapshotStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotStorageService.class);

    @Autowired
    private MysqlShardStorageStrategy mysqlShardStorage;

    @Autowired
    private MysqlDirectStorageStrategy mysqlDirectStorage;

    private final Map<String, SnapshotStorageStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        registerStrategy(mysqlDirectStorage);
        registerStrategy(mysqlShardStorage);
        logger.info("快照存储策略初始化完成，已注册策略: {}", strategyMap.keySet());
    }

    public void registerStrategy(SnapshotStorageStrategy strategy) {
        strategyMap.put(strategy.getStorageType(), strategy);
    }

    public SnapshotStorageStrategy getStrategy(String storageType) {
        if (storageType == null) {
            return mysqlDirectStorage;
        }
        SnapshotStorageStrategy strategy = strategyMap.get(storageType);
        if (strategy != null && strategy.isEnabled()) {
            return strategy;
        }
        return mysqlDirectStorage;
    }

    public boolean isStorageTypeAvailable(String storageType) {
        SnapshotStorageStrategy strategy = strategyMap.get(storageType);
        return strategy != null && strategy.isEnabled();
    }

    public List<String> getAvailableStorageTypes() {
        return strategyMap.values().stream()
                .filter(SnapshotStorageStrategy::isEnabled)
                .map(SnapshotStorageStrategy::getStorageType)
                .toList();
    }
}
