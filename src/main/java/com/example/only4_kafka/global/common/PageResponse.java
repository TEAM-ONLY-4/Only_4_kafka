package com.example.only4_kafka.global.common;


import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class PageResponse {

    private PageInfo pageInfo;

    private List<?> dataList = new ArrayList<>();

    public static PageResponse toPageResponse(PageInfo pageInfo, List<?> dataList) {
        return PageResponse.builder()
                .pageInfo(pageInfo)
                .dataList(dataList)
                .build();
    }
}
