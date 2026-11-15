package com.example.talonstest.fixture.service;

import com.example.talonstest.fixture.entity.ArticleDO;
import com.example.talonstest.fixture.mapper.ArticleMapper;
import io.github.portaldalaran.talons.core.TalonsServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ArticleService extends TalonsServiceImpl<ArticleMapper, ArticleDO> {
}
