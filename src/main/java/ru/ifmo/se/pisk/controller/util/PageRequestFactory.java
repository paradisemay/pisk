package ru.ifmo.se.pisk.controller.util;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {

    private PageRequestFactory() {
    }

    public static Pageable build(Integer page, Integer size, String sort, Sort.Direction direction, Set<String> allowedFields) {
        if (allowedFields != null && !allowedFields.contains(sort)) {
            throw new IllegalArgumentException("Недопустимое поле сортировки: " + sort);
        }
        int resolvedPage = page == null ? 0 : Math.max(page, 0);
        int resolvedSize = size == null ? 20 : size;
        if (resolvedSize < 20 || resolvedSize > 50) {
            throw new IllegalArgumentException("Размер страницы должен быть в диапазоне от 20 до 50");
        }
        Sort sortDefinition = sort == null ? Sort.unsorted() : Sort.by(direction, sort);
        return PageRequest.of(resolvedPage, resolvedSize, sortDefinition);
    }
}
