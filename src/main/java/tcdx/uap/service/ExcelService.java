package tcdx.uap.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.spi.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tcdx.uap.common.utils.Lutils;
import tcdx.uap.service.Listener.NoModleDataListener;
import tcdx.uap.service.entities.Table;
import tcdx.uap.service.entities.TableCol;
import tcdx.uap.service.store.Modules;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    @Autowired
    private BaseDBService baseDBService;

    @Transactional
    public void importExcel(MultipartFile file, String table_id, HttpSession session) throws IOException {
        //导入功能，根据table_id获取字段
//        List<Map> columns_list = baseDBService.selectEq("v_table_col",
//                Lutils.genMap("table_id",table_id));
        Table table = (Table)Modules.getInstance().get(table_id,false);
        List<TableCol> cols = table.cols;
        String tableName = table.table_name;

//        String table_name = baseDBService.selectEq("v_table",
//                Lutils.genMap("id", table_id)).get(0).get("table_name").toString();

        int user_id = Integer.parseInt(session.getAttribute("userId").toString());
        List<Map> userlist = baseDBService.selectEq("v_user", Lutils.genMap("id", user_id));
        String create_staff_nm = userlist.get(0).get("staff_nm").toString();
        int create_staff_no = Integer.parseInt(userlist.get(0).get("id").toString());
        int dept_id = Integer.parseInt(userlist.get(0).get("belong_group_id").toString());

        //5.读取excel数据，和表头结构
        EasyExcel.read(file.getInputStream(),
                new NoModleDataListener(tableName,create_staff_nm,cols,
                        create_staff_no,dept_id,baseDBService)).sheet().doRead();

        
    }
}
