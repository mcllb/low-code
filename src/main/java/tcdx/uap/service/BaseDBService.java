package tcdx.uap.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tcdx.uap.common.utils.*;
import tcdx.uap.mapper.BaseDBMapper;
import tcdx.uap.service.entities.*;
import tcdx.uap.service.store.Modules;

import java.util.*;

/**
 * 参数配置 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class BaseDBService
{

    @Autowired
    BaseDBMapper baseDBMapper;

    public List<Map> selectMaxCol(String tn,String  col){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectMaxByCauses(MapUtils.G(
                "tn", tn, "col", col,
                "obj_c1", null));
        return l;
    }

    public Integer selectMaxColEq(String tn,String col, String eqCol, Object eqVal){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectMaxByCauses(MapUtils.G(
                "tn", tn, "col", col,
                "equalMap", SqlUtil.eq(eqCol, eqVal)));
        if(l!=null && l.size()>0 && l.get(0)!=null) {
            System.out.println(l.get(0));
            return (Integer) l.get(0).get(col);
        }
        else
            return null;
    }

    public Integer selectMaxColEq(String tn,String col, Map eqMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectMaxColEq(MapUtils.G(
                "tn", tn, "col", col,
                "equalMap", eqMap));
        if(l!=null && l.size()>0 && l.get(0)!=null) {
            System.out.println(l.get(0));
            return (Integer) l.get(0).get(col);
        }
        else
            return null;
    }

    public Map selectOne(String tn,Map eqMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectEq(MapUtils.G(
                "tn", tn,
                "equalMap", eqMap));
        if(l!=null && l.size()>0 && l.get(0)!=null) {
            return l.get(0);
        }
        else return null;
    }

    public List<Map> selectEq(String tn,Map eqMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectEq(MapUtils.G(
                "tn", tn,
                "equalMap", eqMap));
        return l;
    }

    public List<Map> selectEq(String tn,Map eqMap, Map sortMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectEq(MapUtils.G(
                "tn", tn,
                "equalMap", eqMap,
                "sortMap", sortMap
        ));
        return l;
    }

    public List<Map> selectEq(String tn, List<String>selectCols, Map eqMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectEq(MapUtils.G(
                "tn", tn,
                "equalMap", eqMap,
                "selectCols", selectCols));
        return l;
    }


    public List<Map> selectEq(String tn, List<String>selectCols,Map eqMap, Map sortMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectEq(MapUtils.G(
                "tn", tn,
                "equalMap", eqMap,
                "selectCols", selectCols,
                "sortMap", sortMap));
        return l;
    }


    public List<Map> selectIn(String tn, String columnName,List vals,Map sortMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectIn(MapUtils.G(
                "tn", tn,
                "columnName", columnName,
                "vals", vals,
                "sortMap", sortMap));
        return l;
    }


    public List<Map> selectIn(String tn, String columnName,List vals){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectIn(MapUtils.G(
                "tn", tn,
                "columnName", columnName,
                "vals", vals));
        return l;
    }

    public List<Map> selectIn(String tn, List<String> selectCols, String columnName,List vals){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectIn(MapUtils.G(
                "tn", tn,
                "selectCols", selectCols,
                "columnName", columnName,
                "vals", vals));
        return l;
    }

    public List<Map> selectEqAndIn(String tn, List<String> selectCols, String columnName,List vals,Map equalMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectEqAndIn(MapUtils.G(
                "tn", tn,
                "selectCols", selectCols,
                "columnName", columnName,
                "vals", vals,
                "equalMap",equalMap));
        return l;
    }

    public List<Map> selectIn(String tn, boolean distinct, List<String> selectCols, String columnName,List vals){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectIn(MapUtils.G(
                "tn", tn,
                "selectCols", selectCols,
                "columnName", columnName,
                "vals", vals));
        return l;
    }

    /**
     * 修改map内部数据
     * */
    public static void handlerObjectTypedField(Map map){
        Map re = new HashMap();
        map.forEach((key,value)->{
            if(value instanceof Map){
                Map obj =((Map)value);
                if(Objects.equals(obj.get("type"), "date") && obj.get("value")!=null){
                    Date d = new Date((Long)obj.get("value"));
                    System.out.println(d);
                    re.put(key, d);
                } else {
                    re.put(key, obj.get("id_"));
                }
            }
            else if (!(value instanceof Date) && (Objects.equals(key, "posted_time_") || Objects.equals(key, "create_time_")) ) {
                Date d = new Date((Long)value);
                re.put(key, d);
            }
//            else if(value instanceof List){
//                re.put(key, ((List)value).get(0));
//            }
            else{
                re.put(key, value);
            }
        });
        re.forEach((key,value)->{
            map.put(key, value);
        });
    }

    public int insertMap(String tn, Map insertMap){
        handlerObjectTypedField(insertMap);
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        //用户赋值默认密码
//        insertMap.put("psw", SecurityUtils.encryptPassword("123456"));
        int rs = baseDBMapper.insertMap(MapUtils.G(
                "tn", tn,
                "insertMap", insertMap));
        return rs;
    }

    /**
     * 插入数据并自动填充最大ord返回ID
     * @param tn 数据表名
     * @param insertMap 添加的数据
     * @param ordCol 排序列
     * @param ordEqMap 排序列
     * */
    public Map insertMapAutoFillMaxOrd(String tn, Map insertMap, String ordCol,Map ordEqMap){
        handlerObjectTypedField(insertMap);
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        Integer max = selectMaxColEq(tn, ordCol, ordEqMap);
        if(max==null)
            max = 1;
        else
            max = max+1;
        insertMap.put(ordCol, max);
        Map rs = insertMapRetRow( tn,insertMap);
        return rs;
    }

    public Map insertMapWithSpecifiedOrd(String tn, Map insertMap, String ordCol, Integer ordValue) {
        handlerObjectTypedField(insertMap);
        insertMap.put(ordCol, ordValue);
        Map rs = insertMapRetRow(tn, insertMap);
        return rs;
    }

    /**
     * 插入数据并返回ID
     * @param tn 数据表名
     * @param insertMap 添加的数据
     * @param equalMap 匹配的数据
     * */
    @Transactional
    public Map insertWhenNotExist(String tn, Map insertMap, Map equalMap){
        SqlUtil.filterKeyword(tn);
        handlerObjectTypedField(insertMap);
        //如果批量插入，则取insertMaps的数据
        if(equalMap==null)
            return null;
        List<Map> exists = selectEq(tn, equalMap);
        if(exists.size()>0){
            return exists.get(0);
        }
        else{
            List<Map> re = null;
            //如果有insertMap，插入insertMap数据
            if(insertMap!=null) {
                 re = baseDBMapper.insertMapRetRow(Lutils.genMap("tn",tn, "insertMap",insertMap));
            }

            return re.get(0);
        }
    }

    public Map insertWhenNotExistUpdateWhenExists(String tn, Map insertMap, Map equalMap){
        SqlUtil.filterKeyword(tn);
        handlerObjectTypedField(insertMap);
        //如果批量插入，则取insertMaps的数据
        if(equalMap==null)
            return null;
        List<Map> exists = selectEq(tn, equalMap);
        if(exists.size()>0){
            if(exists.get(0).get("id")!=null) {
                updateEq(tn, insertMap, Lutils.genMap("id", exists.get(0).get("id")));
            }
            exists = selectEq(tn, equalMap);
            return exists.get(0);
        }
        else{
            List<Map> re = null;
            //如果有insertMap，插入insertMap数据
            if(insertMap!=null) {
                re = baseDBMapper.insertMapRetRow(Lutils.genMap("tn",tn, "insertMap",insertMap));
            }
            return re.get(0);
        }
    }


    public Map insertWhenNotExistUpdateWhenExistsAutoFillOrd(String tn, Map insertMap, Map equalMap,String ordCol,Map ordEqMap){
        SqlUtil.filterKeyword(tn);
        handlerObjectTypedField(insertMap);
        //如果批量插入，则取insertMaps的数据
        if(equalMap==null)
            return null;
        List<Map> exists = selectEq(tn, equalMap);
        if(exists.size()>0){
            if(exists.get(0).get("id")!=null) {
                updateEq(tn, insertMap, Lutils.genMap("id", exists.get(0).get("id")));
            }
            exists = selectEq(tn, equalMap);
            return exists.get(0);
        }
        else{
            //如果有insertMap，插入insertMap数据
            if(insertMap!=null) {
                //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
                Integer max = selectMaxColEq(tn, ordCol, ordEqMap);
                if(max==null)
                    max = 1;
                else
                    max = max+1;
                insertMap.put(ordCol, max);
                return insertMapRetRow( tn,insertMap);
            }

            return null;
        }
    }

    /**
     * 插入数据并返回ID
     * @param tn 添加的数据
     * @param insertMap 添加数据库
     * */
    public Integer insertMapRetVal(String tn, Map insertMap, String valCol){
        handlerObjectTypedField(insertMap);
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List ls = baseDBMapper.insertMapRetRow(MapUtils.G(
                "tn", tn,
                "insertMap", insertMap));
        return (Integer)((Map)ls.get(0)).get(valCol);
    }

    /**
     * 插入数据并返回ID
     * @param tn 添加的数据
     * @param insertMap 添加数据库
     * */
    public Map insertMapRetRow(String tn, Map insertMap){
        handlerObjectTypedField(insertMap);
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> ls = baseDBMapper.insertMapRetRow(MapUtils.G(
                "tn", tn,
                "insertMap", insertMap));
        return ls.get(0);
    }

    public int insertMapList(String tn, List<Map> insertMapList){
        int rs = 0;
        for(Map insertMap:insertMapList){
            handlerObjectTypedField(insertMap);
            //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
             rs += baseDBMapper.insertMap(MapUtils.G(
                    "tn", tn,
                    "insertMap", insertMap));
        }
        return rs;
    }

    public List<Integer> insertMapListRetIds(String tn, List<Map> insertMapList){
        List<Integer> ids = new ArrayList<>();
        for(Map insertMap:insertMapList){
            handlerObjectTypedField(insertMap);
            //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
             Map<String,Object> row= insertMapRetRow(tn, insertMap);
             ids.add((Integer) row.get("id"));
        }
        return ids;
    }


    public List<Map> insertMapListRetRows(String tn, List<Map> insertMapList){
        List<Map> rows = new ArrayList<>();
        for(Map insertMap:insertMapList){
            handlerObjectTypedField(insertMap);
            //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
            Map<String,Object> row= insertMapRetRow(tn, insertMap);
            rows.add(row);
        }
        return rows;
    }


    public int updateEq(String tn, Map updateMap, Map equalMap){
        handlerObjectTypedField(updateMap);
        handlerObjectTypedField(equalMap);
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        int rs = baseDBMapper.updateEq(MapUtils.G(
                "tn", tn,
                "updateMap", updateMap,
                "equalMap", equalMap));
        return rs;
    }


    public int updateIn(String tn, Map updateMap, String columnName, List vals){
        handlerObjectTypedField(updateMap);
        updateMap.remove(tn + "_id");
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        int rs = baseDBMapper.updateIn(MapUtils.G(
                "tn", tn,
                "updateMap", updateMap,
                "columnName", columnName,
                "vals", vals));
        return rs;
    }



    public int deleteEq(String tn, Map equalMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        int rs = baseDBMapper.deleteEq(MapUtils.G(
                "tn", tn,
                "equalMap", equalMap));
        return rs;
    }

    public int deleteIn(String tn, String col, List ids){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        int rs = baseDBMapper.deleteIn(MapUtils.G(
                "tn", tn,
                "col", col,
                "ids", ids));
        return rs;
    }

    public List<Map> selectByCauses(String tn, Map causesObj, Map sortMap){

        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectByCauses(MapUtils.G(
                "tn", tn,
                "obj_c1", causesObj,
                "sortMap", sortMap
        ));
        return l;
    }

    public List<Map> selectAndOrByCauses(String tn, Map causesObj1, Map causesObj2, Map sortMap){

        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectAndOrByCauses(MapUtils.G(
                "tn", tn,
                "obj_c1", causesObj1,
                "obj_c2", causesObj2,
                "sortMap", sortMap
        ));
        return l;
    }

    public List<Map> selectByCauses(String tn, List selColumns, Map causes, Map sortMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectByCauses(MapUtils.G(
                "tn", tn,
                "selColumns", selColumns,
                "obj_c1", causes,
                "sortMap", sortMap
        ));
        return l;
    }

    public List<Map> selectByCauses(String tn,boolean distinct, List<String> selColumns,  Map causes, Map sortMap){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        List<Map> l = baseDBMapper.selectByCauses(MapUtils.G(
                "tn", tn,
                "distinct", distinct?"true": null,
                "selColumns", selColumns,
                "obj_c1", causes,
                "sortMap", sortMap
        ));
        return l;
    }

    public int executeSql(String sql){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        int rs= baseDBMapper.executeSql(sql);
        return rs;
    }

    public int addTableCol(String table_name,String col_name,String col_type){
      return baseDBMapper.tableAddColumn(MapUtils.G("tn", table_name, "columnName", col_name, "columnType", col_type));
    }


    public int executeSql(String sql,Map map){
        map.put("sql",sql);
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        int rs= baseDBMapper.executeSql(map);
        return rs;
    }

    public List<Map> querySql(String sql,Map map){
        map.put("sql",sql);
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        return  baseDBMapper.selectSql(map);
    }


    public List<Map> selectSql(String sql){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        return baseDBMapper.selectSql(Lutils.genMap("sql" ,sql));
    }

    public List<Map> selectSql(Map params){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        return baseDBMapper.selectSql(params);
    }

    public List<Map> selectSql(Integer defined_sql_id,Map map){
        List<Map> sqls = selectEq("v_defined_sql", Lutils.genMap("id", defined_sql_id));
        map.remove("defined_sql_id");
        map.put("sql", sqls.get(0).get("sql").toString());
        return selectSql(map);
    }

    public int insertByBatch(String tableName, List<Map> insertMapData){
        int count = 0;
        for(Map insertData:insertMapData){
            count += baseDBMapper.insertMap(MapUtils.G(
                    "tn", tableName,
                    "insertMap", insertData));
        }
        return count;
    }


    public List<Map> selectNewById(String table_name){
        //获取跟流程相关联的字段，选取关键字段存入流程，以免过多数据库插入，影响效率
        return baseDBMapper.selectNewById(Lutils.genMap("tn", table_name));
    }


    public List<Map> selectTreeList(Integer id,String tn) {
        return baseDBMapper.selectTreeList(Lutils.genMap("id",id,"tn",tn));
    }

    public List<Map> selectDefinedSql(Map params) {
        if(params.get("obj_c1")==null){
            params.put("obj_c1", new HashMap<>());
        }
        return baseDBMapper.selectDefinedSql(params);
    }

    public List<Map> selectDefinedSqlCounts(Map params) {
        if(params.get("obj_c1")==null){
            params.put("obj_c1", new HashMap<>());
        }
        return baseDBMapper.selectDefinedSqlCounts(params);
    }

}
