package com.example.buildsrc;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.SecondaryFile;
import com.android.build.api.transform.SecondaryInput;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;
import com.google.common.collect.ImmutableList;


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class MyTransform extends Transform {
    @Override
    public String getName() {
        return "chapter2_01";
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
    public boolean isCacheable() {
        return super.isCacheable();
    }

    @Override
    public Collection<SecondaryFile> getSecondaryFiles() {
        File file = new File("/Users/wuqinglie/AndroidStudioProjects/MyASM/buildSrc/src/main/java/com/example/buildsrc/log.txt");
        System.out.println("=====secondaryInputs===file==" + file.length());
        return ImmutableList.of(SecondaryFile.incremental(file));
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        System.out.println("======start transform=========");
//        super.transform(transformInvocation);

        // 获取需要消费的数据
        Collection<TransformInput> inputCollection = transformInvocation.getInputs();
        // 遍历数据
        inputCollection.parallelStream().forEach((TransformInput transformInput) -> {
            // 1 获取JAR包类型的输入
            Collection<JarInput> jarInputCollection = transformInput.getJarInputs();

            jarInputCollection.parallelStream().forEach(jarInput -> {
                // 获取JAR包文件
                File file = jarInput.getFile();
                // 获取输出的目标文件
                File outputFile = transformInvocation.getOutputProvider()
                        .getContentLocation(file.getAbsolutePath(),
                                jarInput.getContentTypes(),
                                jarInput.getScopes(),
                                Format.JAR);
                try {
                    // 将数据复制到指定目录
                    FileUtils.copyFile(file, outputFile);
                } catch (IOException e) {
                    System.out.println("======异常1=========" + e);
                }
            });

            // 2 获取源码编译的文件夹输入
            Collection<DirectoryInput> directoryInputCollection = transformInput.getDirectoryInputs();

            directoryInputCollection.parallelStream().forEach(directoryInput -> {
                // 获取源码编译后对应的文件夹
                File file = directoryInput.getFile();
                // 获取输出的目标
                File outputFile = transformInvocation.getOutputProvider()
                        .getContentLocation(file.getAbsolutePath(),
                                directoryInput.getContentTypes(),
                                directoryInput.getScopes(),
                                Format.DIRECTORY);
                try {
                    // 将数据复制到指定目录，outputFile不存在，需要创建
                    FileUtils.mkdirs(outputFile);
                    FileUtils.copyDirectory(file, outputFile);
                } catch (IOException e) {
                    System.out.println("======异常2=========" + e);
                }
            });
        });

        Collection<SecondaryInput> secondaryInputs = transformInvocation.getSecondaryInputs();
        System.out.println("=====secondaryInputs===size==" + secondaryInputs.size());
        secondaryInputs.forEach(input -> {
            SecondaryFile secondaryFile = input.getSecondaryInput();
            Status status = input.getStatus();
            System.out.println("=====secondaryInputs===getFile==" + secondaryFile.getFile());
            System.out.println("=====secondaryInputs===supportsIncrementalBuild==" + secondaryFile.supportsIncrementalBuild());
            System.out.println("=====secondaryInputs===status==" + status.name());

        });
    }
}

/**
 * > Task :app:transformClassesWithChapter2_01ForDebug
 * ======start transform=========
 */
