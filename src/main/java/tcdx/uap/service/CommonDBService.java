package tcdx.uap.service;

import java.util.List;
import java.util.Map;

public interface CommonDBService {
    List<Map> selectById(String tableName, Object id);
    //根据id列表查询数据集
    List<Map> selectIn(String tableName, String colName, List valueList);
    List<Map> selectEq(String tableName, Map equalMap,List<String> selectedColumns);
    List<Map> selectDistinctEq(String tableName, Map equalMap,List<String> selectedColumns);
    List<Map> selectAndCauses(String tableName,List<Map> andCauses,Map sortMap, Object limit,Object offset);
    Integer selectAndCausesCount(String tableName, List<Map> andCauses);
    List<Map> selectAndCausesCountGroupBy(String tableName, List<Map> andCauses,List groupByColumnLists);
    Map updateById(String tableName,Object id,Map updateData);
    Map updateIn(String tableName,String columnName, List valueList,Map updateData);
    Map updateEq(String tableName, Map equalMap, Map updateData);
    Map deleteById(String tableName, Object id);
    Map deleteEq(String tableName,Map equalMap);
    Map insertMap(String tableName,Map insertMap);
    List selectTreeList(String tableName, String parentIdColumnName,String nodeIdColumnName, Object startNodeIdValue,List<Map> andCause);
    Map selectTreeMap(String tableName, String parentIdColumnName,String nodeIdColumnName,
                             Object startNodeIdValue, String childrenKeyName,Map fieldReplace,List<Map> andCause);
    Map selectLinkWorkerTreeMap(String tableName, String parentIdColumnName,String nodeIdColumnName,
                      Object startNodeIdValue, String childrenKeyName,Map fieldReplace,List<Map> andCause);
    Map selectTreeMapUnEmptyFolder(String tableName, String parentIdColumnName,String nodeIdColumnName,
                               Object startNodeIdValue, String childrenKeyName,Map fieldReplace,List<Map> andCause);
    Map createTable(String tableName, Map resultMap);

    Map createLogTable(String tableName, Map resultMap);
    int updateByIdBatch(String tableName, List<Map>updateData);
    int copyTableIn(String fromTableName, String toTableName,List<String> copyColumnNameList, String inColumnName,List<Object>inColumnList);
    List<Map> selectEqSortLimit(String tableName, Map equalMap,List<String> selectedColumns,Map sortMap,Integer offset,Integer limit);
    int updateEqBatch(String tableName, List<Map>updateDataList, List equalCoulmnNameList);
    int insertByBatch(String tableName, List<Map> insertMapData);

    int addTableColumns(String tableName, Map columnMap);
    int delTableColumns(String tableName, List columnNameList);
    Integer selectMax(String tableName, String increasedColumn);

    List<Map> selectDistinctAndCauses(String tableName,List<String> selectedColumns, List<Map> andCauses, Map sortMap, Object limit,Object offset);

    Boolean isSystemManger(String staff_id);

    List<Map> selectInboxList(Object staff_id,Object eid, Object limit,Object offset);
    List<Map> selectInboxList2(Object field,Object haveread,Object staff_id,Object limit,Object offset);
    List<Map> selectOutboxList(Object staffId,Object eid, Object limit,Object offset);
    List<Map> selectOutboxList2(Object field,Object staff_id,Object limit,Object offset);

    Integer selectCountInboxList(List<Map> andCauses);
    Integer selectCountInboxList2(Object field,String haveread,Object staff_id);
    Integer selectCountOutboxList(Object staffId);
    Integer selectCountOutboxList2(Object field,Object staff_id);

    List<Map> mohuSearch(String sql);

    List<Map> sqlSearch(String sql);

    int updateDupName();


    int selectZhijuZoufang(String beginDate,String endDate, String colName, List valueList);
    int selectStaffZoufang(String beginDate,String endDate, Map equalMap,List<String> selectedColumns);

    List<Map> selectZhijuZoufangList(String beginDate, String endDate, String columnName, List valueList,Object limit,Object offset);
    List<Map> selectStaffZoufangList(String beginDate, String endDate, List<Map> andCauses,Object limit,Object offset);
    int selectCountZhijuZoufangList(String beginDate, String endDate, String columnName, List valueList);
    int selectCountStaffZoufangList(String beginDate, String endDate, List<Map> andCauses);

    int selectZhijuShangji(String beginDate, String endDate, String columnName, List valueList);
    int selectStaffShangji(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);
    int selectStaffShangjiWeekMonth(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);
    int selectStaffShangjiWeekMonth2(String beginDate, String endDate,List<Map> andCauses);

    List<Map> selectZhijuShangjiList(String beginDate, String endDate, String columnName, List valueList);
    List<Map> selectStaffShangjiList(String beginDate, String endDate, Map equalMap,List<String> selectedColumns,Object limit,Object offset);
    List<Map> selectStaffShangjiList2(String beginDate, String endDate, Map equalMap,List<String> selectedColumns,Object limit,Object offset);
    int selectCountStaffShangjiList(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);
    int selectCountStaffShangjiList2(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);
    List<Map> selectColsAndCauses(String tableName,List<String> selectedColumns, List<Map> andCauses, Map sortMap, Object limit,Object offset);

    List<Map> selectLinkTimeAndCauseList(String tableName,String timeCol,String beginDate, String endDate,
                                         List<Map> andCauses,Map sortMap,Object limit,Object offset);
    List<Map> selectNotLinkTimeAndCauseList(String tableName,String timeCol,String beginDate, String endDate,
                                         List<Map> andCauses,Map sortMap,Object limit,Object offset);
    int selectCountLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate,
                                              List<Map> andCauses);
    int selectNotCountLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate,
                                        List<Map> andCauses);
}
