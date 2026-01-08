package tcdx.uap.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tcdx.uap.common.entity.AjaxResult;
import tcdx.uap.service.GenModuleService;
import tcdx.uap.service.vo.GenerateModuleReq;

import javax.annotation.Resource;

@RestController
@RequestMapping("/uap/module")
public class ModuleGenerateController {

    @Resource
    private GenModuleService genModuleService;

    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody GenerateModuleReq req) throws Exception {
        genModuleService.generateModule(req);
        return AjaxResult.success();
    }

}
