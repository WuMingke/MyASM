package com.example.buildsrc;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class MyBuildSrcPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("My Second Plugin");

        Task task = project.task("getBuildDir2");
        task.doLast(task1 -> System.out.println("build dir2: " + project.getBuildDir()));
    }
}