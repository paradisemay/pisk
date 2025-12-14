package ru.ifmo.se.pisk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class UserTrackKey implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "track_id")
    private Long trackId;
}
