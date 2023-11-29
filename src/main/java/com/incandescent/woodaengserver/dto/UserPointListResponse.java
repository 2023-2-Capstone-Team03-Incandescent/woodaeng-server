package com.incandescent.woodaengserver.dto;

import com.incandescent.woodaengserver.domain.Point;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPointListResponse {
    private Long id;
    private List<Point> pointList;
}