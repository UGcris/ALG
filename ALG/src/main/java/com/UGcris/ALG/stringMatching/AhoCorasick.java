package com.UGcris.ALG.stringMatching;


import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * @Description AC自动机——用于多模匹配，需要了解KMP原理和Trie树
 * @Author UGcris
 * @Date 2018/9/7
 */
public class AhoCorasick {

    private final int lettersCount=26;

    /**
     * @Description 构造将要匹配的字串的trie树
     * @Params List<String> strList,Trie root
     * @Return
     * @Author UGcris
     * @Aate 2018/9/7
     */
    private void buildTrie(List<String> strList,Trie root){
        if(StringUtils.isEmpty(root)) return;
        for(String str:strList){
            Trie node=root;
            //对应子节点存在则不为空
            for(int i=0;i<str.length();i++){
                char ch=str.charAt(i);
                Trie[] next=node.getNext();
                int j=ch-'a';
                if(StringUtils.isEmpty(next[j])){
                    next[j]=new Trie();
                    next[j].setCh(ch);
                    node.setNext(next);
                }
                //下一节点
                node=next[j];
            }
            node.setCount(node.getCount()+1);
            node.setWord(str);
        }
    }


    /**
     * @Description
     * @Params Trie root
     * @Return
     * @Author UGcris
     * @Aate 2018/9/7
     */
    private void buildFailPoint(Trie root){
        int  h=0,t=1;
        List<Trie> list=new ArrayList<Trie>();
        list.add(root);

        while(h<t){
            Trie now=list.get(h++);
            for(int i=0;i<lettersCount;i++){
                Trie next=now.getNext()[i];
                if(!StringUtils.isEmpty(next)){

                    if(now==root){
                        next.setFail(root);
                    }else{
                        Trie  p=now.getFail();
                        while(!StringUtils.isEmpty(p)){
                            if(!StringUtils.isEmpty(p.getNext()[i])){
                                next.setFail(p.getNext()[i]);
                                break;
                            }
                            p=p.getFail();
                        }
                        if(StringUtils.isEmpty(p)){
                            next.setFail(root);
                        }
                    }
                    list.add(t++,next);
                }
            }
        }
    }


    public String[][] acSearch(List<String> strList,String str){
        String[][] result=new String[str.length()][str.length()];
        Trie root=new Trie();
        buildTrie( strList,root);
        buildFailPoint(root);
        Trie node=root;
        for(int i=0;i<str.length();i++){
            while(node!=root && StringUtils.isEmpty(node.getNext()[str.charAt(i)-'a'])){
                node=node.getFail();
            }
            if(!StringUtils.isEmpty(node.getNext()[str.charAt(i)-'a'])){
                node=node.getNext()[str.charAt(i)-'a'];
            }
            if(node.getCount()>0){
                String word=node.getWord();
                result[i-word.length()+1][i+1]=word;
                node=node.getFail();
            }
        }
        return result;
    }
    /**
     * trie树
     */
    class Trie{
        private final int lettersCount=26;
       /**
        * 匹配完全一个单词加1
        */
       int count;
       Trie fail;
       String word;
       Trie[] next;
       char ch;

       Trie(){
            count=0;
            fail=null;
            word="";
           next=new Trie[lettersCount];
       }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public Trie getFail() {
            return fail;
        }

        public void setFail(Trie fail) {
            this.fail = fail;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public Trie[] getNext() {
            return next;
        }

        public void setNext(Trie[] next) {
            this.next = next;
        }
        public char getCh() {
            return ch;
        }

        public void setCh(char ch) {
            this.ch = ch;
        }
    }
}
