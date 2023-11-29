package com.incandescent.woodaengserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ranking {
    Long id;
    int rank;
    String image;
    String nickname;
    int win_cnt;
}
