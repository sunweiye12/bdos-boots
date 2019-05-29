/**
 *  form 表单封装的类
 * @param table
 * @constructor
 */

ipRegex = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/;

var HostForm = function (table) {
    var _this = this;

    var $hostForm = $("#host_form");
    var $host_save = $("#save_host");
    var $host_add = $("#add_btn");
    var $isGroup = $("#isGroup");
    var $template = $("#template");

    var $IP = $("#ip");
    var $startIp = $("#startIp");
    var $endIp = $("#endIp");
    var $username = $("#username");
    var $password = $("#password");
    var $sshPort = $("#sshPort");

    // 是否是连续主机,初始化之后默认调用了一次切换
    var flag = false;

    var init = function () {
        var valid_ip = function (ip) {
            return ipRegex.test(ip);
        };
        var valid_len = function (value) {
            return  value.length<32&&value.length>3;
        };
        var valid_port = function (port) {
            return port>0&&port<65535;
        };

        // 为input 绑定校验控制
        bind_valid($IP,"主机IP不合法",valid_ip);
        bind_valid($startIp,"起始IP不合法",valid_ip);
        bind_valid($endIp,"结束IP不合法",valid_ip);
        bind_valid($username,"用户名长度范围[4-31]",valid_len);
        bind_valid($password,"密码长度范围[4-31]",valid_len);
        bind_valid($sshPort,"端口范围(0-65535)",valid_port);

        $host_add.on('click',function () {
            change(flag);

            if ($isGroup.parent().hasClass('d-none')){
                $isGroup.parent().removeClass('d-none');
            }
        });
        // 为按钮切换绑定控制方法
        $isGroup.on('change',function () {
            flag = !flag;
            change(flag);
        });

        $host_save.on('click',save);

        // 模板主机保存
        $template.on('change', upload);
        return _this;
    };
    
    var bind_valid= function ($input,memo,invoke) {
        $input.parent().append('<div class="invalid-feedback">'+memo+'!</div>');
        $input.valid_invoke = invoke;
        $input.bind('input propertychange',function () {
            checkReset($input);
            checkActive($input);
        });
    };

    var change=function (value) {
        $( value?".hostSingle":".hostGroup").hide();
        $(value?".hostGroup":".hostSingle").show();
    };

    //IP转数字
    var ip2int = function(ip) {
        ip = ip.split(".");
        var num = Number(ip[0]) * 256 * 256 * 256 + Number(ip[1]) * 256 * 256 + Number(ip[2]) * 256 + Number(ip[3]);
        num = num >>> 0;
        return num;
    };

    //数字转IP
    var int2ip = function(num) {
        var str;
        var tt = [];
        tt[0] = (num >>> 24) >>> 0;
        tt[1] = ((num << 8) >>> 24) >>> 0;
        tt[2] = (num << 16) >>> 24;
        tt[3] = (num << 24) >>> 24;
        str = String(tt[0]) + "." + String(tt[1]) + "." + String(tt[2]) + "." + String(tt[3]);
        return str;
    };
    
    var formJson = function () {
        var array = $hostForm.serializeArray();
        var data = {};
        for (let row of array){
            data[row.name] = row.value.trim();
        }
        return data;
    };

    // 校验IP 是否合法
    var  checkActive = function($this,value){
        if(value === undefined){
            value = $this.val();
        }
        var valid = $this.valid_invoke(value);

        if (valid&&!$this.hasClass("is-valid")){
            $this.addClass("is-valid");
        }
        if(!valid&&!$this.hasClass("is-invalid")){
            $this.addClass("is-invalid");
        }
        return valid;
    };

    // 清空input 样式
    var checkReset = function ($this) {
        // 清楚单个节点的校验展示
        if ($this!==undefined){
            if($this.hasClass("is-valid")){$this.removeClass("is-valid");}
            if($this.hasClass("is-invalid")){$this.removeClass("is-invalid");}
        }else{ // 清楚所有input 的校验展示
            $(".is-valid").each(function () {$(this).removeClass("is-valid");});
            $(".is-invalid").each(function () {$(this).removeClass("is-invalid");})
        }
    };

    // 提交表单用于检查输入
    var doCheck = function (data){
        if (data === undefined){
            data = formJson();
        }

        checkReset();

        var check_flag = true;
        // 检查IP
        if (flag){
            check_flag = checkActive($startIp,data.startIp)&&check_flag;
            check_flag = checkActive($endIp,data.endIp)&&check_flag;
        }else{
            check_flag = checkActive($IP,data.ip)&&check_flag;
        }
        // 检查 用户密码
        check_flag = checkActive($username,data.username)&&check_flag;
        check_flag = checkActive($password,data.password)&&check_flag;
        // 检查端口
        check_flag = checkActive($sshPort,data.sshPort)&&check_flag;

        return check_flag;
    };

    // 提交保存主机信息
    var save = function () {
        var data = formJson();
        if (doCheck(data)){
            var hosts = [];
            if (flag){
                for (var startIp = ip2int(data.startIp),endIp=ip2int(data.endIp);startIp<=endIp;startIp++){
                    hosts[hosts.length] = int2ip(startIp);
                }
            }else{
                hosts[hosts.length] = data.ip;
            }

            var error = [];
            for(let ip of hosts){
                $.ajax({
                    url: 'v1/cluster/host',
                    type:"post",
                    contentType:'application/json',
                    data: JSON.stringify({
                        ip: ip,
                        username: data.username,
                        password: data.password,
                        sshPort: data.sshPort
                    }),
                    success:function(data) {
                        if (ip === hosts[hosts.length-1]){
                            table.refresh();
                        }
                        if (data.code !== 200){
                            alert(error.join(",")+" 主机保存失败！");
                        }
                    }
                })
            }
            checkReset();
        }
    };

    var upload  = function () {
        var template = new FormData(document.getElementById("template-form"));
        $.ajax({
            url:  "v1/cluster/upload",
            type: "post",
            data: template,
            processData:false,
            contentType:false,
            success:function(data){
                if(data.code === 200){
                    table.refresh();
                    $template.replaceWith($template.prop("outerHTML"));
                    $template = $("#template");
                    $template.on('change',upload);
                }else{
                    alert("文件解析失败！");
                    console.log(data.message);
                }
            }
        });
    };
    
    this.editHost = function (row) {
        change(false);
        if (!$isGroup.parent().hasClass('d-none')){
            $isGroup.parent().addClass('d-none');
        }

        $IP.val(row.ip);
        $username.val(row.username);
        $password.val(row.password);
        $sshPort.val(row.sshPort);

    };

    return init();
};