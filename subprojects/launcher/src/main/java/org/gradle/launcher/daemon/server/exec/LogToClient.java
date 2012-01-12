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
package org.gradle.launcher.daemon.server.exec;

import org.gradle.launcher.daemon.protocol.Build;
import org.gradle.logging.LoggingManagerInternal;
import org.gradle.logging.internal.OutputEvent;
import org.gradle.logging.internal.OutputEventListener;
import org.gradle.logging.internal.OutputEventRenderer;

class LogToClient extends BuildCommandOnly {
    
    private final LoggingManagerInternal loggingManager;
    private final OutputEventRenderer renderer;

    public LogToClient(LoggingManagerInternal loggingManager, OutputEventRenderer renderer) {
        this.loggingManager = loggingManager;
        this.renderer = renderer;
    }
        
    protected void doBuild(final DaemonCommandExecution execution, Build build) {
        OutputEventListener outputForwarder = new OutputEventListener() {
            public void onOutput(OutputEvent event) {
                try {
                    execution.getConnection().dispatch(event);
                } catch (Exception e) {
                    //Ignore. It means the client has disconnected so no point sending him any log output.
                    //we should be checking if client still listens elsewhere anyway.
                }
            }
        };

        loggingManager.setLevel(build.getStartParameter().getLogLevel());
        loggingManager.start();
        loggingManager.addOutputEventListener(outputForwarder);

        //TODO SF this is not beautiful at all.
        //Currently, the DaemonMain replaces the sys out with something that prints to a file (daemon log)
        //sometime later LogToClient kicks in and replaces sys out again (loggingManager.start())
        //it would be nice if logging complexity was in one place and replacing of the sys out happened once, as early as possible.
        //for now, let's just add the renderer carried over here from the DaemonMain
        //it's not nice though, because the build request might have high log level and we won't see much stuff in the daemon log anyway :(
        loggingManager.addOutputEventListener(renderer);

        try {
            execution.proceed();
        } finally {
            loggingManager.removeOutputEventListener(outputForwarder);
            loggingManager.stop();
        }
    } 
}

