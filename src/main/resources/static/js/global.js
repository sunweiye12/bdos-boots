/**
 * 主要完成 主机角色策略的生成，角色变更后磁盘的分配
 *
 * @constructor
 */

var Global = function (table) {
    var $vip = $("#k8s_virtual_ip");
    var $global_form = $("#global_form");
    var $save_global = $("#save_global");
    var cfg_type_class = {'0': 'readonly','1':'readonly','2':''};
    var cfg_group={'SYSTEM':"系统配置",'COMPOSE':"组件配置",'PORT': "端口配置"};

    // 可选择的角色节点
    var roles_show = [];
    var roles_base = [];
    var roles_extend = [];
    var global = {};
    var _this = this;
    var _table = table;
    // 存储每个角色对应的主机
    var roles_all = [];

    var init = function () {
        for (var key in cfg_group){
            $global_form.append('<div class="'+key+'"><h5>'+cfg_group[key]+'</h5><hr/></div><br/>');
        }
        $.ajax({
            url:  "v1/global",
            async: false,
            success: function (data) {
                if (data.code===200){
                    data.data.forEach(function (cfg) {
                        global[cfg.cfgKey]=cfg.cfgValue;
                        var cfg_seg = cfg.cfgKey.split('_');
                        $global_form.find('.'+cfg_seg[0]).append('<div class="form-group " >\n' +
                            '                        <label class="control-label" for="'+cfg.cfgKey+'">'+cfg.cfgKey+': '+cfg.memo+'</label>\n' +
                            '                        <div class="input-group">\n' +
                            '                            <div class="input-group-prepend"><span class="input-group-text fa '+cfg.icon+'"></span></div>\n' +
                            '                            <input id="'+cfg.cfgKey+'" name="'+cfg.cfgKey+'" class="form-control '+cfg_type_class[cfg.cfgType]+'"  type="text" value="'+cfg.cfgValue+'"/>\n' +
                            '                        </div>\n' +
                            '                    </div>');
                        if(cfg_type_class[cfg.cfgType]===''){
                            if(cfg_seg[cfg_seg.length-1]==='HA'){
                                bind_valid($("#"+cfg.cfgKey),"端口范围(0-65535)",valid_empty_port);
                            }else if (cfg_seg[0]==='PORT'){
                                bind_valid($("#"+cfg.cfgKey),"端口范围(0-65535)",valid_port);
                            }else if(cfg.cfgKey==='SYSTEM_CONTROL_IP'){
                                bind_valid($("#"+cfg.cfgKey),cfg.memo+"格式不对",valid_ip);
                            }else if(cfg_seg[cfg_seg.length-1]==='IP'){
                                bind_valid($("#"+cfg.cfgKey),cfg.memo+"格式不对",valid_empty_ip);
                            }else if(cfg_seg[cfg_seg.length-1]==='FLAG'){
                                bind_valid($("#"+cfg.cfgKey),"只能输入true/false",valid_bool);
                            }else if(cfg.cfgKey!=='SYSTEM_ENV_SUFFIX' && cfg.cfgKey!=='SYSTEM_TIMEZONE'){
                                bind_valid($("#"+cfg.cfgKey),"参数不能为空",valid_empty);
                            }
                        }
                    });
                    $global_form.find('input'+(_this.isInstalled()?'':'.readonly')).attr("readonly","readonly");
                    $vip.val(global.COMPOSE_K8S_VIRTUAL_IP);
                }else{
                    console.log("全局配置加载失败！");
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
        
        $save_global.on('click',saveAllGlobal)
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

    this.saveVip = function(){
        var masterNum = 0;
        _table.getFields("roles").forEach(function (roles) {
            if (roles.master!==undefined){
                masterNum++;
            }
        });

        if (masterNum>1&&$vip.val()===""){
            alert("多节点master 请设置VIP！");
            return false;
        }

        var flag = true;
        var vip =_this.getCfg("COMPOSE_K8S_VIRTUAL_IP");
        $.ajax({
            url: "v1/dev/allocate",
            type:"post",
            async: false,
            contentType:'application/json',
            data: JSON.stringify({
                COMPOSE_K8S_VIRTUAL_IP: vip
            }),
            success:function(data){
                if (data.code !== 200 ){
                    flag = false;
                }
            }
        });
        return flag;
    };

    // 提交表单用于检查输入
    var doValid = function (data){
        if (data === undefined){
            data = formJson($global_form);
        }
        checkReset();
        var check_flag = true;
        for (var key in global){
            check_flag = checkActive($("#"+key),data[key])&&check_flag;
        }
        return check_flag;
    };
    
    var saveAllGlobal = function () {
        var data = formJson($global_form);
        if (doValid(data)){
            $.ajax({
                url: 'v1/host',
                type:"post",
                contentType:'application/json',
                data: JSON.stringify(data)
            });
            checkReset();
        }
    };

    return init();
};
