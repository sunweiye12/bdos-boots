package com.bonc.bdos.service.cluster.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bonc.bdos.consts.ReturnCode;
import com.bonc.bdos.service.cluster.entity.SysClusterHost;
import com.bonc.bdos.service.cluster.entity.SysClusterHostRole;
import com.bonc.bdos.service.cluster.entity.SysClusterRole;
import com.bonc.bdos.service.cluster.entity.SysClusterRoleDev;
import com.bonc.bdos.service.cluster.exception.ClusterException;
import com.bonc.bdos.service.cluster.repository.SysClusterHostRepository;
import com.bonc.bdos.service.cluster.repository.SysClusterHostRoleRepository;
import com.bonc.bdos.service.cluster.repository.SysClusterRoleDevRepository;
import com.bonc.bdos.service.cluster.service.HostService;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service("hostService")
public class HostServiceImpl implements HostService{
	
	private final SysClusterHostRepository clusterHostDao;
	private final SysClusterHostRoleRepository clusterHostRoleDao;
	private final SysClusterRoleDevRepository clusterRoleDevDao;

	@Autowired
	public HostServiceImpl(SysClusterHostRepository clusterHostDao, SysClusterHostRoleRepository clusterHostRoleDao, SysClusterRoleDevRepository clusterRoleDevDao) {
		this.clusterHostDao = clusterHostDao;
		this.clusterHostRoleDao = clusterHostRoleDao;
		this.clusterRoleDevDao = clusterRoleDevDao;
	}

	private void checkHostLock(String ip) throws ClusterException{
		Optional<SysClusterHost> optional = clusterHostDao.findById(ip);
		if(optional.isPresent()&&optional.get().getHostLock()){
			throw new ClusterException(ReturnCode.CODE_CLUSTER_HOST_CHECK,"有正在执行的任务使用该主机，请稍后更新集群状态");
		}
	}

	@Override
	@Transactional
	public void saveHost(SysClusterHost host)  {
		//1.校验      有任务在执行，主机处于锁住状态，无法进行更新
		//dqy
		checkHostLock(host.getIp());
		clusterHostDao.save(host);
		
		//初始化默认角色 ，如果存在默认角色更新角色状态
        SysClusterHostRole defaultRole = clusterHostRoleDao.findByIpAndRoleCode(host.getIp(),SysClusterRole.DEFAULT_ROLE);
        if (null==defaultRole){
            SysClusterHostRole hostRole = new SysClusterHostRole(host.getIp(),SysClusterRole.DEFAULT_ROLE);
            clusterHostRoleDao.save(hostRole);
        }else{
            defaultRole.setStatus(host.getStatus());
            clusterHostRoleDao.save(defaultRole);
        }
	}

	@Override
	@Transactional
	public void deleteHost(String ip, boolean installflag)  {
	    
	    if(!installflag) {
	     // 校验1： 主机是否有锁
	        checkHostLock(ip);

	        //校验2、主机上没有安装好的角色
	        List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findByIp(ip);
	        if(hostRoles!=null) {
	            List<String> installedRoleInfo = new ArrayList<>();
	            for(SysClusterHostRole hostRole:hostRoles) {
	                if(hostRole.isInstalled()&&!SysClusterRole.DEFAULT_ROLE.equals(hostRole.getRoleCode())) {
	                    installedRoleInfo.add(hostRole.getStatusDesc());
	                }
	            }
	            if (!installedRoleInfo.isEmpty()){
	                throw new ClusterException(ReturnCode.CODE_CLUSTER_ROLE_INSTALLED,installedRoleInfo,"主机上存在已安装的角色！");
	            }
	        }
	    }
		
		clusterRoleDevDao.deleteByIp(ip);
		clusterHostRoleDao.deleteByIp(ip);
		clusterHostDao.deleteById(ip);
	}

	@Override
	public List<SysClusterHost> findHosts() {
		//1.获取SysClusterHost   主机信息
		List<SysClusterHost> hosts = clusterHostDao.findAll();

		//2.根据主机列表获取   SysClusterHostRole  主机角色信息
		for(SysClusterHost host : hosts) {
			//查询主机所有的角色
			List<SysClusterHostRole> hostRoles = clusterHostRoleDao.findByIp(host.getIp());
			
			//3.遍历所有角色
			for(SysClusterHostRole hostRole: hostRoles) {
				List<SysClusterRoleDev> devList = clusterRoleDevDao.findByHostRoleId(hostRole.getId());
				// 3.1 role == default  将设备添加到主机信息里        
				if(SysClusterRole.DEFAULT_ROLE.equals(hostRole.getRoleCode())) {
					host.setDevs(devList);
				} else {
					//3.2   查询角色对应的设备 添加到角色中
					hostRole.setDevs(devList);
					host.addRole(hostRole);
				}
			}
		}
		return hosts;
	}

	@Override
	@Transactional
	public void enableDev(String id, boolean enable)  {
		// 检查设备是否存在
		Optional<SysClusterRoleDev> optional = clusterRoleDevDao.findById(id);
		if (!optional.isPresent()) {
			throw  new ClusterException(ReturnCode.CODE_CLUSTER_DEV_NOT_EXIST,"设备不存在！");
		}

		SysClusterRoleDev dev = optional.get();
		
		if (dev.isUsed()&&!enable) {
			throw  new ClusterException(ReturnCode.CODE_CLUSTER_DEV_IN_USERD," 设备已经被使用");
		}
		//状态为可用  0 1
		if (dev.isEnable()){
		    dev.disable();
		}else {
		    dev.enable();
		}
		//  启停设备
		clusterRoleDevDao.save(dev);
	}

    @Override
    @Transactional
    public void saveTemplate(InputStream is) throws IOException {
        //HSSFWorkbook代表整个Excle
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook(is);
        String[] cols = new String[]{"ip","username","password","sshPort"};

        //循环每一页，并处理当前页
        for(int sheetCur=0,sheetNum=hssfWorkbook.getNumberOfSheets(); sheetCur<sheetNum ; sheetCur++){
            HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(sheetCur); //获得HSSFSheet的某一页
            if(hssfSheet == null){
                continue;
            }
            //处理当前页,循环读取每一行
            for(int rowCur = 0,rowNum = hssfSheet.getLastRowNum(); rowCur<=rowNum; rowCur++){
                try{
                    if (rowCur==0){
                        continue;
                    }

                    HSSFRow hssfRow = hssfSheet.getRow(rowCur);  //HSSFRow表示行
                    int minCol = hssfRow.getFirstCellNum();
                    int maxCol = hssfRow.getLastCellNum();

                    JSONObject hostJson = new JSONObject();
                    //遍历每一行
                    for(int colCur = minCol; colCur<maxCol; colCur++){
                        HSSFCell hssfCell = hssfRow.getCell(colCur);
                        if(hssfCell == null){
                            continue;
                        }
                        hostJson.put(cols[colCur],getCell(hssfCell));
                    }
                    saveHost(JSON.toJavaObject(hostJson,SysClusterHost.class));
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

	private static Object getCell(HSSFCell hssfCell){
        switch (hssfCell.getCellType()) {
        case Cell.CELL_TYPE_BOOLEAN:
            return hssfCell.getBooleanCellValue();
        case Cell.CELL_TYPE_FORMULA:
            return hssfCell.getCellFormula();
        case Cell.CELL_TYPE_NUMERIC:
            return hssfCell.getNumericCellValue();
        case Cell.CELL_TYPE_STRING:
            return hssfCell.getStringCellValue();
        default:
            return null;
        }
    }

	@Override
	public List<SysClusterRoleDev> findDev(String ip) {
		SysClusterHostRole hostRole = clusterHostRoleDao.findByIpAndRoleCode(ip,SysClusterRole.DEFAULT_ROLE);
		if (null!=hostRole){
			return clusterRoleDevDao.findByHostRoleId(hostRole.getId());
		}else{
			return new ArrayList<>();
		}
	}
}
