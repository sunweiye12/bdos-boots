package com.bonc.bdos.controller.cluster;

import com.bonc.bdos.service.cluster.Global;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cluster")
public class ClusterManagerController {

    @RequestMapping(value = "")
    public String index(){
        if (Global.k8sIsInstalled()){
            return "host";
        }else{
            return "index";
        }
    }
}
