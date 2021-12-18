package com.clearspend.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;

/**
 * Paged search results container. Light substitution for standard Page interface implementations
 * (like PageImpl)
 *
 * @param <T> type of content items
 */
@Value
@RequiredArgsConstructor
public class PagedData<T> {

  @JsonProperty("pageNumber")
  private final int pageNumber;

  @JsonProperty("pageSize")
  private final int pageSize;

  @JsonProperty("totalElements")
  private final long totalElements;

  @JsonProperty("content")
  private final List<T> content;

  public static <T> PagedData<T> of(Page<T> page) {
    return new PagedData<>(
        page.getNumber(), page.getSize(), page.getTotalElements(), page.getContent());
  }

  public static <T, M> PagedData<T> of(Page<M> page, Function<M, T> transform) {
    return new PagedData<>(
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getContent().stream().map(transform).collect(Collectors.toList()));
  }
}
