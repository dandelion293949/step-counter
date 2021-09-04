package com.example.diffcount;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Data;

public class DiffCounter {

    private String[] excludes = {".git", "target", ".settings", ".classpath", ".factorypath", ".project", ".devcontainer"};

    private static final int FILE_STATUS_SKIPED = 0;
    private static final int FILE_STATUS_CREATED = 1;
    private static final int FILE_STATUS_UPDATED = 2;
    private static final int FILE_STATUS_DELETED = 3;

    public static DiffCounter builder() {
        return new DiffCounter();
    }

    public DiffCounter setExcludes(String[] excludes) {
        this.excludes = excludes;
        return this;
    }

    public List<FileInfo> deepDiff(File oldSource, File newSource) {
        Map<String, FileInfo> newSourceMap = new HashMap<>();
        if (newSource.isDirectory()) {
            newSourceMap = Stream.of(newSource.listFiles()).flatMap(f -> get(f, newSource.getPath()).stream()).map(f -> {
                f.setStatus(FILE_STATUS_CREATED);
                return f;
            }).collect(Collectors.toMap(FileInfo::getPath, f -> f));
        } else {
            newSourceMap.putAll(get(newSource, newSource.getPath()).stream().map(f -> {
                f.setStatus(FILE_STATUS_CREATED);
                return f;
            }).collect(Collectors.toMap(FileInfo::getPath, f -> f)));
        }

        Map<String, FileInfo> oldSourceMap = new HashMap<>();
        if (oldSource.isDirectory()) {
            oldSourceMap.putAll(Stream.of(oldSource.listFiles()).flatMap(f -> get(f, oldSource.getPath()).stream()).map(f -> {
                f.setStatus(FILE_STATUS_DELETED);
                return f;
            }).collect(Collectors.toMap(FileInfo::getPath, f -> f)));
        } else {
            oldSourceMap.putAll(get(oldSource, oldSource.getPath()).stream().map(f -> {
                f.setStatus(FILE_STATUS_DELETED);
                return f;
            }).collect(Collectors.toMap(FileInfo::getPath, f -> f)));
        }

        for (Entry<String, FileInfo> old : oldSourceMap.entrySet()) {
            newSourceMap.merge(old.getKey(), old.getValue(), (v1, v2) -> {
                if (v1.getDigest().equals(v2.getDigest())) {
                    v1.setStatus(FILE_STATUS_SKIPED);
                    return v1;
                }
                v1.setStatus(FILE_STATUS_UPDATED);
                return v1;
            });
        }
        return newSourceMap.entrySet().stream().map(f -> f.getValue()).collect(Collectors.toList());

    }

    private List<FileInfo> get(File file, String rootPath) {
        if (Arrays.asList(excludes).contains(file.getName())) {
            return Collections.emptyList();
        }

        if (file.isDirectory()) {
            return Stream.of(file.listFiles()).flatMap(f -> get(f, rootPath).stream()).collect(Collectors.toList());
        }

        FileInfo info = new FileInfo();
        info.setPath(file.getPath().substring(rootPath.length()+1));
        info.setFilepath(file.getPath());
        info.setDigest(getHash(file));

        return List.of(info);
    }

    private String getHash(File file) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            return "";
        }

        try (DigestInputStream dis = new DigestInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())), md)) {
            while (dis.read() != -1) {}
        } catch (IOException e) {
            return "";
        }
        byte[] hash = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b: hash) {
            String hex = String.format("%02x", b);
            sb.append(hex);
        }
        return sb.toString();

    }

    @Data
    public static class FileInfo {
        private String path;
        private String filepath;
        private String digest;
        private int status;

        private static final String[] STATUS_MESSAGES = {"SKIPED", "CREATED", "UPDATED", "DELETED"};

        public String toString(String delimiter) {
            return this.path + " : " +
            String.join(
                delimiter,
                STATUS_MESSAGES[this.status]
            );
        }
    }

}
