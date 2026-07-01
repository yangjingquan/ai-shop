package com.shop.common.response;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        PageResult<T> r = new PageResult<>();
        r.list = list;
        r.total = total;
        r.pageNum = pageNum;
        r.pageSize = pageSize;
        return r;
    }
}
