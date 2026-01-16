package com.example.only4_kafka.global.common;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ToString
@Getter
public class PageInfo {

    private Integer currentPage;

    private Integer totalPage;

    private Integer pageSize;

    public PageInfo(){};

    @Builder
    public PageInfo(Integer currentPage, Integer totalPage, Integer pageSize) {
        this.currentPage = currentPage;
        this.totalPage = totalPage;
        this.pageSize = pageSize;
    }

    public static PageInfo toPageInfo(Pageable pageable, Page<?> pageContent) {
        return PageInfo.builder()
                .currentPage(pageable.getPageNumber() + 1)
                .totalPage(pageContent.getTotalPages())
                .pageSize(pageable.getPageSize())
                .build();
    }
}
