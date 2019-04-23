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

        var play = new Play(play_code,{
        	targets : targets,
            read_last: true,
            interval_time: 1000,
            start: function(play,data){
                if (data.code===200){
                    show();
                }else{
                    alert ("错误编码："+data.code +" 描述信息："+data.data[0]);
                }
            },
            callback: function (play,data) {
                if (data.code ===200){

                    // 更新执行日志
                    $(".logInsert").html(data.data.stdout);

                    // 滚动进度
                    active_show(_this.status(),data.data.size,data.data.status==='2');

                    // 安装失败，释放按钮操作
                    if(data.data.status==='3'){
                        $resume_btn.button('reset');
                    }

                    if(data.data.status==='2'){
                        // 跳转到主机页面
                    }
                }
            }
        });

        init_playbook(play.playbooks());

        // 将任务的启动接口暴露给install 对象
        _this.start = play.start;
        _this._play = play;

        $resume_btn.on('click',play.resume);

        return _this;
    };

    var init_playbook = function (playbooks){
        cur_size = 0;
        var $install = $("#installPercent");
        $install.html("");
        for ( var i in playbooks) {
            $install.append('<div class="progress-bar progress-bar-striped " role="progressbar" style="width: 0">'+playbooks[i].playbookName+'</div>');
        }
    };

    // 展示任务界面操作
    var show = function () {
        $("#installModel").modal('show');
    };

    var active_show = function (task_status,size,success) {

        /**
         *  展示playbook 动态效果
         * @param $playbook  指定的playbook jq对象
         * @param last 当前playbook 是否最新的playbook
         * @link success  但钱任务的状态是否成功
         */
        var show_playbook = function ($playbook, last, size) {
            $playbook.addClass("progress-bar-success").attr('style','width: 10%');
            // 展示静态的 (除了最后一个其他的都是静止的，任务静止都静止)
            if (!last||!task_status&&$playbook.hasClass("active")){
                $playbook.removeClass("active");
            }
            // 展示动态的（只有最后一个并且任务是运行状态的才是active状态）
            if (task_status&&last&&!$playbook.hasClass("active")){
                $playbook.addClass("active").html(_this._play.playbooks()[size].playbookName);
            }
            // 展示成功的 (成功了或者不是最后的playbook 展示绿色)
            if (!last||success&&!$playbook.hasClass("progress-bar-success")){
                $playbook.addClass("progress-bar-success").removeClass("progress-bar-danger");
            }
            // 展示失败的 （任务静止了并且没成功还是最后一个显示红色）
            if (!success&&last&&!task_status&&!$playbook.hasClass("progress-bar-danger")){
                $playbook.addClass("progress-bar-danger").removeClass("progress-bar-success");
            }
        };

        var $playDiv = $("#installPercent div");
        //更新playbook进度
        if (cur_size<=size){
            for(; cur_size<=size ; cur_size++){
                show_playbook($playDiv.eq(cur_size),cur_size===size,cur_size);
            }
        }else if(!task_status){ // 如果运行终止更新成功或者失败
            show_playbook($playDiv.eq(size),true,size);
        }

        $log_container.getNiceScroll().resize();
        //$log.getNiceScroll(0).doScrollTop(1000000,5000);
        var div = document.getElementById('log-container');
        div.scrollTop = div.scrollHeight;
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