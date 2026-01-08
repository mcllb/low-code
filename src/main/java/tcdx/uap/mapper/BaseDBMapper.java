package tcdx.uap.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 用户与角色关联表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface BaseDBMapper
{
    public List<Map> selectById(Map<String, Object> m);
    public List<Map> selectIn(Map<String, Object> m);
    public List<Map> selectNew(Map<String, Object> m);
    public List<Map> selectEq(Map<String, Object> m);
    public List<Map> selectMaxByCauses(Map<String, Object> m);
    public List<Map> selectMaxColEq(Map<String, Object> m);
    public List<Map> selectByCauses(Map<String, Object> m);
    public List<Map> selectAndOrByCauses(Map<String, Object> m);
    public List<Map> selectCountByCauses(Map m);
    public List<Map> selectCountGroupByCauses(Map m);
    public int updateById(Map<String,Object> m);
    public int updateIn(Map<String,Object> m);
    public int updateEq(Map<String,Object> m);
    public int deleteById(Map m);
    public int deleteEq(Map<String,Object> m);
    public int deleteIn(Map<String,Object> m);
    public int insertMap(Map<String,Object> m);
    public List insertMapRetRow(Map<String,Object> m);
    public int tableDropColumn(Map<String,Object> m);
    public int tableAddColumn(Map<String,Object> m);
    public int tableAlterColumnType(Map<String,Object> m);
    public int tableAlterColumnName(Map<String,Object> m);
    public int deleteByCause(Map<String,Object> m);
    public int executeSql(String sql);
    public int executeSql(Map<String,Object> m);
    public List<Map> selectSql(Map params);
    public List<Map> selectNewById(Map params);
    public List<Map> selectEqAndIn(Map<String, Object> m);
    List<Map> selectTreeList(Map<String, Object> m);
    List<Map> selectDefinedSql(Map<String, Object> m);
    List<Map> selectDefinedSqlCounts(Map<String, Object> m);

}
