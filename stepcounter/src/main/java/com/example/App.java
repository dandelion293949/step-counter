package com.example;

import java.io.File;
import java.util.List;

import com.example.diffcount.DiffCounter;
import com.example.diffcount.DiffCounter.FileInfo;
import com.example.model.CountResult;
import com.example.stepcount.StepCounter;


/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) {
        String[] excludes = {".git", "target", ".settings", ".classpath", ".factorypath", ".project"};
        System.out.println("==== ステップカウント =====");
        StepCounter stepCounter = StepCounter.builder().setExcludes(excludes);
        // stepCounter.setExcludes({".git", "target", ".settings", ".classpath", ".factorypath", ".project", ".devcontainer"});
        List<CountResult> results = stepCounter.deepCount(new File(".."));
        results.stream()
            .sorted((s1, s2) -> s1.getFilepath().compareTo(s2.getFilepath()))
            .forEach(r -> {
                System.out.println(r.toString(", "));
            });

        System.out.println("==== 差分チェック =====");
        DiffCounter diffCounter = DiffCounter.builder();
        List<FileInfo> diff = diffCounter.deepDiff(new File("..\\..\\cli\\cli"), new File("cli"));
        diff.stream()
            .filter(d -> d.getStatus() != 0)
            .sorted((s1, s2) -> s1.getPath().compareTo(s2.getPath()))
            .forEach(d -> {
                System.out.println(d.toString(", "));
            });
    }
}
