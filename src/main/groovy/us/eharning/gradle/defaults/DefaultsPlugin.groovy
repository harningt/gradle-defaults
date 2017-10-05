/*
 * Copyright 2014-2016 Andrew Oberstar
 * Copyright 2017 Thomas Harning Jr. <harningt@gmail.com>
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
package us.eharning.gradle.defaults

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

class DefaultsPlugin implements Plugin<Project> {

    void apply(Project project) {
        if (project.rootProject != project) {
            throw new GradleException('us.eharning.gradle.defaults must only be applied to the root project')
        }

        DefaultsExtension extension = project.extensions.create('defaults', DefaultsExtension, project)
        def globalOps = new GlobalOperations(project, extension)
        globalOps.addGit()
        globalOps.addReleaseConfig()
        globalOps.addVersioneye()

        project.allprojects { prj ->
            def localOps = new LocalOperations(prj, extension)
            localOps.addCheckstyle()
            localOps.addFindbugs()
            localOps.addLicense()
            localOps.addSpotless()
            localOps.addJavaConfig()
            localOps.addGroovyConfig()
            localOps.addPublishingConfig()
            localOps.addPluginConfig()
            localOps.addOrderingRules()
        }
    }

    private class GlobalOperations {
        private final DefaultsExtension extension
        private final Project project

        GlobalOperations(Project project, DefaultsExtension extension) {
            this.project = project
            this.extension = extension
        }

        void addGit() {
            project.plugins.apply('org.ajoberstar.grgit')
            project.plugins.apply('org.ajoberstar.git-publish')

            def addOutput = { task ->
                project.gitPublish.contents.from(task.outputs.files) {
                    into "docs${task.path}".replace(':', '/')
                }
            }

            project.allprojects { prj ->
                prj.plugins.withId('java') { addOutput(prj.javadoc) }
                prj.plugins.withId('groovy') { addOutput(prj.groovydoc) }
                /* Unknown if kotlin needs its own separate 'doc' output */
            }

            project.gitPublish {
                branch = 'gh-pages'
            }
        }

        void addReleaseConfig() {
            project.plugins.apply('org.ajoberstar.reckon')

            project.reckon {
                normal = scopeFromProp()
                preRelease = stageFromProp('milestone', 'rc', 'final')
            }

            def releaseTask = project.tasks.create('release')
            releaseTask.dependsOn 'reckonTagPush'
            releaseTask.dependsOn 'gitPublishPush'
            project.allprojects { prj ->
                prj.plugins.withId('org.gradle.base') {
                    releaseTask.dependsOn prj.clean, prj.build
                }
                prj.plugins.withId('maven-publish') {
                    releaseTask.dependsOn prj.publish
                }
                prj.plugins.withId('com.gradle.plugin-publish') {
                    releaseTask.dependsOn prj.publishPlugins
                }
            }
        }

        void addVersioneye() {
            project.plugins.apply('org.standardout.versioneye')

            project.afterEvaluate {
                project.versioneye {
                    includePlugins = false
                    /* Workaround for Gradle 4 issue */
                    exclude project.configurations.findAll { !it.canBeResolved }*.name as String[]
                }
            }
        }
    }

    private class LocalOperations {
        private final Project project
        private final DefaultsExtension extension

        LocalOperations(Project project, DefaultsExtension extension) {
            this.project = project
            this.extension = extension
        }

        private void addCheckstyle() {
            project.plugins.withId('java') {
                project.plugins.apply('checkstyle')
                project.checkstyle {
                    toolVersion = '8.0'
                    config = project.resources.text.fromFile(new File(project.rootDir, '/gradle/checkstyle/checkstyle.xml'))
                }
            }
        }

        private void addFindbugs() {
            project.plugins.withId('java') {
                project.plugins.apply('findbugs')
                project.findbugs {
                    sourceSets = [sourceSets.main]
                    excludeFilter project.rootProject.file('gradle/findbugs-exclude.xml')
                }
            }
        }

        private void addLicense() {
            project.plugins.apply('com.github.hierynomus.license')
            project.afterEvaluate {
                project.license {
                    header = project.rootProject.file('gradle/HEADER')
                    strictCheck = true
                    /* So that year-check doesn't muck around */
                    skipExistingHeaders = true
                    useDefaultMappings = false
                    mapping 'groovy', 'SLASHSTAR_STYLE'
                    mapping 'java', 'SLASHSTAR_STYLE'
                    mapping 'kt', 'SLASHSTAR_STYLE'
                    ext.year = Calendar.getInstance().get(Calendar.YEAR)
                    /* Only include those which licensing is setup
                     * excludes for known data files seems not to work
                     * */
                    includes(['**/*.java', '**/*.groovy', '**/*.kt'])
                }
            }
        }

        private void addSpotless() {
            project.plugins.apply('com.diffplug.gradle.spotless')
            /* License management handled by a separate extension due to dates/etc */
            project.spotless {
                project.plugins.withId('java') {
                    java {
                        trimTrailingWhitespace()
                        indentWithSpaces(4)
                        endWithNewline()
                    }
                }
                project.plugins.withId('groovy') {
                    format 'groovy', {
                        target 'src/**/*.groovy'
                        trimTrailingWhitespace()
                        indentWithSpaces(4)
                        endWithNewline()
                    }
                }
                project.plugins.withId('kotlin') {
                    format 'kotlin', {
                        target 'src/**/*.kt'
                        trimTrailingWhitespace()
                        indentWithSpaces(4)
                        endWithNewline()
                    }
                }
                format 'gradle', {
                    target '**/build.gradle'
                    trimTrailingWhitespace()
                    indentWithSpaces(4)
                    endWithNewline()
                }
            }
        }

        private void addJavaConfig() {
            /* Unknown if kotlin needs its own separate setup like this
             * ... currently system piggybacks off of java being included also */
            project.plugins.withId('java') {
                project.plugins.apply('jacoco')
                project.jacoco {
                    toolVersion = '0.7.9'
                }
                project.jacocoTestReport {
                    reports {
                        xml.enabled true
                        html.destination new File(project.buildDir, "jacocoHtml")
                    }
                }


                Task sourcesJar = project.tasks.create('sourcesJar', Jar)
                sourcesJar.with {
                    classifier = 'sources'
                    from project.sourceSets.main.allSource
                }

                Task javadocJar = project.tasks.create('javadocJar', Jar)
                javadocJar.with {
                    classifier = 'javadoc'
                    from project.tasks.javadoc.outputs.files
                }
            }
        }

        private void addGroovyConfig() {
            project.plugins.withId('groovy') {
                project.afterEvaluate {
                    if (!extension.includeGroovy) {
                        return
                    }
                    Task groovydocJar = project.tasks.create('groovydocJar', Jar)
                    groovydocJar.with {
                        classifier = 'groovydoc'
                        from project.tasks.groovydoc.outputs.files
                    }
                }
            }
        }

        private void addPublishingConfig() {
            project.plugins.withId('java') {
                project.plugins.apply('maven-publish')

                project.afterEvaluate {
                    project.publishing {
                        publications {
                            main(MavenPublication) {
                                from project.components.java
                                artifact project.sourcesJar
                                artifact project.javadocJar

                                if (extension.includeGroovy) {
                                    project.plugins.withId('groovy') {
                                        artifact project.groovydocJar
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private void addPluginConfig() {
            project.plugins.withId('java-gradle-plugin') {
                project.plugins.apply('org.ajoberstar.stutter')
                project.plugins.apply('com.gradle.plugin-publish')

                // remove duplicate publication
                project.gradlePlugin.automatedPublishing = false

                // avoid conflict with localGroovy()
                project.configurations.all {
                    exclude group: 'org.codehaus.groovy'
                }
            }
        }

        private void addOrderingRules() {
            project.plugins.withId('org.gradle.base') {
                def clean = project.tasks['clean']
                project.tasks.all { task ->
                    if (task != clean) {
                        task.shouldRunAfter clean
                    }
                }

                def build = project.tasks['build']
                project.tasks.all { task ->
                    if (task.group == 'publishing') {
                        task.shouldRunAfter build
                    }
                }
            }
        }
    }
}
