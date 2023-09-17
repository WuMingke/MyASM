package com.example.buildsrc;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.gradle.internal.impldep.org.apache.commons.codec.digest.DigestUtils;
import org.gradle.internal.impldep.org.apache.commons.io.IOUtils;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * 处理模版
 */
public class TransformTemplate extends Transform {
    @Override
    public String getName() {
        return "transform_template";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        System.out.println("=== transform_template start ===");
        if (!transformInvocation.isIncremental()) {
            transformInvocation.getOutputProvider().deleteAll();
        }
        // 获取需要处理的数据
        Collection<TransformInput> inputCollection = transformInvocation.getInputs();
        // 遍历
        inputCollection.parallelStream().forEach(transformInput -> {
            // 1 Jar包
            Collection<JarInput> jarInputCollection = transformInput.getJarInputs();
            jarInputCollection.parallelStream().forEach(jarInput -> {
                processJarFile(jarInput, transformInvocation);
            });

            // 2 directory
            Collection<DirectoryInput> directoryInputCollection = transformInput.getDirectoryInputs();
            directoryInputCollection.parallelStream().forEach(directoryInput -> {
                processDirectoryFile(directoryInput, transformInvocation);
            });
        });
    }

    /**
     * 处理directory
     *
     * @param directoryInput
     * @param transformInvocation
     */
    private void processDirectoryFile(DirectoryInput directoryInput, TransformInvocation transformInvocation) {
        File srcDir = directoryInput.getFile();
        File outputDir = transformInvocation.getOutputProvider()
                .getContentLocation(
                        srcDir.getAbsolutePath(),
                        directoryInput.getContentTypes(),
                        directoryInput.getScopes(),
                        Format.DIRECTORY);
        try {
            // outputFile不存在，创建
            FileUtils.forceMkdir(outputDir);
            if (transformInvocation.isIncremental()) {
                Map<File, Status> changedFilesMap = directoryInput.getChangedFiles();
                changedFilesMap.forEach((file, status) -> {
                    // 获取变动的文件对应在output directory中的位置
                    String destFilePath = outputDir.getAbsolutePath() +
                            file.getAbsolutePath().replace(srcDir.getAbsolutePath(), "");
                    File destFile = new File(destFilePath);
                    try {
                        switch (status) {
                            case REMOVED:
                                FileUtils.forceDelete(destFile);
                                break;
                            case ADDED:
                            case CHANGED:
                                // 将修改的文件复制到指定位置
                                FileUtils.copyFile(file, destFile);
                                // 获取文件的原始数据
                                byte[] sourceBytes = FileUtils.readFileToByteArray(destFile);
                                // 修改
                                byte[] modifiedBytes = handleBytes(sourceBytes);
                                // 如果修改了，就将新数据保存到原文件中
                                if (modifiedBytes != null) {
                                    FileUtils.writeByteArrayToFile(destFile, modifiedBytes, false);
                                }
                                break;
                            case NOTCHANGED:
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                FileUtils.copyDirectory(srcDir, outputDir);
                FileUtils.listFiles(outputDir,
                                new String[]{"class"},
                                true).parallelStream()
                        .forEach(clazzFile -> {
                            try {
                                byte[] sourceBytes = FileUtils.readFileToByteArray(clazzFile);
                                byte[] modifiedBytes = handleBytes(sourceBytes);
                                if (modifiedBytes != null) {
                                    FileUtils.writeByteArrayToFile(clazzFile, modifiedBytes, false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (Exception e) {
            System.out.println("=== transform_template processDirectoryFile error ===" + e);
        }
    }

    /**
     * 处理Jar包
     *
     * @param jarInput
     * @param transformInvocation
     */
    private void processJarFile(JarInput jarInput, TransformInvocation transformInvocation) {
        // 获取Jar包文件
        File file = jarInput.getFile();
        // 获取输出的目标文件
        File outputFile = transformInvocation.getOutputProvider()
                .getContentLocation(
                        file.getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);
        // 将数据复制到指定目录
        try {
            if (transformInvocation.isIncremental()) { // 增量编译
                switch (jarInput.getStatus()) {
                    case REMOVED:
                        FileUtils.forceDelete(outputFile);
                        break;
                    case CHANGED:
                    case ADDED:
                        File modifierJarFile
                                = modifierJar(file, transformInvocation.getContext().getTemporaryDir());
                        FileUtils.copyFile(modifierJarFile != null ? modifierJarFile : file, outputFile);
                        break;
                    case NOTCHANGED:
                        break;
                }
            } else { // 非
                File modifierJarFile
                        = modifierJar(file, transformInvocation.getContext().getTemporaryDir());
                FileUtils.copyFile(modifierJarFile != null ? modifierJarFile : file, outputFile);
            }
        } catch (Exception e) {
            System.out.println("=== transform_template processJarFile error ===" + e);
        }
    }

    /**
     * 对Jar包中的内容进行处理，通常处理Class文件
     *
     * @param file    Jar包对应的File
     * @param tempDir 输出临时文件的文件夹
     * @return 返回修改过的Jar包，如果返回值是null，表示未修改成功，在这种情况下直接复制原文件即可
     */
    private File modifierJar(File file, File tempDir) {
        if (file == null || file.length() == 0) return null;
        try {
            // 创建JarFile
            JarFile jarFile = new JarFile(file, false);
            // 为了防止重命名导致覆盖，取文件md5的前8位
            String tempNameHex = DigestUtils.md5Hex(file.getAbsolutePath()).substring(0, 8);

            File outputJarFile = new File(tempDir + tempNameHex + file.getName());
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJarFile));
            // 处理Jar包中的内容
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String entryName = jarEntry.getName();
                // 如果有签名文件，需要忽略
                if (entryName.startsWith(".DSA") || entryName.startsWith(".SF")) {
                    // do nothing
                } else {
                    // 创建一个新的entry，将修改后的内容放在此entry中
                    JarEntry outputEntry = new JarEntry(entryName);
                    // 开始写入一个新的Jar File Entry
                    jarOutputStream.putNextEntry(outputEntry);
                    // 获取对应entry的输入流，读取其中的字节数据，
                    try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        // 获取原entry数据
                        byte[] sourceBytes = toByteArrayAndAutoCloseStream(inputStream);
                        byte[] outputBytes = null;
                        // 判断是否是Class文件，如果是，就处理，可以用ASM、Javassist对它修改
                        if (!jarEntry.isDirectory() && entryName.endsWith(".class")) {
                            outputBytes = handleBytes(sourceBytes);
                        }
                        jarOutputStream.write(outputBytes == null ? sourceBytes : outputBytes);
                        // 结束写入当前的entry，可以开启下一个entry
                        jarOutputStream.closeEntry();
                    } catch (Exception e) {
                        System.out.println("=== transform_template getInputStream error ===" + e);
                        IOUtils.closeQuietly(jarFile);
                        IOUtils.closeQuietly(jarOutputStream);
                        return null;
                    }
                }
            }
            IOUtils.closeQuietly(jarFile);
            IOUtils.closeQuietly(jarOutputStream);
            return outputJarFile;

        } catch (Exception e) {
            System.out.println("=== transform_template modifierJar error ===" + e);
        }
        return null;
    }

    /**
     * 处理数据
     *
     * @param originData 原始数据
     * @return 修改后的数据
     */
    private byte[] handleBytes(byte[] originData) {
        return originData;
    }

    /**
     * 将输入流转换成byte数组
     *
     * @param input
     * @return
     */
    private byte[] toByteArrayAndAutoCloseStream(InputStream input) throws Exception {
        ByteArrayOutputStream output = null;
        try {
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 4];
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            output.flush();
            return output.toByteArray();
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
    }
}
