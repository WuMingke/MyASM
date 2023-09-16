package com.example.buildsrc;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;


public class TestTransformPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        AppExtension extension = project.getExtensions().findByType(AppExtension.class);
        if (extension != null) {
            extension.registerTransform(new MyTransform());
        }
    }
}
