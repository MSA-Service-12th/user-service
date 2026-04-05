package com.loopang.userservice.domain.service;

import com.loopang.userservice.domain.vo.CompanyInfo;
import java.util.UUID;

public interface CompanyProvider {

  CompanyInfo get(UUID storeId);
}