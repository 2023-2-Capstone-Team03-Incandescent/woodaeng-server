package com.incandescent.woodaengserver.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.GeoIndexed;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("playerLocation")
public class PlayerLocation {
    private Long id;
    private int team;
    @GeoIndexed
    private Point location;
}
