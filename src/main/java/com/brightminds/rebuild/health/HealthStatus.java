package com.brightminds.rebuild.health;

/**
 * 健康检查接口返回的业务数据。
 */
public record HealthStatus(String status, String service) {
}
