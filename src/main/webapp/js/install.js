var InstallCluster = function (play_code,targets) {
    var $resume_btn = $("#resumeBtn");
    var $log_container = $("#log-container");
    var _this = this;
    var init = function () {
        // 初始化 滚动日志
        $log_container.css({
            height: $(window).height()*7/10+"px"
        });
        this.scroll = $log_container.niceScroll({
            cursorcolor:"gray",
            cursorwidth:"16px",
            enablescrollonselection: true  // 当选择文本时激活内容自动滚动
        });

        var scroll_flag = true;
        var top = 0;
        $log_container.scroll(function () {
            var cur_top = $log_container.scrollTop();
            scroll_flag = cur_top>top;
            top = cur_top;
        });

        var action_btn = function (status) {
            $resume_btn.html(status?"暂停安装":"继续安装");
        };

        var play = new Play(play_code,{
        	targets : targets,
            read_last: true,
            interval_time: 1000,
            start: function(play,data){
                if (data.code===200){
                    show();
                    action_btn(play.status());
                }else{
                    alert ("错误编码："+data.code +" 描述信息："+data.data[0]);
                }
            },
            resume: function(play,data){
                action_btn(play.status());
            },
            callback: function (play,data) {
                if (data.code ===200){
                    // 更新执行日志
                    $(".logInsert").html(data.data.stdout);
                    if (scroll_flag){
                        $log_container.getNiceScroll().resize();
                        var div = document.getElementById('log-container');
                        div.scrollTop = div.scrollHeight;
                    }

                    // 滚动进度
                    active_show(_this.status(),data.data.size,data.data.status);

                    action_btn(play.status());
                }
            }
        });

        // 将任务的启动接口暴露给install 对象
        _this.start = play.start;
        _this._play = play;

        $resume_btn.on('click',function () {
            play.resume(_this.status());
        });

        return _this;
    };

    // 展示任务界面操作
    var show = function () {
        $("#installModel").modal('show');
    };

    var active_show = function (task_status,size,status) {
        var length= _this._play.playbooks().length;
        var percents = parseInt(size*100/length) ;
        var $process = $("#installPercent div");
        $process.attr('style','width: '+percents+"%").html(percents+"%");
        if (!task_status&&$process.hasClass("avtive")){
            $process.removeClass("bg-success").removeClass("active");
        }
        if (status!=='2'&&$process.hasClass("bg-success")){
            $process.removeClass("bg-success");
        }
        if(status!=='3'&&$process.hasClass("bg-danger")){
            $process.removeClass("bg-danger");
        }
        if (task_status&&!$process.hasClass("avtive")){
            $process.addClass("bg-success").addClass("active");
        }
        if(status==='2'&&!$process.hasClass("bg-success")){
            $process.addClass("bg-success");
        }
        if(status==='3'&&!$process.hasClass("bg-danger")){
            $process.addClass("bg-danger");
        }
        $("#processTitle").html(_this._play.playbooks()[size].playbookName);
    };

    /**
     *  判断 play 的执行状态，用于安装集群的时候做判断是否调出弹出窗体
     * @returns {boolean}
     */
    this.status = function () {
        return _this._play.status();
    };

    return init();
};