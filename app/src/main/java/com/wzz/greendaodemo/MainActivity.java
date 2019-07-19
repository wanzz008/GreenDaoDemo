package com.wzz.greendaodemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.wzz.greendaodemo.bean.User;
import com.wzz.greendaodemo.db.BaseDao;
import com.wzz.greendaodemo.db.BaseDaoFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void create(View view) {

        BaseDao<User> userDao =
                BaseDaoFactory.getInstance().getBaseDao(User.class);

        userDao.insert( new User(10 , "hahaha" , "123456") );

        userDao.update( new User(11 , "xixixi", "654321") , new User(10 , "dd" , "ee"));


    }
}
