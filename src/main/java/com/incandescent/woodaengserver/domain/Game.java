package com.incandescent.woodaengserver.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    @EmbeddedId
    private String game_code;
    private int red_score;
}