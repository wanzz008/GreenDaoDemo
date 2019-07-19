package com.wzz.greendaodemo.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.wzz.greendaodemo.annotation.DbField;
import com.wzz.greendaodemo.annotation.DbTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseDao<T> implements IBaseDao<T> {

    private static final String TAG = "wzz---------";
    private SQLiteDatabase sqLiteDatabase ;

    private String tableName;
    private Class<T> entityClass ;

    private boolean initFlag = false ;
    public void init(SQLiteDatabase sqLiteDatabase, Class<T> entityClass) {

        // 防止多次创建重复的表 只让创建一次
        if ( initFlag ){
            return;
        }
        initFlag = true ;

        this.sqLiteDatabase = sqLiteDatabase;
        this.entityClass = entityClass;

        if ( entityClass.getAnnotation(DbTable.class) == null ){
            // 通过反射拿到类名
            this.tableName = entityClass.getSimpleName() ;
        }else {
            // 通过注解获取表名
            this.tableName = entityClass.getAnnotation(DbTable.class).value() ;
        }
        // 执行建表操作
        String createTableSql = getCreateTableSql();
        Log.i(TAG, "BaseDao: 语句：" + createTableSql );
        this.sqLiteDatabase.execSQL( createTableSql );

        initCacheMap();

    }

    public HashMap<String , Field > cacheMap = new HashMap<>(); // {"_id":Field, "name":Field , "sex"：Field }

    /**
     * 建表的sql语句
     * create table if not exists
     * @return
     */
    private String getCreateTableSql() {
        StringBuffer sb = new StringBuffer( );
        sb.append("create table if not exists ");
        sb.append( tableName + "(" );
        // 反射获取类上所有的属性 为了创建表中的字段
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            Class<?> type = field.getType(); // 字段类型 如String integer
            // 如果字段加了注解，通过注解获取
            if ( field.getAnnotation(DbField.class) != null ){
                if ( type == String.class ){
                    sb.append( field.getAnnotation(DbField.class).value() + " Text,");
                }else if ( type == Integer.class  ){
                    sb.append( field.getAnnotation(DbField.class).value() + " INTEGER,");
                }else if ( type == Long.class  ){
                    sb.append( field.getAnnotation(DbField.class).value() + " BIGINT,");
                }

                else  if(type == Double.class){
                    sb.append( field.getAnnotation(DbField.class).value() + " DOUBLE,");
                }else  if(type == byte[].class){
                    sb.append(field.getAnnotation(DbField.class).value() + " BLOB,");
                } else {
                    continue; // 如果是object类型的 表明是不支持的类型
                }

            }else {
                // 通过反射获取
                if ( type == String.class ){
                    sb.append( field.getName() + " Text,");
                }else if ( type == Integer.class  ){
                    sb.append( field.getName()  + " INTEGER,");
                }else if ( type == Long.class  ){
                    sb.append( field.getName()  + " BIGINT,");
                } else  if(type == Double.class){
                    sb.append( field.getName() + " DOUBLE,");
                }else  if(type == byte[].class){
                    sb.append( field.getName() + " BLOB,");
                } else {
                    continue; // 如果是object类型的 表明是不支持的类型
                }
            }

        }
        // 如果最后是","的话 先删除
        if ( sb.charAt( sb.length() - 1 ) == ',' ){
            sb.deleteCharAt( sb.length() - 1 );
        }
        sb.append( ")" );

        return sb.toString() ;
    }

    @Override
    public long insert(T bean) {
//        ContentValues contentValues = new ContentValues() ;
//        contentValues.put("_id" , 1 );
//        contentValues.put("_id" , 1 );

        Map<String ,String > map = getValue( bean );
        // 把map的值转移到contentValues
        ContentValues contentValues = getContentValues(map);
        sqLiteDatabase.insert( tableName , null , contentValues );
        return 0;
    }


    private ContentValues getContentValues(Map<String, String> map) {
        ContentValues contentValues = new ContentValues() ;
        Set<String> keySet = map.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            String value = map.get(key);
            if ( value != null ){
                contentValues.put( key , map.get( key ));
            }
        }

        return contentValues;
    }


    /**
     * 初始化所需要bean对象的属性值 初始化map
     * cacheMap格式为： // {"_id":Field, "name":Field , "sex"：Field }
     */
    private void initCacheMap() {
        // 1、取得所有的字段名
        String sql = "select * from " + tableName + " limit 1,0" ; // 空表
        Cursor cursor = sqLiteDatabase.rawQuery(sql, null);
        String[] columnNames = cursor.getColumnNames();
        // 2.取得所有的成员变量
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible( true );
        }
        //3。字段跟成员变量一一对应
        // 判断数据库表中的列名和bean对象中的属性名一致 然后添加到map中
        for (String columnName : columnNames) {
            Field columnFiled = null ;
            for (Field field : fields) {
                String fileName ;
                if ( field.getAnnotation( DbField.class) != null ){
                    fileName = field.getAnnotation( DbField.class).value();
                }else {
                    fileName = field.getName() ;
                }

                if(columnName.equals( fileName )){
                    columnFiled = field;
                    break;
                }
            }
            if ( columnFiled != null ){
                cacheMap.put( columnName , columnFiled ) ;
            }
        }
    }



    //key(字段)--- value(成员变量)
    /**
     * 根据传入的bean对象，解析bean对象的值，并转换成map 如：// {"_id":1, "name":"wzz" , "sex"：1}
     * @param bean
     * @return
     */
    private Map<String, String> getValue(T bean) {
        // key是字段名（如果有注解就是注解上的标注的值）  value是成员变量值
        Map<String, String> map = new HashMap<>();
        Iterator<Field> iterator = cacheMap.values().iterator();
        while ( iterator.hasNext() ){

            Field field = iterator.next();
            field.setAccessible(true);
            try {
                Object obj = field.get(bean); // 获取到Field对象的属性值
                if ( obj == null ){
                    continue;
                }
                String value = obj.toString();
                String key = "" ;
                if (field.getAnnotation( DbField.class ) !=null ){
                    key = field.getAnnotation( DbField.class ).value() ;
                }else {
                    key = field.getName() ;
                }

                if(!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)){
                    map.put(key, value);
                }


            } catch ( Exception e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * 删除
     * @param bean
     * @return
     */
    @Override
    public int delete(T bean) {

        //        sqLiteDatabase.delete(tableName,"id = ?", new String[]{"1"} );

        int result = -1;
        Map<String, String> map = getValue(bean);
        Condition condition = new Condition(map);
        result = sqLiteDatabase.delete( tableName , condition.whereClause , condition.whereArgs );

        return result;
    }

    /**
     * 更新数据
     * @param entity 需要更新成的数据
     * @param where 更新的条件数据
     * @return
     */
    @Override
    public long update(T entity, T where) {

        /**
         * whereClause 可选的where语句
         * whereArgs whereClause语句中表达式的？占位参数列表
         */
        // sqLiteDatabase.update(tableName, contentValue, "id = ?", new String[]{"1"});

        int result  = -1 ;
        Map<String, String> value = getValue(entity);
        ContentValues contentValues = getContentValues(value);

        Map<String, String> value1 = getValue(where);
        Condition condition = new Condition(value1);

        result = sqLiteDatabase.update(tableName, contentValues, condition.whereClause, condition.whereArgs);

        return result ;
    }

    /**
     * 把要查询的表达式 封装成一个类 便于使用
     */
    private class Condition {

        private String whereClause;  // 1=1 and id = ? and name = ? and password = ?
        private String[] whereArgs; // { 1 ,"wzz","123456"}

        /**
         * new Person(1, "alan", "123")
         * @param whereCasue
         */
        public Condition(Map<String, String> whereCasue ) {
            ArrayList list = new ArrayList();  //whereArgs里面的内容存入的list
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("1=1");
            //取得所有成员变量的名字
            Set<String> keys = whereCasue.keySet();
            Iterator<String> iterator = keys.iterator();
            while (iterator.hasNext()){
                String key = iterator.next();
                String value = whereCasue.get(key);
                if(value != null){
                    stringBuffer.append(" and " + key + "=?");  // 1=1 and id = ? and name = ? and password = ?
                    list.add(value);
                }
            }

            this.whereClause = stringBuffer.toString();
            this.whereArgs = (String[]) list.toArray(new String[list.size()]);
        }
    }


    @Override
    public List<T> query(T bean) {
        return query( bean , null, null, null);
    }

    @Override
    public List<T> query(T where, String orderBy, Integer startIndex, Integer limit) {
        //        sqLiteDatabase.query(tableName, null, "id = ?",
//                new String[], null. null, orderBy, "1, 5");
        //1、准本好ContentValues中需要的数据
        Map<String, String> values = getValue(where);

        String limitString = "";   //"2,6"
        if(startIndex != null && limit != null){
            limitString = startIndex + " , " + limit;
        }

       Condition condition = new Condition(values);

        Cursor query = sqLiteDatabase.query(tableName, null, condition.whereClause,
                condition.whereArgs, null, orderBy, limitString);

        List<T> result = getResult(query, where);  //游标  --- 》 javabean  --- list<javabaen>
        return result;

    }

    private List<T> getResult(Cursor query, T where) {
        ArrayList list = new ArrayList();
        Object item = null;
        while (query.moveToNext()){
            try {
                item = where.getClass().newInstance();  //因为不知道 new  ? , 所以通过反射方式
                //cacheMap  (字段---成员变量的名字)
                Iterator<Map.Entry<String, Field>> iterator = cacheMap.entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<String, Field> entry = iterator.next();
                    //取列名
                    String columnName = entry.getKey();

                    //以列名拿到列名在游标中的位置
                    int columnIndex = query.getColumnIndex(columnName);

                    Field value = entry.getValue();   //id
                    Class<?> type = value.getType();  //Integer
                    if(columnIndex != -1){  //columnName = "age"
                        if(type==String.class){
                            value.set(item,query.getString(columnIndex));//setid(1)
                        }else if(type==Double.class){
                            value.set(item,query.getDouble(columnIndex));
                        }else if(type==Integer.class){
                            value.set(item,query.getInt(columnIndex));
                        }else if(type==Long.class){
                            value.set(item,query.getLong(columnIndex));
                        }else if(type==byte[].class){
                            value.set(item,query.getBlob(columnIndex));
                        }else{
                            continue;
                        }
                    }
                }
                list.add(item);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        query.close();
        return list;
    }

}
