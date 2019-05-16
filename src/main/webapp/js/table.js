var Table = function () {
    var _this = this;
    var $table = $('#host_table');
    var $install_btn = $("#install_cluster");

    var host_num = 0;

    this._form = undefined;
    this._check = undefined;
    this._policy = undefined;
    this._install = undefined;

    // 初始化样式
    var init = function () {

        // 创建策略对象
        _this._policy = new Policy(_this);

        $table.bootstrapTable('changeLocale', 'zh-CN');
        $table.bootstrapTable({
            url:basePath+"v1/cluster/host",
            showRefresh:true,
            classes: "table",
            search:true,
            buttonsAlign:'left',
            searchAlign:'left',
            toolbar:"#toolbar",
            toolbarAlign:'right',
            uniqueId:"ip",
            responseHandler:function(res){
                if(res.code===200){
                    host_num = res.data.length;
                    if (_this._policy.getCfg("K8S_INSTALL_FLAG") !== "true" && host_num>=3){
                        _this._policy.policyRole(res.data);
                    }
                    return res.data;
                }else{
                    return [];
                }
            },
            pageList:[5, 10, 20, 50, 100, 200],
            columns: [
                {
                    field: "isSelect",
                    checkbox: true,
                    formatter: stateFormatter,
                    events:{
                        'click .message': messageEvent
                    }
                },{
                    field: "ip",
                    title: "主机IP",
                    formatter:IPFormatter
                },{
                    field: "username",
                    title: "用户名"
                },{
                    field: "password",
                    title: "密码"
                },{
                    field: "devs",
                    title: "选择设备",
                    formatter: devFormatter,
                    events: {
                        'click .change-dev': changeDevEvent
                    }
                },{
                    field: "cpu",
                    title: "CPU核数"
                },{
                    field: "memory",
                    title: "内存/GB"
                },{
                    field: "allSpace",
                    title: "可使用空间/GB",
                    formatter: spaceFormatter
                },{
                    field: "roles",
                    title: "选择角色",
                    formatter: roleFormatter,
                    events: {
                        'click .change': changeRoleEvent
                    }
                },{
                    field: "minSize",
                    title: "最小需要空间",
                    formatter: minSizeFormatter
                },{
                    field: "operator",
                    title: "操作",
                    width:120,
                    align: 'center',
                    events: {
                        'click .edit': editEvent,
                        'click .check': checkEvent,
                        'click .delete': delEvent
                    },
                    formatter:optFormatter
                }
            ],
            onLoadSuccess: function () {
                $('[data-toggle="tooltip"]').tooltip();
            }
        });

        // 针对主机保存做的类控制器并初始化
        _this._form = new HostForm(_this);

        _this._check = new HostCheck(_this);

        _this.check = _this._check.check;


        // 集群安装或者拓展功能
        if (_this._policy.getCfg("K8S_INSTALL_FLAG")==="false"){
            _this._install = new InstallCluster("install_cluster");
            $install_btn.html(' 安装集群 <i title="安装集群" class="fa fa-power-off"></i>');
            $install_btn.on('click',function () {
                if (!_this._install.status()){
                    var result = _this._policy.savePolicy(_this.getHosts());
                    if (!result){
                        return;
                    }
                }

                var status = _this._install.start();
                if(!status){
                    alert("创建安装任务失败！");
                }
            });
        }else{
            
            $install_btn.html(' 拓展节点 <i title="拓展节点" class="fa fa-power-off"></i>');
            $install_btn.on('click',function () {
            	
            	var req = $table.bootstrapTable('getData');
            	
            	var result = _this._policy.savePolicyToNode(req);
            	if (!result){
                    return;
                }
            	
            	//-----------------循环查出需要扩展node的 主机ip
            	var targets = [];
            	for(let host of req){
            		var flag = true;
        			for(var key in host.roles){
        				if(host.roles[key].status === '2'){
        					flag = false;
        				}
        			}
        			if(flag){
        				targets[targets.length] = host.ip;
        			}
        		}
            	_this._install = new InstallCluster("extend_node",targets);
                var status = _this._install.start();
                if(!status){
                    alert("创建安装任务失败！"); 
                }
            });
        }
    };

    /**
     *
     *
     */
    var stateFormatter = function (value, row, index) {
        // for (var role in row.roles){
        //     if (row.roles[role].status==='2' && role === 'default'){
        //         return false;
        //     }
        // }
        // return true;
    };

    /**
     * @return {string}
     */
    var IPFormatter = function (value,row,index) {
        var color,popover='';

        if (row.status==='2'){
            color = "text-success";
        }else if(row.status ==='1'){
            color = 'text-danger';
            popover = 'data-container="body" data-toggle="popover" data-placement="right" data-content="'+row.message+'"';
        }else if (row.status === '0'){
            color = 'text-secondary';
        }else {
            color = "text-primary";
        }
        return ['<span class="message ',color,'" id="',value.replace(/\./g,'_'),'" ',popover,'><i title="校验主机" class="fa fa-desktop" ></i> ',value,'</span>'].join('');
    };

    // 设备格式化展示
    var devFormatter = function(value,row,index){
        var devButton = " ";
        for (let dev of row.devs){
            var btn_class = dev.status==='0'?' btn-success ':dev.status==='1'?' btn-secondary ':' btn-primary ';
            var str = ' <button type="button" data-trigger="hover" data-toggle="tooltip" data-placement="top" title="可用大小 【'+dev.enableSpace+' GB】" class="change-dev btn btn-sm ' + btn_class + '" data-id="'+dev.id+'" > <i class="fa fa-hdd" > ' + dev.devName.substr(5,3) +' </i>'+' </button> ';
            devButton = str+devButton;
        }
        return devButton;
    };

    // 可用空间格式化展示
    var spaceFormatter = function (value,row,index) {
        row.enableSpace = 0;
        for (let dev  of row.devs){
            if (dev.status !== '1'){row.enableSpace += dev.enableSpace;}
        }
        return ['<span>',row.enableSpace," (GB)",'</span>'].join("");
    };

    // 角色格式化展示
    var roleFormatter = function (value,row,index) {
        var str = "";
        for(let role of _this._policy.roles_show()){
            var show_class = row.roles[role] === undefined ? "btn-secondary":row.roles[role].status==='2'?"btn-primary":"btn-success";

            // 集群安装好之后就不展示没安装的角色。
            if(_this._policy.getCfg("K8S_INSTALL_FLAG") === "true" ){
                str += '<button type="button" class="btn '+show_class+' btn-sm " data-role-code="'+role+'" ><i class="fa fa-cogs"> '+role+'</i></button> '
            }
            if (_this._policy.getCfg("K8S_INSTALL_FLAG") !== "true" ){
                str += '<button type="button" class="btn '+show_class+' btn-sm change" data-role-code="'+role+'" ><i class="fa fa-cogs"> '+role+'</i></button> '
            }
        }
        return str;
    };
    
    var minSizeFormatter = function (value,row,index) {
        return _this._policy.getMinSize(row)+ " (GB)";
    };

    // 操作选项格式化展示
    var optFormatter = function (value,row,index) {
        var str = " ";
        var check_fa;
        if (row.status==='3'){ // 校验成功
            check_fa = '<i title="校验主机" class="fa fa-sync fa-pulse"></i>';
        }else{ // 未校验
            check_fa = '<i title="校验主机" class="fa fa-sync check"></i>';
        }

        str += '<a class="col-sm-offset-1 edit"  href="javascript:void(0)" data-toggle="modal" data-target="#host-add" ><i title="编辑主机" class="fa fa-edit" ></i></a> ';
        if(row.hostLock){
            str += '<a class="col-sm-offset-1 " href="javascript:void(0)" ><i title="主机有任务正在执行" class="fa fa-lock"></i></a> ';
        }else{
            str += '<a class="col-sm-offset-1" href="javascript:void(0)" >'+check_fa+'</a> ';
        }
        str += '<a class="col-sm-offset-1 delete"  href="javascript:void(0)" ><i title="删除主机" class="fa fa-trash"></i></a> ';
        return str+" ";
    };
    
    var messageEvent = function (e, value, row, index) {
        var $this = $(e.target);
        if ($this[0].type !== "span"){
            $this = $this.parents("span");
        }
        $this.popover("hide")
    };
    
    var editEvent = function (e, value, row, index) {
        _this._form.editHost(row);
    };

    // 删除主机事件
    var delTableEvent= function (row) {
        $.ajax({
            type:"delete",
            url:basePath+'v1/cluster/host',
            contentType:'application/json',
            data:row.ip,
            success:function(data){
                if (data.code === 200){
                    _this.refresh();
                }else {
                    console.log(data.code);
                }
            }
        });
    };

    // 清理node
    var delEvent= function (e, value, row, index) {
    	
    	var flag = false;
    	for(var i in row.roles){
	    	if(row.roles[i].status==='2'){
	    		flag=true;
	    	}
    	}
    	
    	if(flag){
    		_this._delete = new DELETE_NODE("delete_node");
        	
        	console.info(row);
        	if(row.locked === true){
        		alert("主机"+row.ip+"处于锁状态！");
        		return;
        	}
        	
        	if((row.roles.harbor !== undefined && row.roles.harbor.status === '2') 
        		|| (row.roles.master !== undefined && row.roles.master.status === '2') 
        		|| (row.roles.etcd !== undefined && row.roles.etcd.status === '2')
        		|| (row.roles.ceph_mon !== undefined && row.roles.ceph_mon.status === '2')
        		|| (row.roles.ceph_osd !== undefined && row.roles.ceph_osd.status === '2')){
        		
        		alert("主机"+row.ip+"存在系统角色，不能清理！");
        		return;
        	}
        	
        	_this._delete.delete_node(row.ip);
    	}else{
    		delTableEvent(row);
    	}
    	
    	
    }
    
    // 检查主机事件
    var checkEvent = function (e, value, row, index) {
        if(_this.check === undefined){
            alert("主机检查模块加载失败！");
        }else{
            _this.check(row.ip);
        }
    };

    // 调整角色事件
    var changeRoleEvent = function (e, value, row, index) {
        if (_this._policy.getCfg("K8S_INSTALL_FLAG") === "true" ){
            return;
        }

        var $this = $(e.target);
        if ($this[0].type !== "button"){
            $this = $this.parents("button");
        }

        var roleCode = $this.data('role-code');
        if (row.roles[roleCode]===undefined){
            row.roles[roleCode] = {}
        }else{
            delete row.roles[roleCode]
        }

        var hosts = _this.getHosts();
        if (hosts.length>=3){
            _this._policy.policyRole(hosts);
            $table.bootstrapTable("load",hosts);
        }else{
            updateHost(row);
        }
    };
    
    var updateHost = function (row) {
        $table.bootstrapTable('updateByUniqueId', {ip: row.ip, row: row});
        $('[data-toggle="tooltip"]').tooltip();
        if (row.status === '1'){
            $("#"+row.ip.replace(/\./g,'_')).popover('show');
        }
    };
    
    var changeDevEvent = function (e, value, row, index) {
        if (row.devs===undefined){
            return
        }

        var $this = $(e.target);
        if ($this[0].type !== "button"){
            $this = $this.parents("button");
        }

        $this.tooltip('hide');
        for (let dev of row.devs){
            if( dev.id === $this.data("id")){
                if (dev.status === '0'){
                    dev.status='1';
                }else if(dev.status === '1'){
                    dev.status='0';
                }else{
                    return;
                }

                $.ajax({
                    type:"post",
                    url:basePath+'v1/cluster/dev/' + (dev.status === '1'?'disable':'enable'),
                    contentType:'application/json',
                    data: dev.id,
                    success:function(data){
                        if (data.code === 200){

                        }else {
                            console.log(data.code)
                        }
                    }
                });

                updateHost(index,row);
                break;
            }
        }
        
        var hosts = _this.getHosts();
        if (hosts.length>=3){
            _this._policy.policyRole(hosts);
            $table.bootstrapTable("load",hosts);
        }else{
            updateHost(row);
        }
    };


    this.refresh = function () {
        $table.bootstrapTable('refresh');
    };

    this.getHosts = function () {
        return $table.bootstrapTable('getData');
    };
    
    this.getIps = function () {
        var hosts  = $table.bootstrapTable('getSelections');
        var ips = [];
        for (let host of hosts){
            ips[ips.length] = host.ip;
        }
        return ips;
    };

    this.updateHostStatus = function (_host) {
        var host = $table.bootstrapTable("getRowByUniqueId",_host.ip);
        if (host.status !== _host.status){
            // 如果任务失败，将更新数据库的状态
            if (status==='1'){
                $.ajax({
                    url: basePath+"v1/cluster/callback/host",
                    type: "post",
                    data: {host: encodeURI(JSON.stringify(_host))}
                });
            }
            host.status = _host.status;
            host.message = _host.message;
            host.devs = _host.devs;
            updateHost(host);
        }
    };
    
    return init();
};
