package com.github.mongobeej.utils;

import org.springframework.context.annotation.Profile;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.partitioningBy;

public class ProfileMatcher {

	private final List<String> activeProfiles;

	public ProfileMatcher(List<String> activeProfiles) {
		this.activeProfiles = activeProfiles;
	}

	public List<?> filterByActiveProfiles(Collection<? extends AnnotatedElement> annotated) {
		List<AnnotatedElement> filtered = new ArrayList<>();
		for (AnnotatedElement element : annotated) {
			if (matchesActiveSpringProfile(element)) {
				filtered.add(element);
			}
		}
		return filtered;
	}

	private boolean matchesActiveSpringProfile(AnnotatedElement element) {
		if (!ClassUtils.isPresent("org.springframework.context.annotation.Profile", null)) {
			return true;
		}
		if (!element.isAnnotationPresent(Profile.class)) {
			return true; // no-profiled changeset always matches
		}
		List<String> profiles = asList(element.getAnnotation(Profile.class).value());

		Map<Boolean, List<String>> split = profiles.stream()
				.collect(partitioningBy(p -> p.startsWith("!")));
		List<String> positive = split.get(false);
		List<String> negative = split.get(true)
        .stream()
        .map(s -> s.replaceFirst("!", ""))
        .collect(Collectors.toList());


		if (!positive.isEmpty() && !negative.isEmpty()) {
			throw new IllegalArgumentException("Cannot supply both positive and negative profiles");
		}

		if (!negative.isEmpty()) {
			boolean hitsNegative = activeProfiles.stream()
					.anyMatch(ap -> negative.contains(ap));

			return !hitsNegative;
		}

		boolean positiveHit = activeProfiles.stream()
				.anyMatch(ap -> positive.contains(ap));

		return positiveHit;
	}

}
