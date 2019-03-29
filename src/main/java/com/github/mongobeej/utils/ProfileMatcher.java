package com.github.mongobeej.utils;

import org.springframework.context.annotation.Profile;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

final class ProfileMatcher {

    private final List<String> activeProfiles;

    ProfileMatcher(List<String> activeProfiles) {
        this.activeProfiles = requireNonNull(activeProfiles);
    }

    List<?> filterByActiveProfiles(Collection<? extends AnnotatedElement> annotated) {
        return annotated.stream()
                .filter(this::matchesActiveSpringProfile)
                .collect(toList());
    }

    private boolean matchesActiveSpringProfile(AnnotatedElement element) {
        if (!ClassUtils.isPresent("org.springframework.context.annotation.Profile", null)) {
            return true;
        }
        if (!isAnnotatedWithProfile(element)) {
            return true;
        }
        List<String> profiles = asList(element.getAnnotation(Profile.class).value());
        Map<Boolean, List<String>> groupedProfiles = profiles.stream()
                .collect(partitioningBy(profile -> profile.startsWith("!")));
        List<String> includedProfiles = groupedProfiles.get(false);
        List<String> excludedProfiles = trimLeadingNegation(groupedProfiles);
        if (!includedProfiles.isEmpty() && !excludedProfiles.isEmpty()) {
            throw new IllegalArgumentException("Can not supply both positive and negative profiles");
        }
        if (!excludedProfiles.isEmpty()) {
            return activeProfiles.stream()
                    .noneMatch(excludedProfiles::contains);
        }
        return activeProfiles.stream()
                .anyMatch(includedProfiles::contains);
    }

    private boolean isAnnotatedWithProfile(AnnotatedElement annotatedElement) {
        return annotatedElement.isAnnotationPresent(Profile.class);
    }

    private List<String> trimLeadingNegation(Map<Boolean, List<String>> groupedProfiles) {
        return groupedProfiles.get(true)
                .stream()
                .map(profile -> profile.replaceFirst("!", ""))
                .collect(toList());
    }
}
