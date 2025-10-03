package com.example.ashore.framework.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Schema(description="分页查询请求参数基类")
@Data
public class PageParam implements Serializable {

    private static final Integer PAGE_NO = 1;    // 页码
    private static final Integer PAGE_SIZE = 10; // 每页条数

    /**
     * 每页条数
     * 例如说，导出接口，可以设置 {@link #pageSize} 为 -1 不分页，查询所有数据。
     */
    public static final Integer NO_PAGINATION = -1;

    @Schema(description = "页码，从 1 开始", requiredMode = Schema.RequiredMode.REQUIRED,example = "1")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = PAGE_NO;

    @Schema(description = "每页条数，最大值为 100", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 100, message = "每页条数最大值为 100")
    private Integer pageSize = PAGE_SIZE;

    // 计算数据库查询的开始位置（例如 MySQL 的 offset，结合 limit 做分页查询时用）
    public Integer getStart() {
        return (pageNo - 1) * pageSize;
    }

    // 判断是否不分页
    public boolean isNoPagination() {
        return NO_PAGINATION.equals(pageSize);
    }

}
