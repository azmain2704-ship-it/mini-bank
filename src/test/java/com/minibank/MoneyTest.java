package com.minibank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.minibank.common.InvalidRequestException;
import com.minibank.common.Money;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void poundsBecomeWholePence() {
        assertThat(Money.toPence(new BigDecimal("12.50"))).isEqualTo(1250);
        assertThat(Money.toPence(new BigDecimal("0.01"))).isEqualTo(1);
        assertThat(Money.toPence(new BigDecimal("7"))).isEqualTo(700);
    }

    @Test
    void penceBecomePoundsWithTwoDecimals() {
        assertThat(Money.toPounds(1250)).isEqualByComparingTo("12.50");
        assertThat(Money.toPounds(1250).toPlainString()).isEqualTo("12.50");
    }

    @Test
    void fractionsOfAPennyAreRefusedNotRounded() {
        assertThatThrownBy(() -> Money.toPence(new BigDecimal("1.005"))).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void whyNotDoubles() {
        // The reason balances are longs: doubles can't even add 10p and 20p exactly
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);
        assertThat(Money.toPence(new BigDecimal("0.10")) + Money.toPence(new BigDecimal("0.20"))).isEqualTo(30);
    }
}
