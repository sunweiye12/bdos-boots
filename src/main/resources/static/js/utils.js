/**
 *  form 表单封装的类
 * @param table
 * @constructor
 */

ipRegex = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/;
valid_ip = function (ip) {
    return ipRegex.test(ip);
};
valid_empty_ip = function (ip) {
    return ip===''||ipRegex.test(ip);
};
valid_empty = function(value){
    return '' !== value;
};
valid_ips = function (ip) {
    var ips = ip.split(",");
    var ip_valid = true;
    if (ips.length>0){
        ips.forEach(function (cur_ip) {
            ip_valid = ip_valid && ipRegex.test(polishIp(ips[0],cur_ip));
        });
    }else{
        ip_valid = false;
    }
    return ip_valid;
};
valid_len = function (value) {
    return  value.length<32&&value.length>3;
};
valid_port = function (port) {
    return port>0&&port<65535;
};
valid_empty_port = function (port) {
    return port===''||port>0&&port<65535;
};
valid_bool = function(flag){
    flag = flag.toLowerCase();
    return flag==='true'||flag==='false';
};

bind_valid= function ($input,memo,invoke) {
    $input.parent().append('<div class="invalid-feedback">'+memo+'!</div>');
    $input.valid_invoke = invoke;
    $input.bind('input propertychange',function () {
        checkReset($input);
        checkActive($input);
    });
};

// 清空input 样式
checkReset = function ($this) {
    // 清楚单个节点的校验展示
    if ($this!==undefined){
        if($this.hasClass("is-valid")){$this.removeClass("is-valid");}
        if($this.hasClass("is-invalid")){$this.removeClass("is-invalid");}
    }else{ // 清楚所有input 的校验展示
        $(".is-valid").each(function () {$(this).removeClass("is-valid");});
        $(".is-invalid").each(function () {$(this).removeClass("is-invalid");})
    }
};

// 校验IP 是否合法
checkActive = function($this,value){
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

//IP转数字
p2int = function(ip) {
    ip = ip.split(".");
    var num = Number(ip[0]) * 256 * 256 * 256 + Number(ip[1]) * 256 * 256 + Number(ip[2]) * 256 + Number(ip[3]);
    num = num >>> 0;
    return num;
};

//数字转IP
int2ip = function(num) {
    var str;
    var tt = [];
    tt[0] = (num >>> 24) >>> 0;
    tt[1] = ((num << 8) >>> 24) >>> 0;
    tt[2] = (num << 16) >>> 24;
    tt[3] = (num << 24) >>> 24;
    str = String(tt[0]) + "." + String(tt[1]) + "." + String(tt[2]) + "." + String(tt[3]);
    return str;
};

formJson = function ($form) {
    var data = {};
    $form.serializeArray().forEach(function (row) {
        data[row.name] = row.value.trim();
    });
    return data;
};