package com.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CountResult {

    /**
     * ファイルパス
     */
    private String filepath;

    /**
     * ファイルタイプ
     */
    private String type;

    /**
     * SHA-256ハッシュ
     */
    private String digest;
    /**
     * 実行行数
     */
    private int code;

    /**
     * コメント行数
     */
    private int comment;

    /**
     * 空白行数
     */
    private int blank;
    
    public CountResult(String filepath) {
        this.filepath = filepath;
    }

    public String toString(String delimiter) {
        return this.filepath + " : " +
            String.join(
                delimiter,
                this.type,
                String.valueOf(this.code),
                String.valueOf(this.blank),
                String.valueOf(this.comment),
                String.valueOf(this.code + this.blank + this.comment)
            );
    }
}
