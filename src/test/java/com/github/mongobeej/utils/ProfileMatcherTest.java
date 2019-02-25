package com.github.mongobeej.utils;

import org.junit.Test;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.AnnotatedElement;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfileMatcherTest {

  private ProfileMatcher profileMatcher;

  @Test
  public void matchesPositive() throws Exception {
    profileMatcher = new ProfileMatcher(asList("local"));
    AnnotatedElement annotatedElement = createAnnotatedElement(new String[]{"dev", "local"});

    List<?> result = profileMatcher.filterByActiveProfiles(asList(annotatedElement));

    assertEquals(result.size(), 1);
    assertEquals(result.get(0), annotatedElement);
  }

  @Test
  public void doesNotmatchPositive() throws Exception {
    profileMatcher = new ProfileMatcher(asList("local"));
    AnnotatedElement annotatedElement = createAnnotatedElement(new String[]{"dev", "qa"});

    List<?> result = profileMatcher.filterByActiveProfiles(asList(annotatedElement));

    assertEquals(result.size(), 0);
  }

  @Test
  public void matchesSingleNegative() throws Exception {
    profileMatcher = new ProfileMatcher(asList("local"));
    AnnotatedElement annotatedElement = createAnnotatedElement(new String[]{"!dev"});

    List<?> result = profileMatcher.filterByActiveProfiles(asList(annotatedElement));

    assertEquals(result.size(), 1);
    assertEquals(result.get(0), annotatedElement);
  }

  @Test
  public void matchesMultiNegative() throws Exception {
    profileMatcher = new ProfileMatcher(asList("local"));
    AnnotatedElement annotatedElement = createAnnotatedElement(new String[]{"!dev", "!prod"});

    List<?> result = profileMatcher.filterByActiveProfiles(asList(annotatedElement));

    assertEquals(result.size(), 1);
    assertEquals(result.get(0), annotatedElement);
  }

  @Test
  public void missSingleNegative() throws Exception {
    profileMatcher = new ProfileMatcher(asList("local"));
    AnnotatedElement annotatedElement = createAnnotatedElement(new String[]{"!local"});

    List<?> result = profileMatcher.filterByActiveProfiles(asList(annotatedElement));

    assertEquals(result.size(), 0);
  }

  @Test
  public void missMultiNegative() throws Exception {
    profileMatcher = new ProfileMatcher(asList("prod"));
    AnnotatedElement annotatedElement = createAnnotatedElement(new String[]{"!preprod", "!prod"});

    List<?> result = profileMatcher.filterByActiveProfiles(asList(annotatedElement));

    assertEquals(result.size(), 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsWithBothPositiveAndNegative() throws Exception {
    profileMatcher = new ProfileMatcher(asList("prod"));
    AnnotatedElement annotatedElement = createAnnotatedElement(new String[]{"!preprod", "prod"});

    profileMatcher.filterByActiveProfiles(asList(annotatedElement));
  }

  private AnnotatedElement createAnnotatedElement(String[] t) {
    AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
    when(annotatedElement.isAnnotationPresent(Profile.class)).thenReturn(true);
    Profile profile = mock(Profile.class);
    when(profile.value()).thenReturn(t);
    when(annotatedElement.getAnnotation(Profile.class)).thenReturn(profile);
    return annotatedElement;
  }

}
