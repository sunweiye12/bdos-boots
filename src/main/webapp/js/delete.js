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
    
    this.delete_node  = function (ip) {
        var play = new Play(play_code,{
            targets: [ip],
            read_last:false,
            interval_time: 1000,
        });
        return play.start();
    };
    
};