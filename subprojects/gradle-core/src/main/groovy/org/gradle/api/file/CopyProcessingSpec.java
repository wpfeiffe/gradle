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
package org.gradle.api.file;

import groovy.lang.Closure;

import java.util.Map;
import java.io.FilterReader;

public interface CopyProcessingSpec {
    /**
     * Specifies the destination directory for a copy. The destination is evaluated as for {@link
     * org.gradle.api.Project#file(Object)}.
     *
     * @param destPath Path to the destination directory for a Copy
     * @return this
     */
    CopyProcessingSpec into(Object destPath);

    /**
     * Renames a source file to a different relative location under the target directory. The closure will be called
     * with a single parameter, the name of the file.  The closure should return a String object with a new target
     * name. The closure may return null, in which case the original name will be used.
     *
     * @param closure rename closure
     * @return this
     */
    CopyProcessingSpec rename(Closure closure);

    /**
     * Renames files based on a regular expression.  Uses java.util.regex type of regular expressions.  Note that the
     * replace string should use the '$1' syntax to refer to capture groups in the source regular expression.  Files
     * that do not match the source regular expression will be copied with the original name.
     *
     * <p> Example:
     * <pre>
     * rename '(.*)_OEM_BLUE_(.*)', '$1$2'
     * </pre>
     * would map the file 'style_OEM_BLUE_.css' to 'style.css'
     *
     * @param sourceRegEx Source regular expression
     * @param replaceWith Replacement string (use $ syntax for capture groups)
     * @return this
     */
    CopyProcessingSpec rename(String sourceRegEx, String replaceWith);

    /**
     * Adds a content filter to be used during the copy.  Multiple calls to filter, add additional filters to the filter
     * chain.  Each filter should implement java.io.FilterReader. Include org.apache.tools.ant.filters.* for access to
     * all the standard ANT filters. <p> Filter parameters may be specified using groovy map syntax. <p> Examples:
     * <pre>
     *    filter(HeadFilter, lines:25, skip:2)
     *    filter(ReplaceTokens, tokens:[copyright:'2009', version:'2.3.1'])
     * </pre>
     *
     * @param map map of filter parameters
     * @param filterType Class of filter to add
     * @return this
     */
    CopyProcessingSpec filter(Map<String, Object> map, Class<FilterReader> filterType);

    /**
     * Adds a content filter to be used during the copy.  Multiple calls to filter, add additional filters to the filter
     * chain.  Each filter should implement java.io.FilterReader. Include org.apache.tools.ant.filters.* for access to
     * all the standard ANT filters. <p> Examples:
     * <pre>
     *    filter(StripJavaComments)
     *    filter(com.mycompany.project.CustomFilter)
     * </pre>
     *
     * @param filterType Class of filter to add
     * @return this
     */
    CopyProcessingSpec filter(Class<FilterReader> filterType);

    /**
     * Adds a content filter based on the provided closure.  The Closure will be called with each line (stripped of line
     * endings) and should return a String to replace the line.
     *
     * @param closure to implement line based filtering
     * @return this
     */
    CopyProcessingSpec filter(Closure closure);
}
