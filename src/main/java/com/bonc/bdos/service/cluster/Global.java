package com.bonc.bdos.service.cluster;

import java.util.Collection;
import java.util.HashMap;

import com.bonc.bdos.service.cluster.entity.SysClusterInfo;

public abstract class Global {

    // 集群信息配置集合
    private static final HashMap<String, SysClusterInfo> GLOBAL = new HashMap<>();

    private static final String HARBOR_ENDPOINT = "";

    public static final char READ_ONLY = '0';
    public static final char INNER_SET = '1';
    public static final char OUTER_SET = '2';

    public abstract void init();

    protected static void loadCfg(Collection<SysClusterInfo> cfgList) {
        for (SysClusterInfo cfg : cfgList) {
            GLOBAL.put(cfg.getCfgKey(), cfg);
        }
    }

    protected static HashMap<String, String> getCfgMap(char status) {
        HashMap<String, String> cfgMap = new HashMap<>();
        for (SysClusterInfo cfg : GLOBAL.values()) {
            if (cfg.isEnable(status)) {
                cfgMap.put(cfg.getCfgKey(), cfg.getCfgValue());
            }
        }
        return cfgMap;
    }

    public static HashMap<String, String> getTotal() {
        HashMap<String, String> all = new HashMap<>();
        for (SysClusterInfo cfg : GLOBAL.values()) {
            all.put(cfg.getCfgKey(), cfg.getCfgValue());
        }
        return all;
    }

    protected static SysClusterInfo getEntity(String key, char cfgType) {
        SysClusterInfo cfg = Global.GLOBAL.get(key);
//        if (cfg != null && cfg.isEnable(cfgType)) {
        if (cfg != null ) {
            return cfg;
        } else {
            return null;
        }
    }

    public static void updateGlobal(SysClusterInfo cfg) {
        GLOBAL.put(cfg.getCfgKey(), cfg);
    }

    /**
     * 获取 ansible 安装包路径
     * 
     * @return WORK_DIR_ANSIBLE
     */
    public static String getAnsibleDir() {
        return Global.GLOBAL.get("WORK_DIR_ANSIBLE").getCfgValue();
    }

    /**
     * 根据key获取配置信息
     * 
     * @param key
     * @return
     */
    public static String getConfig(String key) {
        return Global.GLOBAL.get(key).getCfgValue();
    }

    public static boolean k8sIsInstalled() {
        return false;
    }
}
