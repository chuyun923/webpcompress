package com.maoyan.android.gradle.plugin

import com.android.SdkConstants
import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.AndroidProject
import com.google.common.collect.Sets
import org.apache.commons.io.FileUtils
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

import java.lang.reflect.Field

class GradleUtils {
    static
    final String sPluginMisConfiguredErrorMessage = "Plugin requires the 'android' or 'android-library' plugin to be configured.";

    /**
     * get android variant list of the project
     * @param project the compiling project
     * @return android variants
     */
    public static DomainObjectCollection<BaseVariant> getAndroidVariants(Project project) {
        if (project.getPlugins().hasPlugin(AppPlugin)) {
            return (DomainObjectCollection<BaseVariant>) ((AppExtension) ((AppPlugin) project.getPlugins().getPlugin(AppPlugin)).extension).applicationVariants;
        } else if (project.getPlugins().hasPlugin(LibraryPlugin)) {
            return (DomainObjectCollection<BaseVariant>) ((LibraryExtension) ((LibraryPlugin) project.getPlugins().getPlugin(LibraryPlugin)).extension).libraryVariants;
        } else {
            throw new ProjectConfigurationException(sPluginMisConfiguredErrorMessage, null)
        }
    }

    /**
     * get android variant data list of the project
     * @param project the project
     * @return android variant data list
     */
    public static List<BaseVariantData> getAndroidVariantDataList(Project project) {
        if (project.getPlugins().hasPlugin(AppPlugin)) {
            return project.getPlugins().getPlugin(AppPlugin).variantManager.getVariantDataList();
        } else if (project.getPlugins().hasPlugin(LibraryPlugin)) {
            return project.getPlugins().getPlugin(LibraryPlugin).variantManager.getVariantDataList();
        } else {
            throw new ProjectConfigurationException(sPluginMisConfiguredErrorMessage, null)
        }
    }

    public static BaseExtension getAndroidExtension(Project project) {
        if (project.getPlugins().hasPlugin(AppPlugin)) {
            return project.getPlugins().getPlugin(AppPlugin).extension
        } else if (project.getPlugins().hasPlugin(LibraryPlugin)) {
            return project.getPlugins().getPlugin(LibraryPlugin).extension
        } else {
            throw new ProjectConfigurationException(sPluginMisConfiguredErrorMessage, null)
        }
    }

    static def hasAndroidPlugin(Project project) {
        return project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(LibraryPlugin)
    }

    static def isOfflineBuild(Project project) {
        return project.getGradle().getStartParameter().isOffline()
    }

    static String getResourcesFolder(Project project, BaseVariant variant) {
        if (isGradle140orAbove(project)) {
            return String.format("${project.buildDir}${File.separator}${AndroidProject.FD_INTERMEDIATES}${File.separator}res${File.separator}merged${File.separator}${variant.dirName}")
        } else {
            return String.format("${project.buildDir}${File.separator}${AndroidProject.FD_INTERMEDIATES}${File.separator}res${File.separator}${variant.dirName}")
        }
    }

    static String getAssetsFolder(Project project, BaseVariant variant) {
        return String.format("${project.buildDir}${File.separator}${AndroidProject.FD_INTERMEDIATES}${File.separator}assets${File.separator}${variant.dirName}")
    }

    static String getProcessManifestTaskName(Project project, BaseVariant variant) {
        return "process${variant.name.capitalize()}Manifest"
    }

    static String getJacocoInstrumentTaskName(Project project, BaseVariant variant) {
        if (isGradle140orAbove(project)) {
            return "transformClassesWithJacocoFor${variant.name.capitalize()}"
        } else {
            return "Instrument${variant.name.capitalize()}"
        }
    }

    static String getJavaCompileTaskName(Project project, BaseVariant variant) {
        if (isGradle140orAbove(project)) {
            return "compile${variant.name.capitalize()}JavaWithJavac"
        } else {
            return variant.javaCompile.nameg
        }
    }

    static String getProGuardTaskName(Project project, BaseVariant variant) {
        if (isGradle140orAbove(project)) {
            return "transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}"
        } else {
            return "proguard${variant.name.capitalize()}"
        }
    }

    static String getPreDexTaskName(Project project, BaseVariant variant) {
        if (isGradle140orAbove(project)) {
//            throws new GradleException("not set pre dex task name");
            return ""
        } else {
            return "preDex${variant.name.capitalize()}"
        }
    }

    static String getDexTaskName(Project project, BaseVariant variant) {
        if (isGradle140orAbove(project)) {
            return "transformClassesWithDexFor${variant.name.capitalize()}"
        } else {
            return "dex${variant.name.capitalize()}"
        }
    }

    static String getJarMergingTaskName(Project project, BaseVariant variant) {
        if (isGradle140orAbove(project)) {
            return "transformClassesWithJarMergingFor${variant.name.capitalize()}"
        } else {
            return ""
        }
    }

    static String getProGuardTaskOutputFolder(Project project, BaseVariant variant, Task dexTask) {
        if (dexTask == null) {
            dexTask = project.tasks.findByName(getDexTaskName(project, variant));
        }

        if (isGradle140orAbove(project)) {
            def proGuardFolder = ''
            Set<File> files = Sets.newHashSet();
            dexTask.inputs.files.files.each {
                def extensions = [SdkConstants.EXT_JAR] as String[]
                if (it.exists()) {
                    if (it.isDirectory()) {
                        Collection<File> jars = FileUtils.listFiles(it, extensions, true);
                        files.addAll(jars)
                    } else if (it.name.endsWith(SdkConstants.DOT_JAR)) {
                        files.add(it)
                    }
                }
            }
            files.each {
                if (!it.path.contains("main-dex") && !it.path.contains("secondary-dex") && !it.path.contains("inline-dex") && (it.name.equals("main.jar") || it.name.equals("classes.jar"))) {
                    proGuardFolder = it.parent
                }
            }

            if (proGuardFolder.isEmpty()) {
                throw new GradleException("Fail to get ProGuard output folder")
            } else {
                return proGuardFolder
            }
        } else {
            return "${project.buildDir}${File.separator}${AndroidProject.FD_INTERMEDIATES}${File.separator}classes-proguard${File.separator}${variant.dirName}${File.separator}"
        }
    }

    static File getJarMergingOutputJar(Project project, BaseVariant variant, Task dexTask) {
        if (isGradle140orAbove(project)) {
            File proGuardOutputJar = null
            Set<File> files = Sets.newHashSet();

            def jarMergingFolder = new File("${project.buildDir}${File.separator}${AndroidProject.FD_INTERMEDIATES}${File.separator}transforms${File.separator}jarMerging${File.separator}${variant.dirName}")
            if (jarMergingFolder.exists() && jarMergingFolder.isDirectory()) {
                def extensions = [SdkConstants.EXT_JAR] as String[]
                Collection<File> jars = FileUtils.listFiles(jarMergingFolder, extensions, true);
                files.addAll(jars)

                files.each {
                    if (!it.path.contains("main-dex") && !it.path.contains("secondary-dex") && !it.path.contains("inline-dex") && (it.name.equals("main.jar") || it.name.equals("classes.jar") || it.name.equals("combined.jar"))) {
                        proGuardOutputJar = it
                    }
                }

                if (proGuardOutputJar == null) {
                    throw new GradleException("Fail to get JarMerging output folder")
                } else {
                    return proGuardOutputJar
                }
            }
            return null
        } else {
            return new File("${project.buildDir}${File.separator}${AndroidProject.FD_INTERMEDIATES}${File.separator}multi-dex${File.separator}${variant.dirName}${File.separator}allclasses.jar")
        }
    }

    static File getProGuardTaskOutputJar(Project project, BaseVariant variant, Task dexTask) {
        if (dexTask == null) {
            dexTask = project.tasks.findByName(getDexTaskName(project, variant));
        }

        if (isGradle140orAbove(project)) {
            File proGuardOutputJar = null
            Set<File> files = Sets.newHashSet();

            dexTask.inputs.files.files.each {
                def extensions = [SdkConstants.EXT_JAR] as String[]
                if (it.exists()) {
                    if (it.isDirectory()) {
                        Collection<File> jars = FileUtils.listFiles(it, extensions, true);
                        files.addAll(jars)
                    } else if (it.name.endsWith(SdkConstants.DOT_JAR)) {
                        files.add(it)
                    }
                }
            }

            files.each {
                if (!it.path.contains("main-dex") && !it.path.contains("secondary-dex") && !it.path.contains("inline-dex") && (it.name.equals("main.jar") || it.name.equals("classes.jar"))) {
                    proGuardOutputJar = it
                }
            }

            if (proGuardOutputJar == null) {
                throw new GradleException("Fail to get ProGuard output folder")
            } else {
                return proGuardOutputJar
            }
        } else {
            return new File("${project.buildDir}${File.separator}${AndroidProject.FD_INTERMEDIATES}${File.separator}classes-proguard${File.separator}${variant.dirName}${File.separator}classes.jar")
        }
    }

    static Set<File> getJavaCompileTasOutput(Project project, BaseVariant variant) {
       return project.tasks.findByName(getJavaCompileTaskName(project,variant)).outputs.files.files
    }


    static Set<File> getDexTaskInputFiles(Project project, BaseVariant variant, Task dexTask) {
        if (dexTask == null) {
            dexTask = project.tasks.findByName(getDexTaskName(project, variant));
        }

        if (isGradle140orAbove(project)) {
            Set<File> files = Sets.newHashSet();
            dexTask.inputs.files.files.each {
                def extensions = [SdkConstants.EXT_JAR] as String[]
                if (it.exists()) {
                    if (it.isDirectory()) {
                        Collection<File> jars = FileUtils.listFiles(it, extensions, true);
                        files.addAll(jars)
                    } else if (it.name.endsWith(SdkConstants.DOT_JAR)) {
                        files.add(it)
                    }
                }
            }
            return files
        } else {
            return dexTask.inputs.files.files;
        }
    }

    static String getDexTaskOutputFolder(Project project, BaseVariant variant, Task dexTask) {
        if (dexTask == null) {
            dexTask = project.tasks.findByName(getDexTaskName(project, variant));
        }

        if (isGradle140orAbove(project)) {
            String dexOutputFolder = ""
            dexTask.outputs.files.files.each {
                def extensions = [SdkConstants.EXT_DEX] as String[]
                if (it.exists()) {
                    if (it.isDirectory()) {
                        Collection<File> dexs = FileUtils.listFiles(it, extensions, true);
                        dexs.each { dex ->
                            if (dex.name.startsWith("classes") && dex.name.endsWith(SdkConstants.DOT_DEX)) {
                                dexOutputFolder = dex.parent
                            }
                        }
                    } else if (it.name.startsWith("classes") && it.name.endsWith(SdkConstants.DOT_DEX)) {
                        dexOutputFolder = it.parent
                    }
                }

            }
            if (dexOutputFolder.isEmpty()) {
                throw new GradleException("Fail to get DexTask output folder")
            }
            return dexOutputFolder
        } else {
            return "${project.buildDir.absolutePath}${File.separator}${AndroidProject.FD_INTERMEDIATES}${File.separator}dex${File.separator}${variant.dirName}"
        }
    }

//    static Set<File> walkFolderForDexInputs(File directoryFile) {
//        Set<File> files = Sets.newHashSet();
//        File[] children = directoryFile.listFiles();
//        if (children == null) {
//            return files;
//        }
//
//        for (final File file : children) {
//            if (file.isDirectory()) {
//                files.addAll(walkFolderForDexInputs(file));
//            } else if (file.isFile() && file.name.equals("classes.jar")) {
//                files.add(file);
//            }
//        }
//        return files;
//    }

//    public static void withStyledOutput(
//            AbstractTask task,
//            StyledTextOutput.Style style,
//            LogLevel level = null,
//            @ClosureParams(value = SimpleType, options = ['org.gradle.logging.StyledTextOutput']) Closure closure) {
//        def factory = task.services.get(StyledTextOutputFactory)
//        def output = level == null ? factory.create('dexcount') : factory.create('dexcount', level)
//
//        closure(output.withStyle(style))
//    }

    public static boolean isGradle140orAbove(Project project) {
        Class<?> versionClazz = null
        try {
            versionClazz = Class.forName("com.android.builder.Version")
        } catch (Exception e) {
        }
        if (versionClazz == null) {
            return false
        } else {
            Field pluginVersionField = versionClazz.getField("ANDROID_GRADLE_PLUGIN_VERSION")
            pluginVersionField.setAccessible(true)
            String version = pluginVersionField.get(null)

            return versionCompare(version, "1.4.0") >= 0;
        }
        //return versionCompare(com.android.builder.Version.ANDROID_GRADLE_PLUGIN_VERSION, "1.4.0") >= 0;
    }

    /**
     * Compares two version strings.
     *
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     *         The result is a positive integer if str1 is _numerically_ greater than str2.
     *         The result is zero if the strings are _numerically_ equal.
     */
    private static int versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("-")[0].split("\\.");
        String[] vals2 = str2.split("-")[0].split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }

        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }

        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(vals1.length - vals2.length);
        }
    }


    static def findDependenciesWithGroup(Project project, String group) {
        def deps = []
        for (Configuration configuration : project.configurations) {
            for (Dependency dependency : configuration.dependencies) {
                if (group.equals(dependency.group)) {
                    deps.add dependency
                }
            }
        }
        return deps
    }

    static def findDependenciesStartingWith(Project project, String prefix) {
        def deps = []
        for (Configuration configuration : project.configurations) {
            for (Dependency dependency : configuration.dependencies) {
                if (dependency.group != null && dependency.group.startsWith(prefix)) {
                    deps.add dependency
                }
            }
        }
        return deps
    }

    static String getPluginVersion(Project project, String group, String name) {
        def Project targetProject = project
        while (targetProject != null) {
            def version
            targetProject.buildscript.configurations.classpath.resolvedConfiguration.firstLevelModuleDependencies.each {
                e ->
                    if (e.moduleGroup.equals(group) && e.moduleName.equals(name)) {
                        version = e.moduleVersion
                    }
            }
            if (version != null) {
                return version
            }
            targetProject = targetProject.parent
        }
        return null
    }
}
