/**
 * 主要完成 主机角色策略的生成，角色变更后磁盘的分配
 *
 * @constructor
 */

var Policy = function (table) {
    var $vip = $("#k8s_virtual_ip");

    // 可选择的角色节点
    var roles_show = [];
    var roles_base = [];
    var roles_extend = [];
    var global = undefined;
    var _this = this;
    var _table = table;
    var store_min = {};
    // 存储每个角色对应的主机
    var roles_all = [];

    var init = function () {
        $.ajax({
            url:  "v1/global",
            async: false,
            success: function (data) {
                if (data.code===200){
                    global = data.data;
                }else{
                    console.log("全局配置加载失败！")
                }
            }
        });

        $.ajax({
            url:  "v1/roles_cfg",
            async: false,
            success: function (data) {
                if (data.code===200){
                    for (let role of data.data){
                        switch (role.roleType) {
                            case '0':
                                roles_base.push(role.roleCode);
                                break;
                            case '1':
                                roles_show.push(role.roleCode);
                                roles_extend.push(role);
                                break;
                            case '2':
                                roles_extend.push(role);
                                break;
                        }
                        roles_all[roles_all.length] = role.roleCode;
                    }
                }else{
                    console.log("角色配置加载失败！")
                }
            }
        });

        // 初始化存储配置表
        $.ajax({
            url:  "v1/store",
            async: false,
            success: function (data) {
                if (data.code===200){
                    for (let store of data.data){
                        if (store_min[store.roleCode] === undefined){
                            store_min[store.roleCode] = store.minSize;
                        }else{
                            store_min[store.roleCode] = store.minSize + store_min[store.roleCode];
                        }
                    }
                }else{
                    console.log("存储配置加载失败！")
                }
            }
        });


    };

    // 角色格式化展示
    this.roles_show = function () {
        return roles_show;
    };

    // 角色格式化展示
    this.roles_extend = function () {
        return roles_extend;
    };
    
    this.isInstalled = function () {
        return _this.getCfg("COMPOSE_K8S_INSTALL_FLAG").toLowerCase()==="true";
    };

    this.getCfg = function (key) {
        return global[key];
    };

    /**
     *  根据表数据生成推荐策略，每种角色都有一个对象方法， 如：master对应master 的函数，为了以后方便角色的拓展
     */
    this.policyRole = function(targets){
        $.ajax({
            type:"post",
            url: 'v1/policy',
            contentType:'application/json',
            data: JSON.stringify(targets===undefined?[]:targets),
            success:function(data){
                if (data.code === 200){
                    _table.reload(data.data);
                }else {
                    console.log(data.code);
                }
            }
        });
    };

    return init();
};
