package tcdx.uap.service;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.mapper.CommonDBMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommonDBImpl implements CommonDBService {
    @Autowired
    private CommonDBMapper commonDBMapper;

    //---------------------------------------------------------------------
    @Override
    public List<Map> selectById(String tableName, Object id) {
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName))
            return null;
        return commonDBMapper.selectById(tableName, id);
    }

    @Override
    public List<Map> selectEq(String tableName, Map equalMap, List<String> selectedColumns) {
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName))
            return null;
        if (!Lutils.safe_sql_map(equalMap))
            return null;
        System.out.println(equalMap);
        return commonDBMapper.selectEq(tableName, equalMap, selectedColumns);
    }

    @Override
    public List<Map> selectDistinctEq(String tableName, Map equalMap, List<String> selectedColumns) {
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName))
            return null;
        if (!Lutils.safe_sql_map(equalMap))
            return null;
        System.out.println(equalMap);
        return commonDBMapper.selectDistinctEq(tableName, equalMap, selectedColumns);
    }

    @Override
    public List<Map> selectEqSortLimit(String tableName, Map equalMap, List<String> selectedColumns, Map sortMap, Integer offset, Integer limit) {
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName) || !Lutils.safe_sql_map(equalMap) || !Lutils.safe_sql_map(sortMap))
            return null;
        System.out.println(equalMap);
        return commonDBMapper.selectEqSortLimit(tableName, equalMap, selectedColumns, sortMap, null, null);
    }

    @Override
    public List<Map> selectIn(String tableName, String columnName, List valueList) {
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName) || !Lutils.safe_sql(columnName))
            return null;
        return commonDBMapper.selectIn(tableName, columnName, valueList);
    }

    @Override
    public List<Map> selectAndCauses(String tableName, List<Map> andCauses, Map sortMap, Object limit, Object offset) {
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName))
            return null;
        for (Map m : andCauses) {
            if (!Lutils.safe_sql_map(m))
                return null;
        }
        return commonDBMapper.selectAndCauses(tableName, andCauses, sortMap, limit, offset);
    }

    @Override
    public Integer selectAndCausesCount(String tableName, List<Map> andCauses) {
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName))
            return null;
        for (Map m : andCauses) {
            if (!Lutils.safe_sql_map(m))
                return null;
        }
        List cL = commonDBMapper.selectAndCausesCount(tableName, andCauses);
        Map cM = (Map) cL.get(0);
        Integer cc = Integer.parseInt(cM.get("c").toString());
        return cc;
    }
    @Override
    public List<Map> selectAndCausesCountGroupBy(String tableName, List<Map> andCauses, List groupByColumnLists){
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName) || !Lutils.safe_sql_list(groupByColumnLists))
            return null;
        for (Map m : andCauses) {
            if (!Lutils.safe_sql_map(m))
                return null;
        }
        List cL = commonDBMapper.selectAndCausesCountGroupBy(tableName, andCauses,groupByColumnLists);
        return cL;
    }
    @Override
    public Map updateById(String tableName, Object id, Map updateData) {//检测是否有sql注入
        if(!Lutils.safe_sql(tableName)) {
            return new HashMap<String, Object>(){{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        int handlerResult =  commonDBMapper.updateById(tableName, id, updateData);
        return new HashMap<String, Object>(){{
            put("state", "success");
            put("result", handlerResult);
        }};
    }
    @Override
    public Map updateIn(String tableName, String columnName, List valueList, Map updateData) {//检测是否有sql注入
        if(!Lutils.safe_sql(tableName)) {
            return new HashMap<String, Object>(){{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        if(!Lutils.safe_sql_map(updateData)) {
            return new HashMap<String, Object>() {{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        int handlerResult =  commonDBMapper.updateIn(tableName, columnName,valueList,updateData);
        return new HashMap<String, Object>(){{
            put("state", "success");
            put("result", handlerResult);
        }};
    }
    @Override
    public Map updateEq(String tableName, Map equalMap, Map updateData){
        if(!Lutils.safe_sql(tableName) || !Lutils.safe_sql_map(equalMap) || !Lutils.safe_sql_map(updateData)) {
            return new HashMap<String, Object>(){{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        System.out.println(equalMap);
        int handlerResult =  commonDBMapper.updateEq(tableName, equalMap,updateData);
        return new HashMap<String, Object>(){{
            put("state", "success");
            put("result", handlerResult);
        }};
    }
    @Override
    public Map deleteById(String tableName, Object id){
        if(!Lutils.safe_sql(tableName)) {
            return new HashMap<String, Object>(){{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        int handlerResult =  commonDBMapper.deleteById(tableName, id);
        return new HashMap<String, Object>(){{
            put("state", "success");
            put("result", handlerResult);
        }};
    }
    @Override
    public Map deleteEq(String tableName, Map equalMap){
        if(!Lutils.safe_sql(tableName)) {
            return new HashMap<String, Object>(){{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        int handlerResult =  commonDBMapper.deleteEq(tableName, equalMap);
        return new HashMap<String, Object>(){{
            put("state", "success");
            put("result", handlerResult);
        }};
    }
    @Override
    public Map insertMap(String tableName, Map insertMap){
        if(!Lutils.safe_sql(tableName)) {
            return new HashMap<String, Object>(){{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        int handlerResult =  commonDBMapper.insertMap(tableName, insertMap);
        return new HashMap<String, Object>(){{
            put("state", "success");
            put("result", handlerResult);
        }};
    }
    @Override
    public List selectTreeList(String tableName, String parentIdColumnName,String nodeIdColumnName, Object startNodeIdValue,List<Map> andCause) {
        //检测是否有sql注入
        if( !Lutils.safe_sql(tableName) || !Lutils.safe_sql(parentIdColumnName) || !Lutils.safe_sql(nodeIdColumnName))
            return null;
        if(andCause==null)
            andCause = new ArrayList<Map>();
        List<Map> l1 = commonDBMapper.selectEq(tableName, new <String,Object>HashMap() {{
            put(parentIdColumnName, startNodeIdValue);
        }},null);
        int count = 0;
        List resultList = new ArrayList();
        while(l1.size()>0 && ++count<10){
            resultList.addAll(l1);
            List valueList = new ArrayList<Object>();
            for(Map node:l1){
                valueList.add(node.get(nodeIdColumnName));
            }
            andCause.remove(andCause.size()-1);
            andCause.add(Lutils.genMap("tp","in","cn",parentIdColumnName,"vals",valueList));
            l1 = commonDBMapper.selectAndCauses(tableName, andCause,Lutils.genMap("order_id", "asc"),null,null);
        }
        return resultList;
    }

    @Override
    public Map selectTreeMap(String tableName, String parentIdColumnName,String nodeIdColumnName,
                             Object startNodeIdValue, String childrenKeyName,Map fieldReplace,List<Map> andCause) {
        //检测是否有sql注入
        if(!Lutils.safe_sql(tableName)||!Lutils.safe_sql(parentIdColumnName)||!Lutils.safe_sql(nodeIdColumnName))
            return null;
        if(andCause==null)
            andCause = new ArrayList<Map>();
        List<Map> l1 = commonDBMapper.selectAndCauses(tableName, new ArrayList<Map>(){{
            add(new <String,Object>HashMap() {{
                put("tp", "eq");
                put("cn", nodeIdColumnName);
                put("val", startNodeIdValue);
            }});
        }},new <String,Object>HashMap() {{
            put("id", "asc");
        }},null,null);
        int count = 0;
        Map tree = l1.get(0);
        while(l1.size()>0 && ++count<10){
            //首次初始化
            List valueList = new ArrayList<Object>();
            for(Map node:l1){
                valueList.add(node.get(nodeIdColumnName));
                if(fieldReplace!=null && !fieldReplace.isEmpty()){
                    fieldReplace.forEach((key,value)->{
                        node.put(value, node.get(key));
                    });
                }
                node.put("spread", true);
                Lutils.insertToTree(tree,node,parentIdColumnName,nodeIdColumnName,childrenKeyName);
            }
            andCause.remove(andCause.size()-1);
            andCause.add(Lutils.genMap("tp","in","cn",parentIdColumnName,"vals",valueList));
            l1 = commonDBMapper.selectAndCauses(tableName, andCause,Lutils.genMap("order_id", "asc"),null,null);
        }
        return tree;
    }

    @Override
    public Map selectLinkWorkerTreeMap(String tableName, String parentIdColumnName,String nodeIdColumnName,
                             Object startNodeIdValue, String childrenKeyName,Map fieldReplace,List<Map> andCause) {
        //检测是否有sql注入
        if(!Lutils.safe_sql(tableName)||!Lutils.safe_sql(parentIdColumnName)||!Lutils.safe_sql(nodeIdColumnName))
            return null;
        if(andCause==null){
            andCause = new ArrayList<Map>();
        }
        List<Map> l1 = commonDBMapper.selectAndCauses(tableName, new ArrayList<Map>(){{
            add(new <String,Object>HashMap() {{
                put("tp", "eq");
                put("cn", nodeIdColumnName);
                put("val", startNodeIdValue);
            }});
        }},new <String,Object>HashMap() {{
            put("id", "asc");
        }},null,null);
        int count = 0;
        Map tree = l1.get(0);
        int kk = 0;
        while(l1.size()>0 && ++count<10){
            //首次初始化
            List valueList = new ArrayList<Object>();
            for(Map node:l1){
                valueList.add(node.get(nodeIdColumnName));
                if(fieldReplace!=null && !fieldReplace.isEmpty()){
                    fieldReplace.forEach((key,value)->{
                        node.put(value, node.get(key));
                    });
                }
                String position = (String) node.get("position");
                if(position.equals("员工")){
                    node.put("staffId",node.get(nodeIdColumnName));
                }
                node.put("spread", true);
                node.put("checked", false);
               // node.put("id", kk++);
                Lutils.insertToTree(tree,node,parentIdColumnName,nodeIdColumnName,childrenKeyName);
            }
            //andCause.remove(andCause.size()-1);
            andCause.add(Lutils.genMap("tp","in","cn",parentIdColumnName,"vals",valueList));
            l1 = commonDBMapper.selectAndCauses(tableName, andCause,null,null,null);
        }
        return tree;
    }
    @Override
    public Map selectTreeMapUnEmptyFolder(String tableName, String parentIdColumnName,String nodeIdColumnName,
                                          Object startNodeIdValue, String childrenKeyName,Map fieldReplace,List<Map> andCause) {
        //检测是否有sql注入
        if(!Lutils.safe_sql(tableName)
                ||!Lutils.safe_sql(parentIdColumnName)
                ||!Lutils.safe_sql(nodeIdColumnName))
            return null;
        if(andCause==null)
            andCause = new ArrayList<Map>();
        andCause.add(Lutils.genMap("tp","eq","cn",nodeIdColumnName,"val",startNodeIdValue));
        List<Map> l1 = commonDBMapper.selectAndCauses(tableName, andCause, null,null,null);
        int count = 0;
        List<Map> l2 = new ArrayList<Map>();
        while(l1.size()>0 && ++count<10){
            l2.addAll(0,l1);
            //首次初始化
            List valueList = new ArrayList<Object>();
            for(Map node:l1){
                valueList.add(node.get(nodeIdColumnName));
                if(fieldReplace!=null && !fieldReplace.isEmpty()){
                    fieldReplace.forEach((key,value)->{
                        node.put(value, node.get(key));
                    });
                }
                node.put("spread", true);
            }
            andCause.remove(andCause.size()-1);
            andCause.add(Lutils.genMap("tp","in","cn",parentIdColumnName,"vals",valueList));
            l1 = commonDBMapper.selectAndCauses(tableName, andCause,Lutils.genMap("order_id", "asc"),null,null);
        }
        List children = new ArrayList<Map>();
        for(Map node:l2)
            Lutils.buildTree(children, node,parentIdColumnName,nodeIdColumnName,childrenKeyName);
        if(children.size()>0)
            return (Map)children.get(0);
        else
            return new HashMap<String,Object>();
    }
    @Override
    public Map createTable(String tableName, Map resultMap) {
        if(!Lutils.safe_sql(tableName)) {
            return new HashMap<String, Object>(){{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        int handlerResult =  commonDBMapper.createTable(tableName, resultMap);
        commonDBMapper.createIdSequence(tableName);
        return new HashMap<String, Object>(){{
            put("state", "success");
            put("result", handlerResult);
        }};
    }

    @Override
    public Map createLogTable(String tableName, Map resultMap) {
        if(!Lutils.safe_sql(tableName)) {
            return new HashMap<String, Object>(){{
                put("state", "failed");
                put("desc", "sql injection!");
            }};
        }
        int handlerResult =  commonDBMapper.createTable(tableName, resultMap);
        commonDBMapper.createIdSequence(tableName);
        commonDBMapper.createLogIdSequence(tableName);
        return new HashMap<String, Object>(){{
            put("state", "success");
            put("result", handlerResult);
        }};
    }
    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    @Override
    public int updateByIdBatch(String tableName, List<Map>updateDataList){
        //获取sqlsession
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false); //默认 single
        //获取对应mapper
        CommonDBMapper commonDBMapper = sqlSession.getMapper(CommonDBMapper.class);
        int count = 0;
        for(Map updateData:updateDataList){
            count += commonDBMapper.updateById(tableName, updateData.get("id"), updateData);
        }
        sqlSession.commit();
        sqlSession.close();
        return count;
    }

    @Override
    public int updateEqBatch(String tableName, List<Map>updateDataList, List equalCoulmnNameList){
        //获取sqlsession
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false); //默认 single
        //获取对应mapper
        CommonDBMapper commonDBMapper = sqlSession.getMapper(CommonDBMapper.class);
        int count = 0;
        for(Map updateData:updateDataList){
            Map equalMap = new HashMap<String,Object>();
            for(Object equalCoulmnName:equalCoulmnNameList){
                equalMap.put(equalCoulmnName,updateData.get(equalCoulmnName));
            }
            int rs = commonDBMapper.updateEq(tableName, equalMap, updateData);
            count ++;
        }
        sqlSession.commit();
        sqlSession.close();
        return count;
    }
    @Override
    public int insertByBatch(String tableName, List<Map> insertMapData){
        //获取sqlsession
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false); //默认 single
        //获取对应mapper
        CommonDBMapper commonDBMapper = sqlSession.getMapper(CommonDBMapper.class);
        int count = 0;
        for(Map insertData:insertMapData){
            count += commonDBMapper.insertMap(tableName,insertData);
        }
        sqlSession.commit();
        sqlSession.close();
        return count;
    }
    @Override
    public int copyTableIn(String fromTableName, String toTableName,List<String> copyColumnNameList, String inColumnName,List<Object>inValueList){
        if(!Lutils.safe_sql(fromTableName)
                ||!Lutils.safe_sql(fromTableName)
                ||!Lutils.safe_sql(inColumnName)
        ||!Lutils.safe_sql_list(copyColumnNameList)) {
            return -1;
        }
        int count = commonDBMapper.copyTableIn(fromTableName, toTableName, copyColumnNameList, inColumnName, inValueList);
        return count;
    }

    @Override
    public int addTableColumns(String tableName, Map columnMap){
        if(!Lutils.safe_sql(tableName)
                ||!Lutils.safe_sql_map(columnMap)) {
            return -1;
        }
        commonDBMapper.addTableColumns(tableName, columnMap);
        return 1;
    }

    @Override
    public int delTableColumns(String tableName, List columnNameList){
        if(!Lutils.safe_sql(tableName)
                ||!Lutils.safe_sql_list(columnNameList)) {
            return -1;
        }
        commonDBMapper.delTableColumns(tableName, columnNameList);
        return 1;
    }
    @Override
    public Integer selectMax(String tableName, String increasedColumn){
        if(!Lutils.safe_sql(tableName)
                ||!Lutils.safe_sql(increasedColumn)) {
            return null;
        }
        List<Map> rs= commonDBMapper.selectMax(tableName, increasedColumn);
        System.out.println(rs.get(0));
        if(rs.get(0)!=null){
            return Integer.parseInt(rs.get(0).get(increasedColumn).toString());
        }else{
            return 1;
        }
    }

    @Override
    public List<Map> selectDistinctAndCauses(String tableName, List<String> selectedColumns, List<Map> andCauses, Map sortMap, Object limit, Object offset) {
        //检测是否有sql注入
        if (!Lutils.safe_sql(tableName))
            return null;
        for (Map m : andCauses) {
            if (!Lutils.safe_sql_map(m))
                return null;
        }
        return commonDBMapper.selectDistinctAndCauses(tableName,selectedColumns, andCauses, sortMap, limit, offset);
    }

    @Override
    public Boolean isSystemManger(String staff_id) {
        List<Map> maps = commonDBMapper.selectEq("ly_sys_manager_table", Lutils.genMap("staff_id", Integer.parseInt(staff_id)), null);
        return maps.size()>0;
    }
    @Override
    public List<Map> selectInboxList(Object staff_id,Object eid, Object limit,Object offset){
        return commonDBMapper.selectInboxList(staff_id,eid,limit,offset);
    };

    @Override
    public List<Map> selectInboxList2(Object field,Object haveread,Object staff_id, Object limit,Object offset){
        return commonDBMapper.selectInboxList2(field,haveread,staff_id,limit,offset);
    };

    @Override
    public List<Map> selectOutboxList(Object staffId,Object eid, Object limit,Object offset){
        return commonDBMapper.selectOutboxList(staffId,eid,limit,offset);
    };

    @Override
    public List<Map> selectOutboxList2(Object field,Object staff_id,Object limit,Object offset){
        return commonDBMapper.selectOutboxList2(field,staff_id,limit,offset);
    }

    @Override
    public Integer selectCountInboxList(List<Map> andCauses) {
        return commonDBMapper.selectCountInboxList(andCauses);
    }

    @Override
    public Integer selectCountInboxList2(Object field, String haveread,Object staff_id) {
        return commonDBMapper.selectCountInboxList2(field,haveread,staff_id);
    }

    @Override
    public Integer selectCountOutboxList(Object staffId) {
        return commonDBMapper.selectCountOutboxList(staffId);
    }

    @Override
    public Integer selectCountOutboxList2(Object field, Object staff_id) {
        return commonDBMapper.selectCountOutboxList2(field,staff_id);
    }

    @Override
    public List<Map> mohuSearch(String sql) {
        return commonDBMapper.mohuSearch(sql);
    }

    @Override
    public List<Map> sqlSearch(String sql) {
        return commonDBMapper.sqlSearch(sql);
    }

    @Override
    public int updateDupName(){
        return commonDBMapper.updateDupName();
    }

    @Override
    public int selectZhijuZoufang(String beginDate, String endDate, String colName, List valueList) {
        return commonDBMapper.selectZhijuZoufang(beginDate,endDate, colName, valueList);
    }

    @Override
    public int selectStaffZoufang(String beginDate, String endDate, Map equalMap, List<String> selectedColumns) {
        return commonDBMapper.selectStaffZoufang(beginDate ,endDate, equalMap, selectedColumns);
    }

    @Override
    public List<Map> selectZhijuZoufangList(String beginDate, String endDate, String columnName, List valueList,Object limit,Object offset) {
        return commonDBMapper.selectZhijuZoufangList(beginDate,endDate, columnName, valueList,limit,offset);
    }

    @Override
    public List<Map> selectStaffZoufangList(String beginDate, String endDate,List<Map> andCauses,Object limit,Object offset) {
        return commonDBMapper.selectStaffZoufangList(beginDate ,endDate, andCauses,limit,offset);
    }

    @Override
    public int selectCountZhijuZoufangList(String beginDate, String endDate, String columnName, List valueList) {
        return commonDBMapper.selectCountZhijuZoufangList(beginDate,endDate, columnName, valueList);

    }

    @Override
    public int selectCountStaffZoufangList(String beginDate, String endDate,  List<Map> andCauses) {
        return commonDBMapper.selectCountStaffZoufangList(beginDate,endDate, andCauses);
    }

    @Override
    public int selectZhijuShangji(String beginDate, String endDate, String columnName, List valueList) {
        return commonDBMapper.selectZhijuShangji(beginDate,endDate,columnName,valueList);
    }

    @Override
    public int selectStaffShangjiWeekMonth(String beginDate, String endDate, Map equalMap, List<String> selectedColumns) {
        return commonDBMapper.selectStaffShangjiWeekMonth(beginDate,endDate,equalMap,selectedColumns);
    }

    @Override
    public int selectStaffShangjiWeekMonth2(String beginDate, String endDate, List<Map> andCauses) {
        return commonDBMapper.selectStaffShangjiWeekMonth2(beginDate,endDate,andCauses);
    }

    @Override
    public int selectStaffShangji(String beginDate, String endDate, Map equalMap, List<String> selectedColumns) {
        return commonDBMapper.selectStaffShangji(beginDate,endDate,equalMap,selectedColumns);
    }

    @Override
    public List<Map> selectZhijuShangjiList(String beginDate, String endDate, String columnName, List valueList) {
        return commonDBMapper.selectZhijuShangjiList(beginDate,endDate,columnName,valueList);
    }

    @Override
    public List<Map> selectStaffShangjiList(String beginDate, String endDate, Map equalMap, List<String> selectedColumns,Object limit,Object offset) {
        return commonDBMapper.selectStaffShangjiList(beginDate,endDate,equalMap,selectedColumns,limit,offset);
    }
    @Override
    public List<Map> selectStaffShangjiList2(String beginDate, String endDate, Map equalMap, List<String> selectedColumns,Object limit,Object offset) {
        return commonDBMapper.selectStaffShangjiList2(beginDate,endDate,equalMap,selectedColumns,limit,offset);
    }
    @Override
    public int selectCountStaffShangjiList(String beginDate, String endDate, Map equalMap, List<String> selectedColumns) {
        return commonDBMapper.selectCountStaffShangjiList(beginDate,endDate,equalMap,selectedColumns);
    }

    @Override
    public int selectCountStaffShangjiList2(String beginDate, String endDate, Map equalMap, List<String> selectedColumns) {
        return commonDBMapper.selectCountStaffShangjiList2(beginDate,endDate,equalMap,selectedColumns);
    }

    @Override
    public List<Map> selectColsAndCauses(String tableName, List<String> selectedColumns, List<Map> andCauses, Map sortMap, Object limit, Object offset) {
        if (!Lutils.safe_sql(tableName))
            return null;
        for (Map m : andCauses) {
            if (!Lutils.safe_sql_map(m))
                return null;
        }
        return commonDBMapper.selectColsAndCauses(tableName,selectedColumns, andCauses, sortMap, limit, offset);
    }

    @Override
    public List<Map> selectLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate, List<Map> andCauses,Map sortMap, Object limit, Object offset) {
        if (!Lutils.safe_sql(tableName))
            return null;
        for (Map m : andCauses) {
            if (!Lutils.safe_sql_map(m))
                return null;
        }
        return commonDBMapper.selectLinkTimeAndCauseList(tableName,timeCol,beginDate,endDate, andCauses,sortMap, limit, offset);
    }

    @Override
    public List<Map> selectNotLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate, List<Map> andCauses,Map sortMap, Object limit, Object offset) {
        if (!Lutils.safe_sql(tableName))
            return null;
        for (Map m : andCauses) {
            if (!Lutils.safe_sql_map(m))
                return null;
        }
        return commonDBMapper.selectNotLinkTimeAndCauseList(tableName,timeCol,beginDate,endDate, andCauses,sortMap, limit, offset);
    }

    @Override
    public int selectCountLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate, List<Map> andCauses) {
        return commonDBMapper.selectCountLinkTimeAndCauseList(tableName,timeCol,beginDate,endDate, andCauses);
    }

    @Override
    public int selectNotCountLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate, List<Map> andCauses) {
        return commonDBMapper.selectNotCountLinkTimeAndCauseList(tableName,timeCol,beginDate,endDate, andCauses);
    }
}
