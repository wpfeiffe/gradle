/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.util;

import org.gradle.api.Buildable;
import org.gradle.api.Task;
import org.hamcrest.*;
import static org.hamcrest.Matchers.*;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

public class Matchers {
    @Factory
    public static <T> Matcher<T> reflectionEquals(T equalsTo) {
        return new ReflectionEqualsMatcher<T>(equalsTo);
    }

    @Factory
    public static <T, S extends Iterable<? extends T>> Matcher<S> hasSameItems(final S items) {
        return new BaseMatcher<S>() {
            public boolean matches(Object o) {
                Iterable<? extends T> iterable = (Iterable<? extends T>) o;
                List<T> actual = new ArrayList<T>();
                for (T t : iterable) {
                    actual.add(t);
                }
                List<T> expected = new ArrayList<T>();
                for (T t : items) {
                    expected.add(t);
                }

                return expected.equals(actual);
            }

            public void describeTo(Description description) {
                description.appendText("has same items as ").appendValue(items);
            }
        };
    }

    @Factory
    public static <T extends CharSequence> Matcher<T> matchesRegexp(final String pattern) {
        return new BaseMatcher<T>() {
            public boolean matches(Object o) {
                return Pattern.compile(pattern).matcher((CharSequence) o).matches();
            }

            public void describeTo(Description description) {
                description.appendText("matches regexp ").appendValue(pattern);
            }
        };
    }

    @Factory
    public static <T> Matcher<T> strictlyEqual(final T other) {
        return new BaseMatcher<T>() {
            public boolean matches(Object o) {
                if (!o.equals(other)) {
                    return false;
                }
                if (!other.equals(o)) {
                    return false;
                }
                if (!o.equals(o)) {
                    return false;
                }
                if (other.equals(null)) {
                    return false;
                }
                if (other.equals(new Object())) {
                    return false;
                }
                if (o.hashCode() != other.hashCode()) {
                    return false;
                }
                return true;
            }

            public void describeTo(Description description) {
                description.appendText("strictly equals ").appendValue(other);
            }
        };
    }

    @Factory
    public static Matcher<String> containsLine(final String line) {
        return new BaseMatcher<String>() {
            public boolean matches(Object o) {
                return containsLine(equalTo(line)).matches(o);
            }

            public void describeTo(Description description) {
                description.appendText("contains line ").appendValue(line);
            }
        };
    }

    @Factory
    public static Matcher<String> containsLine(final Matcher<? super String> matcher) {
        return new BaseMatcher<String>() {
            public boolean matches(Object o) {
                String str = (String) o;
                BufferedReader reader = new BufferedReader(new StringReader(str));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        if (matcher.matches(line)) {
                            return true;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return false;
            }

            public void describeTo(Description description) {
                description.appendText("contains line that matches ").appendDescriptionOf(matcher);
            }
        };
    }

    @Factory
    public static Matcher<Iterable<?>> isEmpty() {
        return new BaseMatcher<Iterable<?>>() {
            public boolean matches(Object o) {
                Iterable<?> iterable = (Iterable<?>) o;
                return iterable != null && !iterable.iterator().hasNext();
            }

            public void describeTo(Description description) {
                description.appendText("is empty");
            }
        };
    }

    @Factory
    public static Matcher<Map<?, ?>> isEmptyMap() {
        return new BaseMatcher<Map<?, ?>>() {
            public boolean matches(Object o) {
                Map<?, ?> map = (Map<?, ?>) o;
                return map.isEmpty();
            }

            public void describeTo(Description description) {
                description.appendText("is empty");
            }
        };
    }

    @Factory
    public static Matcher<Object[]> isEmptyArray() {
        return new BaseMatcher<Object[]>() {
            public boolean matches(Object o) {
                Object[] array = (Object[]) o;
                return array.length == 0;
            }

            public void describeTo(Description description) {
                description.appendText("is empty");
            }
        };
    }

    @Factory
    public static Matcher<Throwable> hasMessage(final Matcher<String> matcher) {
        return new BaseMatcher<Throwable>() {
            public boolean matches(Object o) {
                Throwable t = (Throwable) o;
                return matcher.matches(t.getMessage());
            }

            public void describeTo(Description description) {
                description.appendText("exception messages ").appendDescriptionOf(matcher);
            }
        };
    }

    @Factory
    public static Matcher<Task> dependsOn(final String... tasks) {
        return dependsOn(equalTo(new HashSet<String>(Arrays.asList(tasks))));
    }
    
    @Factory
    public static Matcher<Task> dependsOn(final Matcher<? extends Iterable<String>> matcher) {
        return new BaseMatcher<Task>() {
            public boolean matches(Object o) {
                Task task = (Task) o;
                Set<String> names = new HashSet<String>();
                Set<? extends Task> depTasks = task.getTaskDependencies().getDependencies(task);
                for (Task depTask : depTasks) {
                    names.add(depTask.getName());
                }
                boolean matches = matcher.matches(names);
                if (!matches) {
                    StringDescription description = new StringDescription();
                    matcher.describeTo(description);
                    System.out.println(String.format("expected %s, got %s.", description.toString(), names));
                }
                return matches;
            }

            public void describeTo(Description description) {
                description.appendText("depends on ").appendDescriptionOf(matcher);
            }
        };
    }

    @Factory
    public static <T extends Buildable> Matcher<T> builtBy(String... tasks) {
        return builtBy(equalTo(new HashSet<String>(Arrays.asList(tasks))));
    }

    @Factory
    public static <T extends Buildable> Matcher<T> builtBy(final Matcher<? extends Iterable<String>> matcher) {
        return new BaseMatcher<T>() {
            public boolean matches(Object o) {
                Buildable task = (Buildable) o;
                Set<String> names = new HashSet<String>();
                Set<? extends Task> depTasks = task.getBuildDependencies().getDependencies(null);
                for (Task depTask : depTasks) {
                    names.add(depTask.getName());
                }
                boolean matches = matcher.matches(names);
                if (!matches) {
                    StringDescription description = new StringDescription();
                    matcher.describeTo(description);
                    System.out.println(String.format("expected %s, got %s.", description.toString(), names));
                }
                return matches;
            }

            public void describeTo(Description description) {
                description.appendText("built by ").appendDescriptionOf(matcher);
            }
        };
    }

    public static <T> Collector<T> collectParam() {
        return new Collector<T>();
    }

    public static class Collector<T> implements Action {
        private T value;

        public T get() {
            return value;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            value = (T) invocation.getParameter(0);
            return null;
        }

        public void describeTo(Description description) {
            description.appendText("collect parameter");
        }
    }
}
