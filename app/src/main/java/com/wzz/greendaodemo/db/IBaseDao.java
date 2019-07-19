package com.wzz.greendaodemo.db;

import java.util.List;

public interface IBaseDao<T> {

    //插入
    long insert(T bean);
    int delete(T bean);
    long update(T entity , T where);
    List<T> query(T bean);


    List<T> query(T where, String orderBy, Integer startIndex, Integer limit);

}
