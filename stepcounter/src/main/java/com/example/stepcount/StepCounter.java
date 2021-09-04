package com.example.stepcount;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.model.CountResult;

public class StepCounter {

    private String[] excludes = {".git", "target", ".settings", ".classpath", ".factorypath", ".project", ".devcontainer"};

    private static final String BLANK = "";
    private static final String ONELINE_COMMENT = "//";
    private static final String MULTILINE_COMMENT_START = "/*";
    private static final String MULTILINE_COMMENT_END = "*/";

    private static final int TYPE_CODE = 0;
    private static final int TYPE_BLANK = 1;
    private static final int TYPE_ONELINE_COMMENT = 2;
    private static final int TYPE_MULTILINE_COMMENT_START = 3;
    private static final int TYPE_MULTILINE_COMMENT_END = 4;

    public static StepCounter builder() {
        return new StepCounter();
    }

    public StepCounter setExcludes(String[] excludes) {
        this.excludes = excludes;
        return this;
    }

    public List<CountResult> deepCount(File file) {
        if (Arrays.asList(excludes).contains(file.getName())) {
            return Collections.emptyList();
        }

        if (file.isDirectory()) {
            return Stream.of(file.listFiles()).flatMap(f -> deepCount(f).stream()).collect(Collectors.toList());
        }

        Optional<CountResult> result = count(file);
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(result.get());

    }

    private Optional<CountResult> count(File file) {
        if (!file.exists()) {
            return Optional.empty();
        }

        int code = 0;
        int blank = 0;
        int comment = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = null;
            boolean isAreaComment = false;

            while ((line = br.readLine()) != null) {
                switch (checkType(line.trim(), isAreaComment)) {
                    case TYPE_BLANK:
                        blank++;
                        break;
                    case TYPE_ONELINE_COMMENT:
                        comment++;
                        break;
                    case TYPE_MULTILINE_COMMENT_START:
                        comment++;
                        isAreaComment = true;
                        break;
                    case TYPE_MULTILINE_COMMENT_END:
                        comment++;
                        isAreaComment = false;
                        break;
                    default:
                        code++;
                }
            }

        } catch (IOException e) {
            return Optional.empty();
        }

        CountResult result = new CountResult(file.getPath());
        result.setType(file.getName().replaceFirst("^.*\\.", ""));
        result.setBlank(blank);
        result.setComment(comment);
        result.setCode(code);

        return Optional.of(result);
    }

    private int checkType(String line, boolean isAreaComment) {
        // 複数行数のコメント中に複数行数コメントの終端記号で終わった場合
        if (isAreaComment && line.endsWith(MULTILINE_COMMENT_END)) return TYPE_MULTILINE_COMMENT_END;
        // 複数行数コメント中はコメントの終端記号が含まれない限りコメント
        if (isAreaComment && !line.contains(MULTILINE_COMMENT_END)) return TYPE_ONELINE_COMMENT;
        // スペースとタブ以外に何もなければ空行
        if (BLANK.equals(line)) return TYPE_BLANK;
        // 単行コメント記号で始まっていればコメント
        if (line.startsWith(ONELINE_COMMENT)) return TYPE_ONELINE_COMMENT;
        if (line.startsWith(MULTILINE_COMMENT_START)) {
            if (line.endsWith(MULTILINE_COMMENT_END)) {
                // 複数行数コメントの開始記号で始まって、終端記号で終わっていれば単行コメント
                return TYPE_ONELINE_COMMENT;
            } else {
                // 複数行数コメントの開始記号で始まって、終端記号で終わらなければ複数行数コメントの始まり
                return TYPE_MULTILINE_COMMENT_START;
            }
        }
        // 空行、コメントのどちらでもないときは実行行
        return TYPE_CODE;
    }
}
