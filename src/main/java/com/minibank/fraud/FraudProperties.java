package com.minibank.fraud;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The fraud rules' limits, set in application.yml (bank.fraud.*) so they can change without touching code.
 *
 * @param largeAmount  a single transfer at or above this many pounds gets flagged
 * @param rapidCount   this many transfers out of one account...
 * @param rapidWindow  ...within this long gets flagged
 */
@ConfigurationProperties(prefix = "bank.fraud")
public record FraudProperties(BigDecimal largeAmount, int rapidCount, Duration rapidWindow) {
}
