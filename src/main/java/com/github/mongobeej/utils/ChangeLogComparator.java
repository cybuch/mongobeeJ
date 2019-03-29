package com.github.mongobeej.utils;

import com.github.mongobeej.changeset.ChangeLog;

import java.io.Serializable;
import java.util.Comparator;

import static org.springframework.util.StringUtils.hasText;

class ChangeLogComparator implements Comparator<Class<?>>, Serializable {
    @Override
    public int compare(Class<?> left, Class<?> right) {
        ChangeLog leftChangeLog = left.getAnnotation(ChangeLog.class);
        ChangeLog rightChangeLog = right.getAnnotation(ChangeLog.class);
        String leftOrder = !(hasText(leftChangeLog.order())) ? left.getCanonicalName() : leftChangeLog.order();
        String rightOrder = !(hasText(rightChangeLog.order())) ? right.getCanonicalName() : rightChangeLog.order();
        if (leftOrder == null && rightOrder == null) {
            return 0;
        } else if (leftOrder == null) {
            return -1;
        } else if (rightOrder == null) {
            return 1;
        }
        return leftOrder.compareTo(rightOrder);
    }
}
