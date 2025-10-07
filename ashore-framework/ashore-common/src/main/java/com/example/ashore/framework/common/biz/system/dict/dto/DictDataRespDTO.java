package com.example.ashore.framework.common.biz.system.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPC 服务 - 字典数据 Response DTO")
@Data
public class DictDataRespDTO {

    @Schema(description = "字典标签", requiredMode = Schema.RequiredMode.REQUIRED, example = "保密")
    private String label;

    @Schema(description = "字典值", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private String value;

    @Schema(description = "字典类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "sys_common_sex")
    private String dictType;

    /**
     * see {@link com.example.ashore.framework.common.enums.CommonStatusEnum} 枚举
     */
    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

}
