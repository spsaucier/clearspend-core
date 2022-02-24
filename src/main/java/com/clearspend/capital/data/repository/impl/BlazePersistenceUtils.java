package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.JoinType;
import com.blazebit.persistence.PagedList;
import com.clearspend.capital.common.data.model.Versioned;
import com.clearspend.capital.service.type.PageToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.persistence.Tuple;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

@UtilityClass
public class BlazePersistenceUtils {

  public static <T> CriteriaBuilder<T> joinOnForeignKey(
      CriteriaBuilder<T> builder,
      Class<? extends Versioned> from,
      Class<? extends Versioned> to,
      JoinType joinType) {
    String fromName = getClassName(from);
    String toName = getClassName(to);
    String fromForeignKey = "%s.%sId".formatted(fromName, toName);
    String toPrimaryKey = "%s.id".formatted(toName);

    return builder.joinOn(to, toName, joinType).on(fromForeignKey).eqExpression(toPrimaryKey).end();
  }

  public static <T> CriteriaBuilder<T> joinOnPrimaryKey(
      CriteriaBuilder<T> builder,
      Class<? extends Versioned> from,
      Class<? extends Versioned> target,
      String targetForeignKeyName,
      JoinType joinType) {
    String targetName = getClassName(target);
    String fromId = "%s.id".formatted(getClassName(from));
    String toForeignKey = "%s.%s".formatted(getClassName(target), targetForeignKeyName);

    return builder.joinOn(target, targetName, joinType).on(fromId).eqExpression(toForeignKey).end();
  }

  @SuppressWarnings("unchecked")
  public <T extends Record> Page<T> queryPagedTuples(
      Class<T> recordClass,
      CriteriaBuilder<Tuple> builder,
      PageToken pageToken,
      boolean generateSelectStatement) {
    Constructor<T> recordConstructor = (Constructor<T>) recordClass.getConstructors()[0];
    Parameter[] constructorParameters = recordConstructor.getParameters();

    if (generateSelectStatement) {
      Stream.of(constructorParameters).forEach(p -> builder.select(p.getName()));
    }

    if (!CollectionUtils.isEmpty(pageToken.getOrderBy())) {
      pageToken
          .getOrderBy()
          .forEach(
              orderBy -> {
                switch (orderBy.getDirection()) {
                  case ASC -> builder.orderByAsc(orderBy.getItem());
                  case DESC -> builder.orderByDesc(orderBy.getItem());
                }
              });
    }
    builder.orderByDesc(constructorParameters[0].getName() + ".id");

    int pageNumber = pageToken.getPageNumber();
    int pageSize = pageToken.getPageSize();

    PagedList<Tuple> results = builder.page(pageNumber * pageSize, pageSize).getResultList();

    return new PageImpl<>(
        results.stream()
            .map(
                r -> {
                  Object[] values =
                      IntStream.range(0, constructorParameters.length).mapToObj(r::get).toArray();
                  return instantiateRecord(recordConstructor, values);
                })
            .collect(Collectors.toList()),
        PageRequest.of(pageNumber, pageSize),
        results.getTotalSize());
  }

  @SuppressWarnings("unchecked")
  public <T extends Record> List<T> queryTuples(
      Class<T> recordClass, CriteriaBuilder<Tuple> builder, boolean generateSelectStatement) {
    Constructor<T> recordConstructor = (Constructor<T>) recordClass.getConstructors()[0];
    Parameter[] constructorParameters = recordConstructor.getParameters();

    if (generateSelectStatement) {
      Stream.of(constructorParameters).forEach(p -> builder.select(p.getName()));
    }

    return builder.getResultList().stream()
        .map(
            r -> {
              Object[] values =
                  IntStream.range(0, constructorParameters.length).mapToObj(r::get).toArray();
              return instantiateRecord(recordConstructor, values);
            })
        .collect(Collectors.toList());
  }

  private static String getClassName(Class<?> clazz) {
    return StringUtils.uncapitalize(clazz.getSimpleName());
  }

  @SneakyThrows
  private <T extends Record> T instantiateRecord(Constructor<T> constructor, Object[] arguments) {
    return constructor.newInstance(arguments);
  }
}
