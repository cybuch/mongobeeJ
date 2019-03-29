package com.github.mongobeej.utils;

import com.github.mongobeej.changeset.ChangeSet;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Comparator;

class ChangeSetComparator implements Comparator<Method>, Serializable {
  @Override
  public int compare(Method left, Method right) {
    ChangeSet leftChangeSet = left.getAnnotation(ChangeSet.class);
    ChangeSet rightChangeSet = right.getAnnotation(ChangeSet.class);
    return leftChangeSet.order().compareTo(rightChangeSet.order());
  }
}
