package com.loopang.userservice.domain.vo;

import com.loopang.common.exception.BadRequestException;
import com.loopang.userservice.domain.service.CompanyProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInfo {

  @Column(name = "company_id")
  private UUID companyId;

  @Column(length = 100, name = "company_name")
  private String companyName;

  protected CompanyInfo(UUID id, CompanyProvider companyProvider) {

    if (id == null || companyProvider == null) {
      throw new BadRequestException("소속 업체 등록/수정을 위한 필수 항목이 누락되었습니다.");
    }

    CompanyInfo company = companyProvider.get(id);
    if (company == null) {
      throw new BadRequestException("소속 업체 등록/수정을 위한 필수 항목이 누락되었습니다.");
    }

    this.companyId = id;
    this.companyName = company.getCompanyName();
  }
}