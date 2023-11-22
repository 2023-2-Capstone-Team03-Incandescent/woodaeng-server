package com.incandescent.woodaengserver.domain;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class BallId implements Serializable {
    private int ballId;
    private String game_code;
}
