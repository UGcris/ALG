package com.UGcris.ALG.stringMatching;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description KMP算法——用于单模匹配。
 * @Author UGcris
 * @Date 2018/9/7
 */
public class KMP {
    /**
     * @Description
     * @Params String
     * @Return int[]
     * @Author UGcris
     * @Aate 2018/9/7
     */
    private int[] getNext(String str){
        //存储对应下标回溯的位置
        int[] next=new int[str.length()+1];
        //第一位回溯未开始匹配
        next[0]=next[1]=0;
        //如果下标为i的字符与下标为j的字符匹配则向后移，匹配下一个字符
        //如果下一个字符未匹配，则回到下标i对应的回溯下标J
        //最大公共子串
        int j=0;
        for(int i=1;i<str.length();){
            while(j>0 && str.charAt(i)!=str.charAt(j))
                j=next[i];
            if(str.charAt(i)==str.charAt(j)) j++;
            next[++i]=j;
        }
        return next;
    }

    /**
     * @Description
     * @Params str,matchedStr
     * @Return List<Ineger>
     * @Author UGcris
     * @Aate 2018/9/7
     */
    public List<Integer> search(String str , String matchedStr){
        int[] next=getNext(matchedStr);
        int j=0;
        List<Integer> list=new ArrayList<Integer>();
        for(int i=0;i<str.length();i++){
            while(j>0 && str.charAt(i) != matchedStr.charAt(j) )
                j=next[j];
            if(str.charAt(i) == matchedStr.charAt(j))
                j++;
            if(j==matchedStr.length()){
                list.add(i-j+1);
                j=next[j];
            }
        }
        return  list;
    }
}
