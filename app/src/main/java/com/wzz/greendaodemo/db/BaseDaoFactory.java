package com.wzz.greendaodemo.db;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.File;

public class BaseDaoFactory {

    private SQLiteDatabase sqLiteDatabase ;

    private static BaseDaoFactory instance = new BaseDaoFactory();

    public static BaseDaoFactory getInstance(){
        return instance ;
    }
    //定义建数据库的路径
    private String databasePath;
    public BaseDaoFactory(){
        // 新建数据库
        databasePath = Environment.getExternalStorageDirectory() + File.separator + "my.db";
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(databasePath ,null );
    }

    public <T> BaseDao<T> getBaseDao(Class<T> tClass){
        BaseDao baseDao = null ;
        try {
            baseDao = BaseDao.class.newInstance();
            baseDao.init( sqLiteDatabase , tClass );
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return baseDao ;
    }


}
