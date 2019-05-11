<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <%String path=request.getContextPath(); %>
    <script>
        var ctx = "<%=path%>";
        var appUrl = (window.location+'').split('/');
        var basePath = appUrl[0]+'//'+appUrl[2]+'/'+appUrl[3]+'/';
    </script>
    <style>
        .bs-checkbox label {margin: 2px 0 0 0;}
        .input-group-prepend span {padding: 10px 12px 6px;}
    </style>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="<%=path %>/bootstrap-4.3.1-dist/css/bootstrap.css">
    <link rel="stylesheet" href="<%=path %>/bootstrap-table/dist/bootstrap-table.css">
    <link rel="stylesheet" href="<%=path %>/fontawesome-free-5.7.2/css/all.css">

    <script src="<%=path %>/jquery-3.3.1/jquery-3.3.1.js" ></script>
    <script src="<%=path %>/bootstrap-4.3.1-dist/js/bootstrap.bundle.js" ></script>
    <script src="<%=path %>/bootstrap-table/dist/bootstrap-table.js" ></script>
    <script src="<%=path %>/bootstrap-table/dist/extensions/toolbar/bootstrap-table-toolbar.js" ></script>
    <script src="<%=path %>/bootstrap-table/dist/extensions/i18n-enhance/bootstrap-table-i18n-enhance.min.js"></script>
    <script src="<%=path %>/jquery.nicescroll-3.7.6/jquery.nicescroll.js" ></script>
</head>
<body>
<nav class="navbar sticky-top navbar-expand-lg navbar-dark bg-dark">
    <a class="navbar-brand" href="#">集群安装</a>
    <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <ul class="navbar-nav mr-auto">
        </ul>
        <form id="template-form">
            <a href="<%=path %>/template/host_template.xls">下载模板 <i title="下载" class="fas fa-download"></i></a>
            <a class="btn btn-primary" href="javascript:void(0);"  data-toggle="modal" data-target="#host-add"  id="add_btn"><i title="添加主机" class="fa fa-plus"></i> 添加主机</a>
            <a class="btn btn-primary" href="javascript:void(0);" onclick="$('#template').click()"><i title="导入" class="fas fa-upload" type="file" ></i> 导入模板主机</a>
            <input type="file" id="template" name="template" class="d-none" />
            <a class="btn btn-primary" href="javascript:void(0);"  id="host_check" data-loading-text="校验中...."><i title="校验" class="fa fa-sync"></i> 主机校验</a>
            <a class="btn btn-danger" href="javascript:void(0);" id="install_cluster" > 安装集群 <i title="安装集群" class="fa fa-power-off"></i></a>
        </form>
    </div>
</nav>
<div class="modal" id="host-add" role="dialog" aria-labelledby="添加主机" >
    <div class="modal-dialog modal-lg modal-dialog-centered" role="document" data-show="true">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="exampleModalLabel">添加主机</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
            </div>
            <div class="modal-body" >
                <form id='host_form' class="form-horizontal ">
                    <div class="form-group hostSingle">
                        <label class="control-label">主机IP:</label>
                        <div class="input-group">
                            <div class="input-group-prepend"><span class="input-group-text fa fa-desktop"></span></div>
                            <input id="ip" name="ip" class="form-control" placeholder="192.168.1.1" type="text" value=""/>
                        </div>
                    </div>
                    <div class="form-row hostGroup">
                        <div class="form-group col-md-6">
                            <label class="control-label">开始IP:</label>
                            <div class="input-group">
                                <div class="input-group-prepend"><span class="input-group-text fa fa-desktop"></span></div>
                                <input id="startIp" name="startIp" class="form-control" placeholder="192.168.1.1" type="text" value=""/>
                            </div>
                        </div>
                        <div class="form-group col-md-6">
                            <label class="control-label">结束IP:</label>
                            <div class="input-group">
                                <div class="input-group-prepend"><span class="input-group-text fa fa-desktop"></span></div>
                                <input id="endIp" name="endIp" class="form-control" placeholder="192.168.1.5"  type="text" value=""/>
                            </div>
                        </div>
                    </div>
                    <div class="form-group custom-control custom-checkbox">
                        <input type="checkbox" class="custom-control-input" id="isGroup">
                        <label class="custom-control-label" for="isGroup">连续主机</label>
                    </div>
                    <div class="form-group " >
                        <label class="control-label" for="username">用户名:</label>
                        <div class="input-group">
                            <div class="input-group-prepend"><span class="input-group-text fa fa-user"></span></div>
                            <input id="username" name="username" class="form-control" placeholder="username" type="text" value=""/>
                        </div>
                    </div>

                    <div class="form-group " >
                        <label class="control-label" for="password">密码:</label>
                        <div class="input-group">
                            <div class="input-group-prepend"><span class="input-group-text fa fa-lock"></span></div>
                            <input id="password" name="password" class="form-control" placeholder="password" type="text" value=""/>
                        </div>
                    </div>

                    <div class="form-group " >
                        <label class="control-label" for="sshPort">ssh端口:</label>
                        <div class="input-group">
                            <div class="input-group-prepend"><span class="input-group-text fa fa-exchange-alt"></span></div>
                            <input id="sshPort" name="sshPort" class="form-control" placeholder="22" type="text" value="22"/>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-dismiss="modal">取消</button>
                <button type="button" class="btn btn-primary" id="save_host">保存</button>
            </div>
        </div>
    </div>
</div>

<div class="container-fluid">
    <div id="toolbar">
        <form id="global_form" >
            <div class="row">
                <label for="k8s_virtual_ip" class="col-sm-2 col-form-label">VIP：</label>
                <div class="col-sm-10">
                    <input type="text" class="form-control" id="k8s_virtual_ip" placeholder="K8S虚拟访问IP">
                </div>
            </div>
        </form>
    </div>
    <table id="host_table"></table>
</div>


<div class="modal bs-example-modal-lg" id="installModel" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog modal-xl modal-dialog-centered" >
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title" >安装进度 <span class="small" id="processTitle"></span></h4>

                <button type="button" id="closeLog"  class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
            </div>
            <div class="modal-body">
                <div id="installPercent" class="progress"><div role="progressbar" ></div></div>
                <div id="log-container" >
                    <div class="logInsert" ></div>
                </div>
            </div>
            <div class="modal-footer">
                <button id="resumeBtn" type="button" class="btn btn-primary" data-loading-text="操作中....">继续安装</button>
            </div>
        </div>
    </div>
</div>
</body>
<script src="<%=path %>/js/play.js"></script>
<script src="<%=path %>/js/policy.js"></script>
<script src="<%=path %>/js/check.js"></script>
<script src="<%=path %>/js/form.js"></script>
<script src="<%=path %>/js/install.js"></script>
<script src="<%=path %>/js/delete.js"></script>
<script src="<%=path %>/js/table.js"></script>

<script type="text/javascript">
    $(function(){
        new Table();
    });
</script>
</html>
