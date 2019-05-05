#!/bin/bash

pack-bdos-boot ()
{
  #下载最新安装包
  #复制到本地
  #
  read -p "bdos-boots项目该主机上是否已经下载（git clone xx） 是：y  否：n" choice
  if [[ "$choice" == "y" ]] ;then
     read -p "请输入bdos-boot项目的路径" bdos_boot_dir
  else
    echo "下载bdos-boot项目，代码下载到当前路径，保证当前主机具有下载代码的权限"
    #echo "代码下载到当前路径："`pwd`;
    bdos_boot_dir=`pwd`/bdos-boots;
    echo " git clone ssh://git@code.bonc.com.cn:10022/017549/bdos-boots.git "
    git clone ssh://git@code.bonc.com.cn:10022/017549/bdos-boots.git;
  fi
  
  if [ ! -d "${bdos_boot_dir}" ];then 
    echo "bdos-boots项目路径：${bdos_boot_dir} 不存在或者当前git clone下载失败" ;
    exit;
  else 
    echo "开始打包,当前bdos-boot项目的路径为：bdos_boot_dir";
  fi
  
  export bdos_boot_path=$work_dir/work
  cd $bdos_boot_dir;
  export bdos_hashcode=`git log --stat | grep commit|awk '{print $2}'|head -1`
  git pull;
  mvn clean package -P prod;

  cp $bdos_boot_dir/target/bdos.war ${bdos_boot_path}
}

#门户安装镜像包 bdos bpm bconsole cas portal secrity
pack-bcos ()
{
  read -p "请输入门户安装镜像包的路径：" bcos_dir
  export bcos_path=$work_dir/work

  while [ ! -d "${bcos_dir}" ]
  do
    read -p "当前文件夹不存在，请重新输入门户安装镜像包的路径：" bcos_dir
  done


  if [[ `ll ${bcos_dir} |wc -l ` > 1 ]] ;then
    cp bcos_dir/* $work_dir/work
  else
    echo "文件夹" ${bcos_dir} "未上传镜像安装包,请上传完镜像包之后操作"
    exit
  fi
}
 
 pack-ansible ()
 {

  read -p "bdos-boots项目该主机上是否已经下载（git clone xx） 是：y  否：n" choice
  if [[ "$choice" == "y" ]] ;then
     read -p "请输入bdos-boot项目的路径" ansible_dir
  else
    echo "下载bdos-boot项目，代码下载到当前路径，保证当前主机具有下载代码的权限"
    #echo "代码下载到当前路径："`pwd`;
    bdos_boot_dir=`pwd`/bdos-boots;
    echo " git clone ssh://git@code.bonc.com.cn:10022/017549/bdos-boots.git "
    git clone ssh://git@code.bonc.com.cn:10022/017549/bdos-boots.git;
  fi


  dirpath=/version-control/bdos-ansible
  export ansible_path=/data02/version/develop/ansible/
  cd $dirpath;
  export ansible_hashcode=`git log --stat | grep commit|awk '{print $2}'|head -1`
  export ansible_branch=`git branch | head -1 | awk '{ print $2 }'`
  git pull;
  cp -r $dirpath $ansible_path
  cd ${ansible_path}bdos-ansible
  rm -rf .git*
 }

 pack-images ()
 {
  dirpath=/data02/harbor_data/data
  tar -czvf harbor-registry.tar.gz $dirpath/database $dirpath/registry
  cp $dirpath/harbor-registry.tar.gz /data02/version/develop/images/
 }

 update-images ()
 {
  #生成环境  上传镜像更新镜像包操作：需先按照开发环境更新本机harbor仓库，变更images_update_history镜像记录文件
  #
  #1.先判断images_update_history更新文件中有没有大于当前环境的时间记录
  #2.如果有大于当前环境的时间记录，把这些记录提取出来更新镜像包
  export image-path=
  echo "更新生产环境"${name}"镜像"
  harbor_url=172.16.3.122:8443
  ansible-playbook image.yml -e '{"name": "${name}","image-path": "${image-path}","harbor_url","${harbor_url}"}'

 }

 export workdir=/data02/version/product
 pack-all()
 {

  update-relesepath=`pwd`
  echo "创建bdos-boot最新安装包"
  pack-bdos-boot
  cp ${bdos-path}bdos.war $update-relesepath

  echo "下载ansible代码包"
  pack-ansible

  #bconsole  blogic安装包
  echo "获取门户镜像包"
  pack-bcos

  echo "创建harbor最新压缩镜像包"
  pack-images

  echo "创建完整压缩包"
  cp ${update-relesepath}bdos.war ${ansible_path}bdos-ansible/roles/deploy/files/bdos/
  mv /data02/version/develop/images/harbor-registry.tar.gz ${ansible_path}bdos-ansible/roles/harbor/files/
  tar -czvf ${update-relesepath}bdos-ansible.tar.gz ${ansible_path}bdos-ansible

  echo "记录一键部署安装包版本号"
  TIME=$(date "+%Y-%m-%d_%H:%M:%S")
  mkdir ${workdir}/${name}
  echo "bdos-boots-git-version " $bdos_hashcode  > ${workdir}/${name}/bdos-boots.txt
  echo "ansible-git-version " $ansible_hashcode  > ${workdir}/${name}/ansible.txt

  echo "仓库镜像更新信息"
  touch /data02/version/product/ansible/$name/images_update_info
  echo `date "+%Y-%m-%d %H:%M:%S"` pid >> /data02/version/product/ansible/$name/images_update_info 

  echo "记录安装包分支和版本号环境记录信息，需上传主机列表信息，项目信息"
  
  rm -rf ${bdos-path}bdos.war
  rm -rf ${bdos-path}bdos.war
  rm -rf ${ansible_path}*
 }


 {
  read -p "请输入打包的工作路径(如/tmp 不输入默认为当前路径):" work_dir
  if [[ "$work_dir" == "" ]] ;then
    echo "当前工作路径为："`pwd`;
    work_dir=`pwd`;
  else 
    echo "工作路径为："$work_dir
  fi 
  export  work_dir

  mkdir $work_dir/product
  mkdir $work_dir/work

  read -p "请输入生产环境编码(编码唯一):" name
  export name 

    declare -A menus=(
        ["1"]="pack-all bdos-ansible-images-rpm完整安装包"
        ["2"]="pack-bcos 更新门户安装镜像包"
        ["3"]="pack-images 仅harbor镜像压缩包"
        ["4"]="update-images 更新镜像包"
      )

    while true;  
    do
      menu
      read -p "请输入操作项:" key
      cmd_key=${menus["$key"]}
      if [[ $cmd_key =~ "install_" ]];then
        $cmd_key > $work_path/output/${key}.sh
      else
        $cmd_key
      fi
      read -p "输入任回车键继续"
    done 
  }
