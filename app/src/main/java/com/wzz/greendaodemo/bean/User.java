package com.wzz.greendaodemo.bean;

import com.wzz.greendaodemo.annotation.DbField;
import com.wzz.greendaodemo.annotation.DbTable;

@DbTable("table_user")
public class User {


    @DbField("wId")
    public Integer id ;
    private String name ;

    private String pwd ;

//    public Long count ;


    public User(Integer id, String name, String pwd) {
        this.id = id;
        this.name = name;
        this.pwd = pwd;
    }
}
