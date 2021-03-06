/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.internal.nativeplatform;

public abstract class FileSystems {
    /**
     * Returns the default file system. The default file system is defined as the file system
     * that holds the <tt>java.io.tmpdir</tt> directory.
     *
     * @return the default file system
     */
    public static FileSystem getDefault() {
        return OperatingSystem.current().isWindows()
            ? DefaultWindowsFileSystem.INSTANCE
            : DefaultFileSystem.INSTANCE;
    }
    
    private static class DefaultFileSystem {
        static final FileSystem INSTANCE = new GenericFileSystem();    
    }

    private static final class DefaultWindowsFileSystem {
        static final FileSystem INSTANCE = new WindowsFileSystem();
    }
}
