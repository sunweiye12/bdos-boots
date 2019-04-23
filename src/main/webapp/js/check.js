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
 * @constructor
 */

var HostCheck = function (table) {
    var _this = this;
    var play_code = "host_check";
    var _table = table;
    var $check_btn = $('#host_check');
    var count = 0;
    
    var init  = function () {
        $check_btn.on('click',_this.checkAll);
    };

    var getDev = function (ip) {
        var dev = [];
        $.ajax({
            url: basePath +"v1/cluster/dev?ip="+ip,
            async: false,
            success: function (data) {
                if (data.code === 200){
                    dev = data.data;
                }
            }
        });
        return dev;
    };

    var callback = function (play,data) {
        var msg_json=JSON.parse(data.data.msg);
        // 获取节点IP
        for (let ip of play.targets()){
            var host= {ip: ip,devs:[],message:"",status:'0'};
            if (msg_json!==undefined){
                for (let msg of msg_json){
                    host.message+=msg
                }
            }
            // 任务正在安装中
            if (play.status()){
                host.status='3';
            }else{ //  任务已经停止
                if (data.data.status==='2'){
                    host.status = '2';
                    host.devs = getDev(ip);
                }else{
                    host.status = '1'
                }
                count--;
                if (count===0){

                }
            }
            _table.updateHostStatus(host);
        }
    };

    this.check  = function (ip) {
        _table.updateHostStatus({ip: ip,devs:[],message:"",status:'3'});
        var play = new Play(play_code,{
            targets: [ip],
            read_last:false,
            interval_time: 1000,
            callback: callback
        });
        return play.start();
    };
    
    this.checkAll = function () {
        for (let host of _table.getHosts()){
            // 只对未校验成功的主机进行骄傲眼
            if (host.status!=='2'){
                var play = new Play(play_code,{
                    targets: [host.ip],
                    read_last:false,
                    interval_time: 1000,
                    callback: callback
                });

                var status = play.start();

                if (!status){
                    console.log("主机"+host.ip+" 检查任务创建失败！")
                }else{
                    count++;
                }
            }
        }
    };

    return init();
};