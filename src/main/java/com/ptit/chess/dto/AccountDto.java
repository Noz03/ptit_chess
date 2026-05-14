package com.ptit.chess.dto;

import com.ptit.chess.entity.AccountStatus;
import com.ptit.chess.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountDto {
    private Long id;
    private String username;
    private Role role;
    private AccountStatus status;
    private String displayName;
    private Integer elo;
    private Integer winCount;
    private Integer drawCount;
    private Integer lossCount;
}
