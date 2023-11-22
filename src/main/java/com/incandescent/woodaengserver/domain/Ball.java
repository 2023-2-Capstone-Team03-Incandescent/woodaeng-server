package com.incandescent.woodaengserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ball {
    @EmbeddedId
    private int ballId;
    private double latitude;
    private double longitude;
    private int color;
}

