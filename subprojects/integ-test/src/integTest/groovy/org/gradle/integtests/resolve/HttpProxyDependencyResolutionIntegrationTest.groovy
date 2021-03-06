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
package org.gradle.integtests.resolve

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.HttpServer
import org.gradle.integtests.fixtures.IvyRepository
import org.gradle.util.SetSystemProperties
import org.junit.Rule

import org.gradle.integtests.fixtures.TestProxyServer

class HttpProxyDependencyResolutionIntegrationTest extends AbstractIntegrationSpec {
    @Rule
    public final HttpServer server = new HttpServer()
    @Rule
    public final TestProxyServer proxyServer = new TestProxyServer(server)
    @Rule
    public SetSystemProperties systemProperties = new SetSystemProperties()

    def "setup"() {
        requireOwnUserHomeDir()
    }

    public void "uses configured proxy to access remote HTTP repository"() {
        server.start()
        proxyServer.start()

        given:
        def repo = ivyRepo()
        def module = repo.module('group', 'projectA', '1.2')
        module.publish()

        and:
        buildFile << """
repositories {
    ivy { url "http://not.a.real.domain/repo" }
}
configurations { compile }
dependencies { compile 'group:projectA:1.2' }
task listJars << {
    assert configurations.compile.collect { it.name } == ['projectA-1.2.jar']
}
"""

        when:
        executer.withArguments("-Dhttp.proxyHost=localhost", "-Dhttp.proxyPort=${proxyServer.port}")

        and:
        server.expectGet('/repo/group/projectA/1.2/ivy-1.2.xml', module.ivyFile)
        server.expectGet('/repo/group/projectA/1.2/projectA-1.2.jar', module.jarFile)

        then:
        succeeds('listJars')

        and:
        proxyServer.requestCount == 2
    }

    public void "uses authenticated proxy to access remote HTTP repository"() {
        server.start()
        proxyServer.start()

        given:
        def repo = ivyRepo()
        def module = repo.module('group', 'projectA', '1.2')
        module.publish()

        and:
        buildFile << """
repositories {
    ivy {
        url "http://not.a.real.domain/repo"
    }
}
configurations { compile }
dependencies { compile 'group:projectA:1.2' }
task listJars << {
    assert configurations.compile.collect { it.name } == ['projectA-1.2.jar']
}
"""

        when:
        executer.withArguments("-Dhttp.proxyHost=localhost", "-Dhttp.proxyPort=${proxyServer.port}", "-Dhttp.nonProxyHosts=foo",
                               "-Dhttp.proxyUser=proxyUser", "-Dhttp.proxyPassword=proxyPassword")

        and:
        proxyServer.requireAuthentication('proxyUser', 'proxyPassword')

        and:
        server.expectGet('/repo/group/projectA/1.2/ivy-1.2.xml', module.ivyFile)
        server.expectGet('/repo/group/projectA/1.2/projectA-1.2.jar', module.jarFile)

        then:
        succeeds('listJars')

        and:
        proxyServer.requestCount == 2
    }

    public void "passes target credentials to target server via proxy"() {
        server.start()
        proxyServer.start()

        given:
        def repo = ivyRepo()
        def module = repo.module('group', 'projectA', '1.2')
        module.publish()

        and:
        buildFile << """
repositories {
    ivy {
        url "http://not.a.real.domain/repo"
        credentials {
            username 'targetUser'
            password 'targetPassword'
        }
    }
}
configurations { compile }
dependencies { compile 'group:projectA:1.2' }
task listJars << {
    assert configurations.compile.collect { it.name } == ['projectA-1.2.jar']
}
"""

        when:
        executer.withArguments("-Dhttp.proxyHost=localhost", "-Dhttp.proxyPort=${proxyServer.port}", "-Dhttp.proxyUser=proxyUser", "-Dhttp.proxyPassword=proxyPassword")

        and:
        proxyServer.requireAuthentication('proxyUser', 'proxyPassword')

        and:
        server.expectGet('/repo/group/projectA/1.2/ivy-1.2.xml', 'targetUser', 'targetPassword', module.ivyFile)
        server.expectGet('/repo/group/projectA/1.2/projectA-1.2.jar', 'targetUser', 'targetPassword', module.jarFile)

        then:
        executer.withDeprecationChecksDisabled()
        succeeds('listJars')

        and:
        proxyServer.requestCount == 2
    }

    IvyRepository ivyRepo() {
        return new IvyRepository(file('ivy-repo'))
    }
}
