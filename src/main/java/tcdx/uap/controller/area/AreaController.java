package tcdx.uap.controller.area;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.*;
import tcdx.uap.common.entity.AjaxResult;
import tcdx.uap.common.utils.AreaUtils;
import tcdx.uap.common.utils.BeanUtils;
import tcdx.uap.controller.area.vo.AreaNodeRespVO;
import tcdx.uap.service.entities.Area;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/uap/area")
public class AreaController {
    @GetMapping("/tree")
    public AjaxResult getAreaTree() {
        Area area = AreaUtils.getArea(Area.ID_CHINA);
        Assert.notNull(area, "获取不到中国");
        return AjaxResult.success("success", BeanUtils.toBean(area.getChildren(), AreaNodeRespVO.class));
    }

    @PostMapping("/detail")
    public AjaxResult getAreaDetail(@RequestBody JSONObject req) {
        try {
            String areaJsonStr = req.getString("areaJson");
            JSONObject areaJson = JSON.parseObject(areaJsonStr);
            JSONArray region = areaJson.getJSONArray("region");
            String detailAddr = areaJson.getString("detail");
            String res = "";
            for (Object obj : region) {
                res = AreaUtils.format((Integer) obj, "");
            }
            res += detailAddr;
            return AjaxResult.success("success", res);
        } catch (Exception e) {
            return AjaxResult.error("参数解析失败: " + e.getMessage());
        }
    }
}
