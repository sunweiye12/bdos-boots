/**
 * 主要完成 主机角色策略的生成，角色变更后磁盘的分配
 *
 * @constructor
 */

var Policy = function (table) {
    var $vip = $("#k8s_virtual_ip");
    var flag = false;

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

    this.getMinSize = function (host) {
        var minSize = 0;
        for (var role in host.roles){
            if (role in store_min){
                minSize+=store_min[role];
            }
        }
        return minSize
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
    this.policyRole = function(){
        // $.map(data,function (host) {
        //     return {
        //         cpu: host.cpu,
        //         hostname: host.hostname,
        //         ip: host.ip,
        //         memory: host.memory,
        //         password: host.root,
        //         sshPort: host.sshPort,
        //         username: host.root
        //     }
        // })
        // var data = _table.getHosts();
        // data: JSON.stringify(data),

        $.ajax({
            type:"post",
            url: 'v1/policy',
            contentType:'application/json',
            data: JSON.stringify([]),
            success:function(data){
                if (data.code === 200){
                    _table.reload(data.data);
                }else {
                    console.log(data.code);
                }
            }
        });
    };

    this.savePolicy = function (data) {
        //save global
        var roles_set = _this.policyRole(data);
        var vip = $vip.val().trim();

        if (roles_set.master.length === 0){
            alert("没有选择MASTER角色！");
            return false;
        }
        if (vip!==""&&!ipRegex.test(vip)){
            alert("VIP 不合法！");
            $vip.focus();
            return false;
        }
        if (vip === "" && roles_set.master.length>1){
            alert("多MASTER 请输入VIP");
            $vip.focus();
            return false;
        }

        if (vip === ""){
            vip = roles_set.master[0];
        }
        var flag = false;
        var REGISTER_IP = roles_set.harbor[0];
        $.ajax({
            url:   "v1/global",
            method: "post",
            contentType: "application/json",
            data: JSON.stringify({
                COMPOSE_K8S_VIRTUAL_IP: vip,
                COMPOSE_HARBOR_IP : REGISTER_IP
            }),
            async: false,
            success: function (data) {
                if (data.code === 200){
                    flag = true;
                }else{
                    alert(data.message)
                }
            }
        });

        if(!flag){
            return flag;
        }

        $.ajax({
            url:   "v1/roles",
            method: "post",
            contentType: "application/json",
            data: JSON.stringify(roles_set),
            async: false,
            success: function (data) {
                if (data.code !== 200){
                    flag = false;
                    alert(data.message)
                }
            }
        });

        return flag;
    };

    //拓展node时，保存角色
    //拓展node时，只需要分配node，operator，docker，ceph_client
    this.savePolicyToNode = function (data) {
    	var _this = this;
    	var roles_set = {};

    	//1.组装node   除了master节点  全部为node
		var node_set = [];
		var i = 0;
		for(let host of data){
			if(host.roles.master === undefined){
				node_set[i] = host.ip;
				i ++ ;
			}
		}
		roles_set.node = node_set;

		//2.组装operator    组装上原来已安装好的operator，然后加上新增的node ip
		var operator_set = [];
		for(let host of data){
			if(host.roles.operator !== undefined){
				operator_set[operator_set.length] = host.ip;
			}

			if(host.roles.operator === undefined && host.roles.master === undefined ){
				operator_set[operator_set.length] = host.ip;
			}
		}
		roles_set.operator = node_set;

		//3.组装docker  主机全部安装docker
		var docker_set = [];
		for(let host of data){
			docker_set[docker_set.length] = host.ip;
		}
		roles_set.docker = docker_set;

		//4.组装ceph_client  同docker一样 主机全部安装ceph_client
		roles_set.ceph_client = docker_set;

    	var flag = true;
    	$.ajax({
            url:   "v1/roles",
            method: "post",
            contentType: "application/json",
            data: JSON.stringify(roles_set),
            async: false,
            success: function (data) {
                if (data.code !== 200){
                    flag = false;
                    alert(data.message)
                }
            }
        });
        return flag;
    };

    return init();
};
