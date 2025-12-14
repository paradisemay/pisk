package ru.ifmo.se.pisk.controller.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageRequestFactoryTest {

    @Test
    void buildsDefaultPageableWithBounds() {
        Pageable pageable = PageRequestFactory.build(null, null, "rating", Sort.Direction.ASC, Set.of("rating"));

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("rating")).isNotNull();
    }

    @Test
    void rejectsUnknownSortField() {
        assertThatThrownBy(() -> PageRequestFactory.build(0, 20, "unknown", Sort.Direction.ASC, Set.of("rating")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Недопустимое поле сортировки");
    }

    @Test
    void rejectsTooLargePageSize() {
        assertThatThrownBy(() -> PageRequestFactory.build(0, 100, "rating", Sort.Direction.ASC, Set.of("rating")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Размер страницы");
    }

    @Test
    void rejectsTooSmallPageSize() {
        assertThatThrownBy(() -> PageRequestFactory.build(0, 10, "rating", Sort.Direction.ASC, Set.of("rating")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Размер страницы");
    }
}
