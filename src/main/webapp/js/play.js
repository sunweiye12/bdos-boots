/**
 *  play 公共的play任务处理类
 * @param play_code  任务编码
 * @param opt  操作对象
 *      read_last = true/false  是否读取上次的任务ID
 * @constructor
 */


var Play = function (play_code,opt) {

    Play.OPT_DEFAULTS = {
        targets: [],
        read_last:false,
        interval_time: 1000,
        // 启动的回掉函数
        start: function(play,data){
            console.log(play.uuid());
        },
        // 暂停或者恢复后的回掉函数
        resume: function(play,data){
            console.log(play.uuid());
        },
        // 日志输出的回掉函数
        callback: function (play,data) {
            console.log(play.uuid());
        }
    };

    //  获取任务ID
    var task_name = play_code;
    var task_status = false;
    var _this = this;

    // 生成 最终的操作选项
    const _opt = $.extend({}, Play.OPT_DEFAULTS, opt);

    // 创建静态缓存
    var playbooks = [];

    var task_id = "";
    var interval;

    var init = function () {
        if (_opt.read_last){
            $.ajax({
                type : "get",
                url : basePath + "v1/cluster/exec/task?playCode="+task_name,
                async: false,
                success : function(result) {
                    if(result.code===200){
                        task_id = result.data;
                        _this.status(true);
                        _opt.start(_this,result);
                    }else{
                        _this.status(false);
                    }
                }
            });
        }

        // 加载playbooks 执行步骤清单
        $.ajax({
            type : "get",
            url : basePath + "v1/cluster/exec/playbooks?playCode="+task_name,
            async: false,
            success : function(result) {
                playbooks = result.data;
            }
        });

        interval = setInterval(get_log,_opt.interval_time);
        return _this;
    };

    var get_log = function(){
        if(!task_status||task_id===""){
            return;
        }

        $.ajax({
            type : "get",
            url : basePath + "v1/cluster/exec/query?uuid="+task_id,
            contentType:'application/json',
            async: false,
            success : function(result) {
                if(result.code===200){
                    _this.status(result.data.status!=='2'&&result.data.status!=='3');
                    _opt.callback(_this,result);
                }
            }
        });
    };

    this.uuid = function () {
        return task_id;
    };
    
    this.targets = function () {
        return _opt.targets;
    };

    this.status = function (status) {
        if (status!==undefined){
            task_status = status;
        }
        return task_status;
    };

    this.playbooks = function () {
        return playbooks;
    };

    // 暂停或者继续执行当前任务
    this.resume=function (status) {
        // 校验任务是否存在
        if (task_id === ""){
            return false;
        }
        // 进行任务调度
        $.ajax({
            type:"post",
            url:basePath+"v1/cluster/exec/"+(status?"pause":"resume"),
            contentType:'application/json',
            data: task_id,
            async: false,
            success:function(result){
                _this.status(result.code===200) ;
                if(!task_status){
                    console.log(task_name+"继续执行失败!")
                }

                _opt.resume(_this,result);
            }
        });
        return task_status;
    };

    this.start = function () {
        // 如果有任务在执行，直接调出执行窗体
        if (task_status){
            _opt.start(_this,{code:200});
            return task_status;
        }

        $.ajax({
            type:"post",
            url:basePath+"v1/cluster/exec/"+task_name,
            contentType: 'application/json',
            data:JSON.stringify(_opt.targets),
            async: false,
            success:function(result){
                _this.status(result.code===200) ;
                if(task_status){
                    task_id = result.data;
                }else{
                    console.log(task_name+"任务创建失败!")
                }
                _opt.start(_this,result);
            }
        });

        return task_status;
    };


    this.destroy = function(){
        if (interval!==undefined){
            window.clearInterval(interval);
        }
    };

    return init();
};