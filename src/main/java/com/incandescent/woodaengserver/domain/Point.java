package com.incandescent.woodaengserver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point {
    private Long user_id;
    private int point;
    private String detail;
    private Timestamp time;
}
