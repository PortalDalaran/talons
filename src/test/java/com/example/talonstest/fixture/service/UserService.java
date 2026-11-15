package com.example.talonstest.fixture.service;

import com.example.talonstest.fixture.entity.UserDO;
import com.example.talonstest.fixture.mapper.UserMapper;
import io.github.portaldalaran.talons.core.TalonsServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserService extends TalonsServiceImpl<UserMapper, UserDO> {
}
