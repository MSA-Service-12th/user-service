package com.loopang.userservice.infrastructure.feign;

import com.loopang.common.response.CommonResponse;
import com.loopang.userservice.infrastructure.feign.dto.HubData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubFeignClient {

    @GetMapping("/api/hubs/{hubId}")
    CommonResponse<HubData> getHub(@PathVariable("hubId") UUID hubId);
}
