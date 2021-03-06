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
package org.gradle.api.internal.file;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.GradleException;
import org.gradle.api.file.CopyAction;
import org.gradle.api.file.FileVisitDetails;

import java.io.IOException;
import java.io.File;

public class ZipCopyVisitor implements CopySpecVisitor {
    private ZipArchiveOutputStream zipOutStr;
    private File zipFile;

    public void startVisit(CopyAction action) {
        ArchiveCopyAction archiveAction = (ArchiveCopyAction) action;
        zipFile = archiveAction.getArchivePath();
        try {
            zipOutStr = new ZipArchiveOutputStream(zipFile);
        } catch (Exception e) {
            throw new GradleException(String.format("Could not create ZIP '%s'.", zipFile), e);
        }
    }

    public void endVisit() {
        try {
            zipOutStr.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSpec(CopySpecImpl spec) {
    }

    public void visitFile(FileVisitDetails fileDetails) {
        try {
            ZipArchiveEntry archiveEntry = new ZipArchiveEntry(fileDetails.getRelativePath().getPathString());
            archiveEntry.setMethod(ZipArchiveEntry.DEFLATED);
            archiveEntry.setTime(fileDetails.getLastModified());
            zipOutStr.putArchiveEntry(archiveEntry);
            fileDetails.copyTo(zipOutStr);
            zipOutStr.closeArchiveEntry();
        } catch (Exception e) {
            throw new GradleException(String.format("Could not add %s to ZIP '%s'.", fileDetails, zipFile), e);
        }
    }

    public void visitDir(FileVisitDetails dirDetails) {
    }

    public boolean getDidWork() {
        return true;
    }
}
