package com.example.talonstest.fixture.service;

import com.example.talonstest.fixture.entity.AccountDO;
import com.example.talonstest.fixture.mapper.AccountMapper;
import io.github.portaldalaran.talons.core.TalonsServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AccountService extends TalonsServiceImpl<AccountMapper, AccountDO> {
}
