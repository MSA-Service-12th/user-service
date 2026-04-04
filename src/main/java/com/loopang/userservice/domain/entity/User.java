package com.loopang.userservice.domain.entity;

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
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

@Entity
@Getter
@Builder
@Table(name = "p_user")
@SQLRestriction("deleted_at IS NULL")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUserEntity {

  @Id
  private UUID id;

  @Version
  private int version;

  @Column(length = 50, nullable = false)
  private String email;

  @Column(length = 50, nullable = false)
  private String name;

  @Column(length = 100, nullable = false)
  private String slackId;

  @Column(length = 10, nullable = false)
  @Enumerated(EnumType.STRING)
  private UserType role;

  @Embedded
  private HubInfo hubInfo;

  @Embedded
  private CompanyInfo companyInfo;

  private boolean approved;

  public boolean isEnabled() {
    return this.approved && this.getDeletedAt() == null;
  }

  public void update(String name, String slackId, UserType role, HubInfo hubInfo, CompanyInfo companyInfo, Boolean approved) {
    if (name != null) this.name = name;
    if (slackId != null) this.slackId = slackId;
    if (role != null) this.role = role;
    if (hubInfo != null) this.hubInfo = hubInfo;
    if (companyInfo != null) this.companyInfo = companyInfo;
    if (approved != null) this.approved = approved;
  }

  private void checkMasterId(UUID masterId) {
    if (!StringUtils.hasText(String.valueOf(masterId))) {
      throw new BadRequestException("관리자 아이디가 누락되었습니다.");
    }
  }

  public void delete(UUID masterId, RoleCheck roleCheck, IdentityProvider identityProvider) {
    if (this.getDeletedAt() != null) {
      return;
    }
    checkMasterId(masterId);
    checkMaster(roleCheck);
    super.delete(masterId);
    identityProvider.withdraw(id);
  }

  private void checkMaster(RoleCheck roleCheck) {
    if (!roleCheck.hasRole(MASTER)) {
      throw new ForbiddenException(MASTER.getDescription() + " 권한이 필요합니다.");
    }
  }
}
