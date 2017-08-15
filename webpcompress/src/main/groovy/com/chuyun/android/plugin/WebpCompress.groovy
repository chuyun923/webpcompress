package com.chuyun.android.plugin

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

public class WebpCompress implements Plugin<Project> {

    Extension mExtension;
    static String separator = File.separatorChar;

    @Override
    void apply(Project project) {
        DomainObjectCollection<BaseVariant> variants = GradleUtils.getAndroidVariants(project);
        mExtension = project.extensions.create("webpCompress", Extension.class);

        project.afterEvaluate {

            variants.all { variant ->

                def buildType = variant.getVariantData().getVariantConfiguration().getBuildType().name

                if (mExtension.skipDebug && "${buildType}".toLowerCase().contains("debug")) {
                    log "skip webp compress in DEBUG build!"
                    return
                }

                def dx = project.tasks.findByName("process${variant.name.capitalize()}Resources")
                def webpCompressName = "webpCompress${variant.name.capitalize()}"

                project.task(webpCompressName) << {

                    def outputfile = new File("${project.buildDir}${File.separator}outputs${File.separator}webpcompressoutput.txt")
                    if (outputfile.exists()) {
                        outputfile.delete()
                    }

                    outputfile.createNewFile();
                    Set<String> whiteList = getWhiteList(project, mExtension);

                    dx.inputs.files.files.each { file ->
                        if (!isCompressAblePicture(file)) return

                        String name = file.name;

                        if (whiteList.contains(name)) {
                            log "${name} is skiped because of whitelist!"
                            return
                        }

                        if (mExtension.filterAlpha && hasAlpha(file)) {
                            log "${name} is skiped because of has ALPHA!"
                            return
                        }

                        String absolutePath = file.absolutePath;
                        String picName = name.split('\\.')[0]
                        String dirName = absolutePath.substring(0, absolutePath.lastIndexOf(separator))

                        executeSync "${mExtension.cwebpPath} -q ${mExtension.q} -m 6 ${absolutePath} -o ${dirName}/${picName}.webp"
                        File webpFile = new File("${dirName}/${picName}.webp");
                        if(file.size() <= webpFile.size()) {
                            log "${absolutePath} png is smaller than webp!"
                            executeSync "rm ${dirName}/${picName}.webp"
                            return
                        }else {
                            executeSync "rm ${absolutePath}"
                        }

                        def picWriter = new FileWriter(outputfile, true);
                        picWriter.write("${absolutePath}-->${picName}.webp\n");
                        picWriter.flush()
                        picWriter.close()
                    }
                }

                project.tasks.findByName(webpCompressName).dependsOn dx.taskDependencies.getDependencies(dx)
                dx.dependsOn project.tasks.findByName(webpCompressName)
            }

        }
    }

    private void log(String s) {
        if (mExtension.openLog) {
            println s;
        }
    }

    private void executeSync(String cmd) {
        Process process = cmd.execute();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while (br.readLine() != null);

        BufferedReader stdinReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (stdinReader.readLine() != null);
        process.waitFor();
    }

    private static Set<String> getWhiteList(Project project, Extension extension) {
        Set<String> result = new HashSet<>();
        def f = new File("${project.projectDir}/webp_white_list.txt");

        if (f.exists()) {
            f.eachLine { name ->
                result.add(name);
            }
        }

        if (extension.whiteList != null) {
            extension.whiteList.each { name ->
                result.add(name);
            }
        }
        return result;
    }

    private static boolean isCompressAblePicture(File file) {
        return !file.name.contains(".9") && (file.name.endsWith(".jpg") || file.name.endsWith(".png"));
    }

    private static boolean hasAlpha(File file) {
        if (!file.name.endsWith(".png")) {
            return false;
        }

        BufferedImage img = ImageIO.read(file);
        return img.getColorModel().hasAlpha();
    }

}

class Extension {
    //压缩比例
    int q = 80;
    //是否过滤掉ALPHA通道的图片,Android >= 4.3 ，可以关闭这个选项
    boolean filterAlpha = true;
    boolean openLog = true;
    boolean skipDebug = true;
    String cwebpPath = "cwebp";

    List<String> whiteList;

    public static Extension getConfig(Project project) {
        Extension extension = project.getExtensions().findByType(Extension.class);
        if (extension == null) {
            extension = new Extension();
        }
        return extension;
    }
}