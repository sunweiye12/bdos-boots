/**
 *  主机检查的功能主要是根据IP调度任务模块，
 *
 *  主机检查中，
 *  主机检查成功，
 *  主机检查失败，
 *
 *  让检查按钮绑定checkAll
 *  主机检查按钮绑定check
 * @param table table 表对象
 * @param play_code 处理编码
 * @constructor
 */

// 存储全局 配置信息
host_msg = {};
var HostHandle = function (table,play_code) {
    var _this = this;
    var _table = table;
    this.handle  = function (targets,finish) {
        var index = true;
        // 初始化消息对象
        targets.forEach(function (ip) {
            host_msg[ip]='';
        });
        var play = new Play(play_code,{
            read_last:false,
            interval_time: 1000,
            callback: function (play,data) {
                var msg_json=JSON.parse(data.data.msg);
                for (let ip of targets){
                    var msgs = "";
                    if (msg_json!==undefined){
                        for (let msg of msg_json){
                            msgs+=msg
                        }
                    }
                    if (msgs!==""){
                        host_msg[ip]=msgs+host_msg[ip]+";";
                    }
                }
                if (index!==data.data.size){
                    $.get("v1/host", function(result){
                        _table.reload(result.data);
                    });
                    index = data.data.size;
                }
                if (data.data.status==='2'||data.data.status==='3'){
                    $.get("v1/host", function(result){
                        _table.reload(result.data);
                        $('.popover').remove();
                        $('[data-toggle="popover"]').popover('show');
                    });
                    if (typeof finish === "function"){
                        finish(data.data);
                    }
                }
            }
        });
        return play.start(targets);
    };
    
    this.handleAll = function () {
        var hosts = _table.getSelectHosts();
        if (hosts.length === 0){
            alert(" 请选择要操作的主机节点！主机操作请谨慎");
            return;
        }
        for (let host of _table.getSelectHosts()){
            _this.handle([host.ip]);
        }
    };
    
    this.getHostMsg = function (ip) {
        return host_msg[ip];
    };

};