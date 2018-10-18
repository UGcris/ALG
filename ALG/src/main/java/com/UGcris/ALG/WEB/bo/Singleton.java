package com.UGcris.ALG.WEB.bo;

/**
 * @Description 单例模式，实现真实进度条
 * @Author UGcris
 * @Date 2018/10/9
 */
public class Singleton {

    private static Singleton instance;
    private Integer  count= 0;
    public Integer getCount(){
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
    public Integer count()
    {
        count = count + 1;
        return count;
    }
    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }

    public static void initInstance() {
        instance = new Singleton();
    }
}
