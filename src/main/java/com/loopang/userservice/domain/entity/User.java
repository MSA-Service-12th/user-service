package com.loopang.userservice.domain.entity;

/*
 * 사용자 도메인
 *
 *
 *
 */

import static com.loopang.userservice.domain.vo.UserType.MASTER;

import com.loopang.common.domain.BaseUserEntity;
import com.loopang.common.exception.BadRequestException;
import com.loopang.common.exception.ForbiddenException;
import com.loopang.userservice.domain.service.IdentityProvider;
import com.loopang.userservice.domain.service.RoleCheck;
import com.loopang.userservice.domain.vo.CompanyInfo;
import com.loopang.userservice.domain.vo.HubInfo;
import com.loopang.userservice.domain.vo.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Getter
@Builder
@Table(name = "p_user")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUserEntity {

  @Id
  private UUID id;

  @Column(length = 50, nullable = false)
  private String email;

  @Column(length = 5, nullable = false)
  private String name;

  @Column(length = 30, nullable = false)
  private String slackId;

  @Column(length = 10, nullable = false)
  @Enumerated(EnumType.STRING)
  private UserType role;

  @Embedded
  private HubInfo hubInfo;

  @Embedded
  private CompanyInfo companyInfo;

  private boolean approved;

  // 활성화 사용자 여부(재직중 직원 여부)
  public boolean isEnabled() {
    return this.approved && this.getDeletedAt() == null;
  }

  // MASTER 관리자 ID 체크
  private void checkMasterId(UUID masterId) {
    if (!StringUtils.hasText(String.valueOf(masterId))) {
      throw new BadRequestException("관리자 아이디가 누락되었습니다.");
    }
  }

  // 직원 퇴사시 soft delete 삭제, MASTER 관리자만 가능
  public void delete(UUID masterId, RoleCheck roleCheck, IdentityProvider identityProvider) {
    // 이미 탈퇴한 경우 처리하지 않음
    if (this.getDeletedAt() != null) {
      return;
    }

    checkMasterId(masterId);
    checkMaster(roleCheck);
    super.delete(masterId);

    identityProvider.withdraw(id);
  }

  // MASTER 권한 체크
  private void checkMaster(RoleCheck roleCheck) {
    if (!roleCheck.hasRole(MASTER)) {
      throw new ForbiddenException(MASTER.getDescription() + " 권한이 필요합니다.");
    }
  }
}