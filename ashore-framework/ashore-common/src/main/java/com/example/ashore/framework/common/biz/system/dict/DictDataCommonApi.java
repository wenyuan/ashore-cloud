package com.example.ashore.framework.common.biz.system.dict;

import com.example.ashore.framework.common.biz.system.dict.dto.DictDataRespDTO;
import com.example.ashore.framework.common.enums.RpcConstants;
import com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.example.ashore.framework.common.pojo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = RpcConstants.SYSTEM_NAME, primary = false, fallbackFactory = DictDataCommonApi.DictDataCommonApiFallbackFactory.class)
@Tag(name = "RPC 服务 - 字典数据")
public interface DictDataCommonApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/dict-data";

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "获得指定字典类型的字典数据列表")
    @Parameter(name = "dictType", description = "字典类型", example = "SEX", required = true)
    ApiResponse<List<DictDataRespDTO>> getDictDataList(@RequestParam("dictType") String dictType);

    @Slf4j
    @Component
    class DictDataCommonApiFallbackFactory implements FallbackFactory<DictDataCommonApi> {

        @Override
        public DictDataCommonApi create(Throwable cause) {
            log.error("[DictDataCommonApi][字典数据服务调用失败]", cause);
            return dictType -> ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "字典服务调用失败，请稍后重试");
        }
    }

}
