package com.github.mongobeej.utils;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.changeset.ChangeLog;
import com.github.mongobeej.changeset.ChangeSet;
import com.github.mongobeej.exception.MongobeeChangeSetException;
import org.reflections.Reflections;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;

public class ChangeService {
    private static final String DEFAULT_PROFILE = "default";

    private final String changeLogsBasePackage;
    private final ProfileMatcher profileMatcher;

    public ChangeService(String changeLogsBasePackage, Environment environment) {
        this.changeLogsBasePackage = changeLogsBasePackage;
        List<String> activeProfiles = getActiveProfiles(environment);
        profileMatcher = new ProfileMatcher(activeProfiles);
    }

    private List<String> getActiveProfiles(Environment environment) {
        if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length > 0) {
            return asList(environment.getActiveProfiles());
        } else {
            return asList(DEFAULT_PROFILE);
        }
    }

    public List<Class<?>> fetchChangeLogs() {
        Reflections reflections = new Reflections(changeLogsBasePackage);
        Set<Class<?>> changeLogs = reflections.getTypesAnnotatedWith(ChangeLog.class); // TODO remove dependency, do own method
        List<Class<?>> filteredChangeLogs = (List<Class<?>>) profileMatcher.filterByActiveProfiles(changeLogs);
        filteredChangeLogs.sort(new ChangeLogComparator());
        return filteredChangeLogs;
    }

    public List<Method> fetchChangeSets(final Class<?> type) throws MongobeeChangeSetException {
        final List<Method> changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));
        final List<Method> filteredChangeSets = (List<Method>) profileMatcher.filterByActiveProfiles(changeSets);
        filteredChangeSets.sort(new ChangeSetComparator());
        return filteredChangeSets;
    }

    public boolean isRunAlwaysChangeSet(Method changeseSMethod) {
        if (changeseSMethod.isAnnotationPresent(ChangeSet.class)) {
            ChangeSet annotation = changeseSMethod.getAnnotation(ChangeSet.class);
            return annotation.runAlways();
        } else {
            return false;
        }
    }

    public ChangeEntry createChangeEntry(Method changeSetMethod) {
        if (changeSetMethod.isAnnotationPresent(ChangeSet.class)) {
            ChangeSet annotation = changeSetMethod.getAnnotation(ChangeSet.class);
            return new ChangeEntry(
                    annotation.id(),
                    annotation.author(),
                    new Date(),
                    changeSetMethod.getDeclaringClass().getName(),
                    changeSetMethod.getName());
        } else {
            return null;
        }
    }

    private List<Method> filterChangeSetAnnotation(List<Method> allMethods) throws MongobeeChangeSetException {
        Set<String> changeSetIds = new HashSet<>();
        final List<Method> changeSetMethods = new ArrayList<>();
        for (Method method : allMethods) {
            if (method.isAnnotationPresent(ChangeSet.class)) {
                String changeId = method.getAnnotation(ChangeSet.class).id();
                if (changeSetIds.contains(changeId)) {
                    throw new MongobeeChangeSetException(String.format("Duplicated change set id found: '%s'", changeId));
                }
                changeSetIds.add(changeId);
                changeSetMethods.add(method);
            }
        }
        return changeSetMethods;
    }
}
