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
    var global = undefined;
    var _this = this;
    var _table = table;
    var store_min = {};
    // 存储每个角色对应的主机
    var roles_all = [];

    var init = function () {
        $.ajax({
            url: basePath+"v1/cluster/global",
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
            url: basePath+"v1/cluster/roles_cfg",
            async: false,
            success: function (data) {
                if (data.code===200){
                    for (let role of data.data){
                        switch (role.roleType) {
                            case '0':
                                roles_base[roles_base.length] = role.roleCode;
                                break;
                            case '1':
                                roles_show[roles_show.length] = role.roleCode;
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
            url: basePath+"v1/cluster/store",
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
    this.roles_base = function () {
        return roles_base;
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
    
    this.getCfg = function (key) {
        return global[key];
    };

    /**
     *  根据表数据生成推荐策略，每种角色都有一个对象方法， 如：master对应master 的函数，为了以后方便角色的拓展
     * @param data
     */
    this.policyRole = function(data){
        var _this = this;
        var master = "master";
        var roles_set = {};
        var host_map = {};

        var get_role_set = function (roleCode) {
            var role_set = [];
            for (let host of data){
                if (roleCode in host.roles){
                    role_set[role_set.length] = host.ip;
                }
            }
            return  role_set;
        };

        this.etcd = function (role_code) {
            var etcd_set = get_role_set(role_code);

            if (etcd_set.length>=3){
                roles_set[role_code] = etcd_set;
                return
            }

            for (let host of data){
                if (etcd_set.length <3){
                    etcd_set[etcd_set.length] = host.ip;
                }else{
                    break;
                }
            }

            roles_set[role_code] = etcd_set;
        };

        this.master=function (role_code) {
            var  control_ip;
            var master_set = get_role_set(role_code);

            if(master_set.length>0){
                roles_set[role_code] = master_set;
                return;
            }

            for (let host of data){
                if (_this.getCfg("SYSTEM_CONTROL_IP") === host.ip){
                    control_ip = host.ip
                }
            }

            if (master_set.length === 0 && control_ip !== undefined){
                master_set[0] = control_ip;
            }

            roles_set[role_code] = master_set;
        };

        /**
         *  node 依赖master策略
         * @param role_code
         */
        this.node=function (role_code) {
            if (roles_set[master] === undefined){
                _this[master](master);
            }
            var master_set = roles_set[master];

            var node_set = [];
            for (let host of data){
                if (master_set.indexOf(host.ip) === -1){
                    node_set[node_set.length] = host.ip;
                }
            }

            roles_set[role_code] = node_set;
        };

        this.harbor = function (role_code) {
            var harbor_set = [];
            if (roles_set[master] === undefined){
                _this[master](master);
            }
            var master_set = roles_set[master];

            var max_size = -1;
            var max_ip = "";
            for (let host of data){
                if (host.enableSpace === undefined){
                    var space = 0;
                    for (let dev of host.devs){
                    	if (dev.status!=='1'){
                    		space += dev.enableSpace;
                    	}
                    }
                    host.enableSpace = space;
                }

                if (host.enableSpace >= max_size ){
                    if (master_set.indexOf(host.ip) === -1){
                        harbor_set[0] = host.ip;
                        max_size=host.enableSpace;
                    }
                    max_ip = host.ip;
                }
            }

            if (harbor_set.length === 0){
                harbor_set[0] = max_ip;
            }
            roles_set[role_code] = harbor_set;
        };

        this.operator = function (role_code) {
            var operator_set = [];

            if (roles_set[master] === undefined){
                _this[master](master);
            }

            var master_ip = "";
            if (roles_set[master].length>0){
                master_ip = roles_set[master][0];
            }

            for (let host of data){
                if (host.ip!==master_ip){
                    operator_set[operator_set.length] = host.ip;
                }
            }
            roles_set[role_code] = operator_set;
        };

        this.ceph_osd = function (role_code) {
            var harbor = "harbor";
            var ceph_osd_set = get_role_set(role_code);

            if (role_code in roles_show || ceph_osd_set.length>=3){
                roles_set[role_code] = ceph_osd_set;
                return
            }

            // 获取非harbor主机
            for (let host of data){
                if (ceph_osd_set.length<3){
                    ceph_osd_set[ceph_osd_set.length] = host.ip;
                }
            }

            // 如果最终补救不到三台，放弃治疗，不装ceph了
            if (ceph_osd_set.length <3){
                ceph_osd_set = [];
            }

            roles_set[role_code] = ceph_osd_set;
        };

        this.ceph_mon = function (role_code) {
            var ceph_osd = "ceph_osd";
            var ceph_set = get_role_set(role_code);

            if (roles_set[ceph_osd] === undefined){
                _this[ceph_osd](ceph_osd);
            }

            if (flag){
                roles_set[role_code] = ceph_set;
                if (ceph_set.length === 0){
                    roles_set[ceph_osd] = [];
                }
                return;
            }else{
                flag = !flag;
            }

            if (roles_set[master] === undefined){
                _this[master](master);
            }

            var master_ip = "";
            if (roles_set[master].length>0){
                master_ip = roles_set[master][0];
            }

            if (roles_set[ceph_osd].length > 0){
                if (roles_set[ceph_osd][0]!==master_ip){
                    ceph_set = [roles_set[ceph_osd][0]];
                }else{
                    ceph_set = [roles_set[ceph_osd][1]];
                }
            }

            roles_set[role_code] = ceph_set;
        };

        // 为了统一调用，把这个两个相同的策略复制到另外两个角色里面
        this.ceph_rgw = function (role_code) {
            var ceph_mon = "ceph_mon";
            if (roles_set[ceph_mon] === undefined){
                _this[ceph_mon](ceph_mon);
            }
            roles_set[role_code] = roles_set[ceph_mon];
        };

        this.ceph_mds = _this.ceph_rgw;

        if (data.length ===0){
            return roles_set;
        }

        for (let role of roles_all){
            if (roles_set[role] === undefined && roles_base.indexOf(role) === -1){
                _this[role](role);
            }
        }

        var allIp = [];
        for (let host of data){
            allIp[allIp.length] = host.ip;
            host.roles = {};
            host_map[host.ip] = host;
        }
        for (let base of roles_base){
            if (roles_set["ceph_mon"] === 0 && base.contains("ceph")){
                roles_set[base] = []
            }else{
                roles_set[base] = allIp;
            }
        }

        for (var role in roles_set){
            for (let ip of roles_set[role]){
                var host = host_map[ip];
                if (!(role in host.roles)){
                    host.roles[role] = {status:0}
                }
            }
        }

        return roles_set;
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
            url: basePath + "v1/cluster/global",
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
            url: basePath + "v1/cluster/roles",
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
            url: basePath + "v1/cluster/roles",
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
    }
    
    return init();
};
