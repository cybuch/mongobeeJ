package com.github.mongobeej.utils;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.changeset.ChangeLog;
import com.github.mongobeej.changeset.ChangeSet;
import com.github.mongobeej.exception.MongobeeChangeSetException;
import org.reflections.Reflections;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Utilities to deal with reflections and annotations
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeService {
  private static final String DEFAULT_PROFILE = "default";

  private final String changeLogsBasePackage;
  private final ProfileMatcher profileMatcher;

  public ChangeService(String changeLogsBasePackage) {
    this(changeLogsBasePackage, null);
  }

  public ChangeService(String changeLogsBasePackage, Environment environment) {
    this.changeLogsBasePackage = changeLogsBasePackage;


    List<String> activeProfiles;
    if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length> 0) {
      activeProfiles = asList(environment.getActiveProfiles());
    } else {
      activeProfiles = asList(DEFAULT_PROFILE);
    }
    profileMatcher = new ProfileMatcher(activeProfiles);
  }

  public List<Class<?>> fetchChangeLogs(){
    Reflections reflections = new Reflections(changeLogsBasePackage);
    Set<Class<?>> changeLogs = reflections.getTypesAnnotatedWith(ChangeLog.class); // TODO remove dependency, do own method
    List<Class<?>> filteredChangeLogs = (List<Class<?>>) profileMatcher.filterByActiveProfiles(changeLogs);

    Collections.sort(filteredChangeLogs, new ChangeLogComparator());

    return filteredChangeLogs;
  }

  public List<Method> fetchChangeSets(final Class<?> type) throws MongobeeChangeSetException {
    final List<Method> changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));
    final List<Method> filteredChangeSets = (List<Method>) profileMatcher.filterByActiveProfiles(changeSets);

    Collections.sort(filteredChangeSets, new ChangeSetComparator());

    return filteredChangeSets;
  }

  public boolean isRunAlwaysChangeSet(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
      ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
      return annotation.runAlways();
    } else {
      return false;
    }
  }

  public ChangeEntry createChangeEntry(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
      ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
  
      return new ChangeEntry(
          annotation.id(),
          annotation.author(),
          new Date(),
          changesetMethod.getDeclaringClass().getName(),
          changesetMethod.getName());
    } else {
      return null;
    }
  }


  private List<Method> filterChangeSetAnnotation(List<Method> allMethods) throws MongobeeChangeSetException {
    final Set<String> changeSetIds = new HashSet<>();
    final List<Method> changesetMethods = new ArrayList<>();
    for (final Method method : allMethods) {
      if (method.isAnnotationPresent(ChangeSet.class)) {
        String id = method.getAnnotation(ChangeSet.class).id();
        if (changeSetIds.contains(id)) {
          throw new MongobeeChangeSetException(String.format("Duplicated changeset id found: '%s'", id));
        }
        changeSetIds.add(id);
        changesetMethods.add(method);
      }
    }
    return changesetMethods;
  }

}
