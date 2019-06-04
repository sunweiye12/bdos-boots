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
            host_msg[ip]=[];
        });
        var play = new Play(play_code,{
            read_last:false,
            interval_time: 1000,
            callback: function (play,data) {
                // 当执行下一个playbook 或者任务接数的时候，需要刷新一下表格
                if (index!==data.data.size||!play.status()){
                    _table.reload();
                    index = data.data.size;
                    JSON.parse(data.data.msg).forEach(function (message) {
                        var ip_json=JSON.parse(message);
                        if (host_msg[ip_json.ip].indexOf(ip_json.message)===-1){
                            host_msg[ip_json.ip].push(ip_json.message);
                        }
                    });
                }
                if (!play.status() && typeof finish === "function"){
                    finish(data.data);
                }
            }
        });
        return play.start(targets);
    };
    
    this.handleAll = function () {
        var targets = _table.getSelectFields("ip");
        if (targets.length === 0){
            alert(" 请选择要操作的主机节点！主机操作请谨慎");
            return;
        }
        _this.handle(targets);
    };
    
    this.getHostMsg = function (ip) {
        return host_msg[ip];
    };

};