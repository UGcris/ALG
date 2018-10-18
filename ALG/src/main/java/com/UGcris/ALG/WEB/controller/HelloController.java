package com.UGcris.ALG.WEB.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Description
 * @Author UGcris
 * @Date 2018/10/9
 */
@Controller
public class HelloController {

    /**
     * 文字内容
     * @return
     */
    @RequestMapping("/")
    String home() {
        return "Hello World!";
    }

   /** 
   * @Description: 跳转至html页面 
   * @Param:  
   * @return:  
   * @Author: UGcris 
   * @Date: 2018/10/18 
   */ 
    @RequestMapping("/hello")
    public String hello(){
            return  "hello";
    }




}
