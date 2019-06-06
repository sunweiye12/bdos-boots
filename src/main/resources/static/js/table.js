var Table = function () {
    var _this = this;
    var $table = $('#host_table');
    var $check_btn = $('#host_check');
    var $install_btn = $("#install_cluster");
    var $clean_cluster = $("#clean_cluster");

    var host_status_class={'1':"text-danger",'2':"text-success",'0':"text-secondary"};
    var dev_status_class={'0':"btn-success",'1':"btn-secondary",'2':"btn-primary"};
    var role_status_class={'-1':'change btn-secondary','0':'change btn-success','1':'change btn-success','2':'btn-primary'};
    var host_lock_class={true:'fa-pulse',false:'check'};
    var pop_msg_data={true:'data-toggle="popover" data-trigger="focus" data-container="body" data-placement="right"',false:''};
    var cluster_opt={true:{title: '拓展节点',code: 'install_cluster'},false:{title: '安装集群',code:'extend_node'}};
    var dev_enable_swatch={'1':{status:'0',url: 'disable'},'0':{status: '1',url: 'enable'}};

    _this._policy = new Global(_this);
    _this.install_flag = _this._policy.isInstalled();

    _this._form = new HostForm(_this);
    _this._check = new HostHandle(_this,"host_check");
    _this._delete = new HostHandle(_this,"delete_node");
    _this._clean = new HostHandle(_this,"host_clean");

    _this._install = new InstallCluster("install_cluster");
    _this._extend = new InstallCluster("extend_node");

    // 初始化样式
    var init = function () {
        // 创建策略对象
        $check_btn.on('click',_this._check.handleAll);
        $clean_cluster.on('click',_this._clean.handleAll);
        _this.dynamic_columns=[];
        _this.role_extend = _this._policy.roles_extend();
        var role_show = _this._policy.roles_show();
        var show_columns = [
            {
                field: "isSelect",
                checkbox: true,
                rowspan: 2,
                switchable:false,
                valign: "middle"
            },{
                field: "ip",
                title: "主机IP",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                formatter:IPFormatter
            },{
                field: "username",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "用户名"
            },{
                field: "password",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "密码"
            },{
                field: "user",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "负责人"
            },{
                field: "cpu",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "CPU核数"
            },{
                field: "memory",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "内存/GB"
            },{
                field: "devs",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "设备选择",
                formatter: devFormatter,
                events: {
                    'click .change-dev': changeDevEvent
                }
            },{
                field: "allSpace",
                rowspan: 2,
                switchable:false,
                valign: "middle",
                title: "可使存储",
                formatter: spaceFormatter
            },{
                field: "roles",
                title: "安装角色",
                align: 'center',
                colspan: _this.role_extend.length+1
            }
        ];

        _this.role_extend.forEach(function (role) {
            var visible=role_show.indexOf(role.roleCode)!==-1;
            _this.dynamic_columns.push({
                field: role.roleCode,
                title: role.roleDesc,
                visible: visible,
                formatter: function (value,row,index,field) {
                    var host_role = row.roles[field];
                    if(host_role === undefined){  host_role={'status':'-1'}; }
                    return  '<button type="button" class="btn btn-sm '+role_status_class[host_role.status]+' " data-role-code="'+field+'" ><i class="fa '+role.icon+'"> </i></button> '
                },
                width: "55px",
                align: 'center',
                events: {
                    'click .change': function (e, value, row, index) {
                        if (_this.install_flag){
                            return;
                        }
                        //更新对应的角色节点
                        var roleCode = role.roleCode;
                        if (row.roles[roleCode]===undefined){
                            row.roles[roleCode] = {status: '0'}
                        }else{
                            delete row.roles[roleCode]
                        }
                        _this._policy.policyRole(_this.getHosts());
                    }
                }
            });
        });

        _this.dynamic_columns.push({
            field: "operator",
            title: "操作",
            width:120,
            switchable:false,
            height: "32px",
            align: 'center',
            formatter:optFormatter,
            events: {
                'click .edit': function (e, value, row, index) {
                    _this._form.editHost(row);
                },
                'click .check': function (e, value, row, index) {
                    _this._check.handle([row.ip]);
                },
                'click .delete': delEvent
            }
        });

        $table.bootstrapTable({
            url: "v1/host",
            classes: "table",
            minimumCountColumns: 2,
            buttonsAlign:'right',
            searchAlign:'right',
            toolbar:"#toolbar",
            exportTypes: ['json', 'csv', 'txt', 'sql', 'excel'],
            toolbarAlign:'left',
            uniqueId:"ip",
            pageList:[5, 10, 20, 50, 100, 200],
            columns: [show_columns,_this.dynamic_columns],
            formatFullscreen: function(){ return "全屏"; },
            formatColumns: function(){ return "角色"; },
            responseHandler:function(res){
                return res.code===200?res.data:[];
            },
            onResetView: function () {
                $('.tooltip').remove();
                $('[data-toggle="tooltip"]').tooltip();
                $('.popover').remove();
                $('[data-toggle="popover"]').popover('show');
            }
        });

        clusterBtnReset();

        $install_btn.on('click',function () {
            var targets;
            if (_this.install_flag){
                targets = _this.getSelectFields("ip");
                if (targets.length===0){
                    alert("请选择拓展的主机节点");
                    return;
                }
            }else{
                if(!_this._policy.saveVip()){
                    return;
                }
                targets = _this.getFields("ip") ;
            }
            $.ajax({
                url: "v1/dev/allocate",
                type:"post",
                async: false,
                contentType:'application/json',
                data: JSON.stringify(targets),
                success:function(data){
                    if (data.code === 200 ){
                        var status = _this.install_flag?_this._extend.start(targets):_this._install.start(targets);
                        if(!status){
                            alert("创建任务失败！");
                        }
                    }else{
                        alert("存储分配失败！"+data.message);
                    }
                }
            });
        });
    };

    var clusterBtnReset = function () {
        $install_btn.html(' '+cluster_opt[_this.install_flag].title+'  <i title="集群操作请慎重！" class="fa fa-power-off"></i>');
    };

    /**
     * @return {string}
     */
    var IPFormatter = function (value,row,index) {
        var msg = _this._check.getHostMsg(value);
        var has_msg = msg!==undefined && msg.length>0;
        return [' <span class="',host_status_class[row.status],'" ', pop_msg_data[has_msg] ,' data-container="body" data-content="',has_msg?msg.join(' ; '):'','" > ',
                        '<i title="校验主机" class="fa fa-desktop" ></i> ', value,
                    '</span>'].join('');
    };
    // 设备格式化展示
    var devFormatter = function(value,row,index){
        var dev_html = [];
        row.enableSize=0;
        row.devs.forEach(function (dev) {
            dev_html.push(' <button type="button" data-trigger="hover" data-toggle="tooltip" data-placement="top" title="可用大小 【'+dev.enableSize+' GB】" class="change-dev btn btn-sm ' + dev_status_class[dev.status] + '" data-id="'+dev.id+'" > ');
            dev_html.push('     <i class="fa fa-hdd" > ' + dev.devName.substr(5,3) +' </i>');
            dev_html.push(' </button> ');
            if (dev.status !== '1'){row.enableSize += dev.enableSize;}
        });
        return dev_html.join(' ');
    };
    // 可用空间格式化展示
    var spaceFormatter = function (value,row,index) {
        return ['<span>',row.enableSize," (GB)",'</span>'].join("");
    };
    // 操作选项格式化展示
    var optFormatter = function (value,row,index) {
        return ['<a class="col-sm-offset-1 edit"  href="javascript:void(0)" data-toggle="modal" data-target="#host-add" ><i title="编辑主机" class="fa fa-edit " ></i></a>',
            '<a class="col-sm-offset-1 " href="javascript:void(0)" ><i title="执行中..." class="fa fa-sync '+host_lock_class[row.hostLock]+'"></i></a> ',
            '<a class="col-sm-offset-1 delete"  href="javascript:void(0)" ><i title="删除主机" class="fa fa-trash "></i></a> '
        ].join(' ');
    };

    var delEvent = function (e, value, row, index) {
        for (let roleCode of _this.role_extend){
            var role = row.roles[roleCode];
            if (role!==undefined && role.status==='2'){
                alert(roleCode+"核心节点已安装，删除后集群将不可用！");
                return;
            }
        }
        var docker = row.roles.docker;
        if (docker !==undefined && docker.status==='2'){
            _this._delete.handle([row.ip],function (data) {
                if (data.status==='2'){
                    deleteHost(row);
                }else{
                    alert("节点卸载任务失败！");
                }
            });
        }else{
            deleteHost(row);
        }
    };


    // 删除主机事件
    var deleteHost= function (row) {
         $.ajax({
            type:"delete",
            url: 'v1/host',
            contentType:'application/json',
            data:row.ip,
            success:function(data){
                if (data.code === 200){
                    _this.delete(row.ip);
                }else {
                    console.log(data.code);
                }
            }
        });
    };

    var changeDevEvent = function (e, value, row, index) {
        var $this = $(e.target);
        if ($this[0].type !== "button"){
            $this = $this.parents("button");
        }

        // $this.tooltip('hide');
        row.devs.forEach(function (dev) {
            if( dev.id === $this.data("id") ){
                dev.status = dev_enable_swatch[dev.status].status;
                $.ajax({
                    type:"post",
                    url: 'v1/dev/' + dev_enable_swatch[dev.status].url,
                    contentType:'application/json',
                    data: dev.id,
                    success:function(data){
                        if (data.code !== 200){
                            console.log(data.code)
                        }
                    }
                });
            }
        });

        _this.reload();
    };


    this.refresh = function () {
        $table.bootstrapTable('refresh');
    };
    
    this.reload = function (data) {
        if (data === undefined){
            $.get("v1/host", function(result){
                $table.bootstrapTable('load',result.data);
            });
        }else{
            $table.bootstrapTable('load',data);
        }
    };

    this.getSelectHosts = function () {
        return $table.bootstrapTable('getAllSelections');
    };

    this.getHosts = function () {
        return $table.bootstrapTable('getData');
    };
    
    this.delete = function (ip) {
        $table.bootstrapTable('removeByUniqueId',ip);
    };
    
    this.getSelectFields = function (field) {
        return _this.getSelectHosts().map(function (row) {
            return row[field];
        });
    };

    this.getFields = function (field) {
        return _this.getHosts().map(function (row) {
            return row[field];
        });
    };

    return init();
};
