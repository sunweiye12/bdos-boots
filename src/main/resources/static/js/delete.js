/**
 *  节点删除
 *
 *
 *  主机检查按钮绑定delete_node
 * @param table table 表对象
 * @constructor
 */

var DELETE_NODE = function (play_code) {
    var _this = this;
    
    this.delete_node = function (ip) {
        var list = {targets: [ip]};
        
        $.ajax({
            type:"post",
            url: "v1/cluster/exec/"+play_code,
            contentType: 'application/json',
            data:JSON.stringify(list.targets),
            async: false,
            success:function(result){
                if(result.code===200){
                    task_id = result.data;
                }else{
                    console.log(play_code+"任务创建失败!")
                }
            }
        });
        
        
        
        $.ajax({
            type:"delete",
            url: 'v1/cluster/callback/host',
            contentType:'application/json',
            data:ip,
            success:function(data){
                if (data.code === 200){
                	var table = new Table();
                	table.refresh();
                }else {
                    console.log(data.code);
                }
            }
        });

    }
};