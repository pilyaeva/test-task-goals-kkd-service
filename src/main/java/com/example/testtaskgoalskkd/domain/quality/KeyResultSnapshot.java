package com.example.testtaskgoalskkd.domain.quality;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ключевой результат цели в том виде, в каком он нужен правилам.
 *
 * @param weight      вес КР; может быть null
 * @param updatedDate дата последнего обновления факта; null — факт ни разу не обновлялся
 */
public record KeyResultSnapshot(BigDecimal weight, LocalDate updatedDate) {
}
