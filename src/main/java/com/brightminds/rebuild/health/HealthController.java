package com.brightminds.rebuild.health;

import com.brightminds.rebuild.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthStatus> health() {
        HealthStatus status = new HealthStatus("UP", "brightminds-rebuild");
        return ApiResponse.success(status);
    }
}
