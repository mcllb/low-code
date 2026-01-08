package tcdx.uap.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommonDBMapper {
    List<Map> selectById(String tableName,Object id);
    List<Map> selectEq(String tableName,Map equalMap,List<String> selectedColumns);
    List<Map> selectDistinctEq(String tableName,Map equalMap,List<String> selectedColumns);
    List<Map> selectIn(String tableName,String columnName, List valueList);
    List<Map> selectAndCauses(String tableName, List<Map> andCauses, Map sortMap, Object limit,Object offset);
    List<Map> selectColsAndCauses(String tableName,List<String> selectedColumns, List<Map> andCauses, Map sortMap, Object limit,Object offset);
    List<Map> selectDistinctAndCauses(String tableName,List<String> selectedColumns, List<Map> andCauses, Map sortMap, Object limit,Object offset);
    List<Map> selectAndCausesCount(String tableName, List<Map> andCauses);
    int updateById(String tableName, Object id, Map updateData);
    int updateIn(String tableName, String columnName, List valueList, Map updateData);
    int updateEq(String tableName, Map equalMap, Map updateData);
    int deleteById(String tableName, Object id);
    int deleteEq(String tableName,Map equalMap);
    int insertMap(String tableName,Map insertMap);
    int createTable(String tableName,Map createMap);
    int createIdSequence(String tableName);
    int createLogIdSequence(String tableName);
    int copyTableIn(String fromTableName, String toTableName,List<String> copyColumnNameList, String inColumnName,List<Object>inValueList);
    List<Map> selectEqSortLimit(String tableName, Map equalMap,List<String> selectedColumns,Map sortMap,Integer offset,Integer limit);
    int addTableColumns(String tableName, Map columnMap);
    int delTableColumns(String tableName, List columnNameList);
    List<Map> selectMax(String tableName, String increasedColumn);
    List<Map> selectAndCausesCountGroupBy(String tableName, List<Map> andCauses, List groupByColumnLists);
    List<Map> selectInboxList(Object staff_id,Object eid, Object limit,Object offset);
    List<Map> selectInboxList2(Object field,Object haveread,Object staff_id, Object limit,Object offset);
    List<Map> selectOutboxList(Object staffId,Object eid,Object limit,Object offset);
    List<Map> selectOutboxList2(Object field,Object staff_id, Object limit, Object offset);

    Integer selectCountInboxList(List<Map> andCauses);
    Integer selectCountInboxList2(Object field,String haveread,Object staff_id);
    Integer selectCountOutboxList(Object staffId);
    Integer selectCountOutboxList2(Object field,Object staff_id);

    List<Map> mohuSearch(String sql);
    List<Map> sqlSearch(String sql);

    int updateDupName();

    int selectZhijuZoufang(String beginDate, String endDate, String columnName, List valueList);
    int selectStaffZoufang(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);

    List<Map> selectZhijuZoufangList(String beginDate, String endDate, String columnName, List valueList,Object limit,Object offset);
    List<Map> selectStaffZoufangList(String beginDate, String endDate, List<Map> andCauses,Object limit,Object offset);
    int selectCountZhijuZoufangList(String beginDate, String endDate, String columnName, List valueList);
    int selectCountStaffZoufangList(String beginDate, String endDate, List<Map> andCauses);


    int selectZhijuShangji(String beginDate, String endDate, String columnName, List valueList);
    int selectStaffShangji(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);
    int selectStaffShangjiWeekMonth(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);


    List<Map> selectZhijuShangjiList(String beginDate, String endDate, String columnName, List valueList);
    List<Map> selectStaffShangjiList(String beginDate, String endDate, Map equalMap,List<String> selectedColumns,Object limit,Object offset);
    List<Map> selectStaffShangjiList2(String beginDate, String endDate, Map equalMap,List<String> selectedColumns,Object limit,Object offset);

    int selectCountStaffShangjiList(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);
    int selectCountStaffShangjiList2(String beginDate, String endDate, Map equalMap,List<String> selectedColumns);

    List<Map> getManagerTubiaoInfo(String zhiju);
    List<Map> getManagerProductTubiaoInfo(String zhiju);

    List<Map> getKehuTubiaoInfo(String kehuName);

    List<Map> selectLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate,
                                         List<Map> andCauses,Map sortMap, Object limit, Object offset);

    List<Map> selectNotLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate,
                                         List<Map> andCauses,Map sortMap, Object limit, Object offset);

    int selectCountLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate,List<Map> andCauses);

    int selectNotCountLinkTimeAndCauseList(String tableName,String timeCol, String beginDate, String endDate,List<Map> andCauses);

    int selectStaffShangjiWeekMonth2(String beginDate, String endDate, List<Map> andCauses);
}
