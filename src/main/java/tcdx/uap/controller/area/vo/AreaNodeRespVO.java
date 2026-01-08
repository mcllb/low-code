package tcdx.uap.controller.area.vo;

import lombok.Data;

import java.util.List;

@Data
public class AreaNodeRespVO {

    private Integer value;

    private String label;

    /**
     * 子节点
     */
    private List<AreaNodeRespVO> children;
}
