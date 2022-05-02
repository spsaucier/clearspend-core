package com.clearspend.capital.common.data.util;

import com.clearspend.capital.common.GlobalObjectMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vladmihalcea.hibernate.type.util.JsonSerializer;
import com.vladmihalcea.hibernate.type.util.ObjectMapperWrapper;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.SerializationException;
import org.springframework.util.ClassUtils;

/**
 * A slightly modified version of the {@link
 * com.vladmihalcea.hibernate.type.util.ObjectMapperJsonSerializer} capable of properly detecting
 * base class in collections/maps to prevent errors during cloning of polymorphic json objects
 */
public class HibernateTypesJsonSerializer implements JsonSerializer {

  @SuppressWarnings("unchecked")
  public <T> T clone(T object) {
    if (object instanceof JsonNode jsonNode) {
      return (T) jsonNode.deepCopy();
    }

    if (object instanceof Collection collection) {
      Object firstElement = findFirstNonNullElement(collection);
      if (firstElement != null && !(firstElement instanceof Serializable)) {
        Class<?> targetClass = firstElement.getClass();
        for (Object element : collection) {
          if (element != null) {
            targetClass = ClassUtils.determineCommonAncestor(targetClass, element.getClass());
          }
        }
        JavaType type =
            TypeFactory.defaultInstance().constructParametricType(object.getClass(), targetClass);
        return ObjectMapperWrapperHolder.instance.fromBytes(
            ObjectMapperWrapperHolder.instance.toBytes(object), type);
      }
    }

    if (object instanceof Map map) {
      Map.Entry<?, ?> firstEntry = this.findFirstNonNullEntry(map);
      if (firstEntry != null) {
        Object key = firstEntry.getKey();
        Object value = firstEntry.getValue();
        Class<?> valueClass = value.getClass();
        if (!(key instanceof Serializable) || !(value instanceof Serializable)) {
          for (Object element : map.values()) {
            if (element != null) {
              valueClass = ClassUtils.determineCommonAncestor(valueClass, element.getClass());
            }
          }
          JavaType type =
              TypeFactory.defaultInstance()
                  .constructParametricType(object.getClass(), key.getClass(), valueClass);
          return ObjectMapperWrapperHolder.instance.fromBytes(
              ObjectMapperWrapperHolder.instance.toBytes(object), type);
        }
      }
    }
    if (object instanceof Serializable) {
      try {
        return (T) SerializationHelper.clone((Serializable) object);
      } catch (SerializationException e) {
        // it is possible that object itself implements java.io.Serializable, but underlying
        // structure does not
        // in this case we switch to the other JSON marshaling strategy which doesn't use the Java
        // serialization
        return jsonClone(object);
      }
    } else {
      return jsonClone(object);
    }
  }

  private <T> T findFirstNonNullElement(Collection<T> collection) {
    return collection.stream().filter(Objects::nonNull).findFirst().orElse(null);
  }

  private <K, V> Map.Entry<K, V> findFirstNonNullEntry(Map<K, V> map) {
    return findFirstNonNullElement(map.entrySet());
  }

  @SuppressWarnings("unchecked")
  private <T> T jsonClone(T object) {
    return ObjectMapperWrapperHolder.instance.fromBytes(
        ObjectMapperWrapperHolder.instance.toBytes(object), (Class<T>) object.getClass());
  }

  private static class ObjectMapperWrapperHolder {
    private static final ObjectMapperWrapper instance =
        new ObjectMapperWrapper(GlobalObjectMapper.get());
  }
}
