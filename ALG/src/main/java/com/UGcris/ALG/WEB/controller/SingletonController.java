package com.UGcris.ALG.WEB.controller;

import com.UGcris.ALG.WEB.bo.Singleton;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @Author UGcris
 * @Date 2018/10/9
 */
@RestController
public class SingletonController {
    
    /** 
    * @Description:  
    * @Param:  
    * @return:  
    * @Author: UGcris 
    * @Date: 2018/10/18 
    */ 
    @RequestMapping(value="/flesh", method = RequestMethod.GET)
    public Singleton flesh() {
        Singleton.initInstance();
        Singleton singleton= Singleton.getInstance();
        double cc=20d;
        for(int i=0;i<20;i++){
            singleton.setCount((int)(i/cc*100));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        singleton.setCount(100);
        return  singleton;
    }

    /** 
    * @Description: 页面轮循获取当前进度 
    * @Param:
    * @return:  
    * @Author: UGcris 
    * @Date: 2018/10/18 
    */ 
    @RequestMapping(value="/count", method = RequestMethod.GET)
    public int count() {
        Singleton singleton= Singleton.getInstance();
        return  singleton.getCount();
    }
}
