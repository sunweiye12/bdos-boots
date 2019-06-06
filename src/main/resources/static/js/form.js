/**
 *  form 表单封装的类
 * @param table
 * @constructor
 */


var HostForm = function (table) {
    var _this = this;
    _this._policy = table._policy;

    var $host_form = $("#host_form");
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
    var $user= $("#user");
    var $phone = $("#phone");

    // 是否是连续主机,初始化之后默认调用了一次切换
    var flag = false;

    var init = function () {
        // 为input 绑定校验控制
        bind_valid($IP,"主机IP不合法",valid_ips);
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

    var change=function (value) {
        $( value?".hostSingle":".hostGroup").hide();
        $(value?".hostGroup":".hostSingle").show();
    };

    // 根据第一个ip 段补齐剩余的ip
    var polishIp = function(example,ip) {
        if (example.split(".").length!==4){
            return example;
        }
        var ip_seg = example.split(".").slice(0,4-ip.split(".").length);
        ip_seg.push(ip);
        return ip_seg.join(".");
    };

    // 提交表单用于检查输入
    var doValid = function (data){
        if (data === undefined){
            data = formJson($host_form);
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
        var data = formJson($host_form);
        if (doValid(data)){
            var hosts = [];
            if (flag){
                for (var curIp = ip2int(data.startIp),endIp=ip2int(data.endIp);curIp<=endIp;curIp++){
                    hosts.push(int2ip(curIp));
                }
            }else{
                var ips = data.ip.split(",");
                ips.forEach(function (curIp) {
                    hosts.push(polishIp(ips[0],curIp));
                });
            }

            var error = [];
            hosts.forEach(function (ip) {
                $.ajax({
                    url: 'v1/host',
                    type:"post",
                    contentType:'application/json',
                    data: JSON.stringify({
                        ip: ip.trim(),
                        username: data.username.trim(),
                        password: data.password.trim(),
                        sshPort: data.sshPort.trim(),
                        user: data.user.trim(),
                        phone: data.phone.trim()
                    }),
                    success:function(data) {
                        if (ip === hosts[hosts.length-1]){
                            table._policy.policyRole();
                        }
                        if (data.code !== 200){
                            alert(error.join(",")+" 主机保存失败！");
                        }
                    }
                })
            });

            checkReset();
        }
    };

    var upload  = function () {
        var template = new FormData(document.getElementById("template-form"));
        $.ajax({
            url:  "v1/upload",
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
                    table._policy.policyRole();
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
        $user.val(row.user);
        $phone.val(row.phone);
    };

    return init();
};