package com.incandescent.woodaengserver.dto.game;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BallLocation {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ballId;
    private Double latitude;
    private Double longitude;

    public BallLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
