package io.github.portaldalaran.talons.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.github.portaldalaran.talons.core.TalonsAssociationQuery;
import io.github.portaldalaran.talons.core.TalonsAssociationService;
import io.github.portaldalaran.talons.core.TalonsHelper;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author aohee@163.com
 */
@Configuration
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ComponentScan(value = "io.github.portaldalaran.talons")
public class TalonsConfiguration implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Bean
    public TalonsAssociationQuery talonsAssociationQuery(ObjectFactory<SqlSession> factory) {
        TalonsAssociationQuery talonsAssociationQuery = new TalonsAssociationQuery();
        talonsAssociationQuery.setFactory(factory);
        return talonsAssociationQuery;
    }

    @Bean
    public TalonsAssociationService talonsAssociationService(ObjectFactory<SqlSession> factory) {
        TalonsAssociationService talonsAssociationService = new TalonsAssociationService();
        talonsAssociationService.setFactory(factory);
        return talonsAssociationService;
    }

    @Bean
    public TalonsHelper talonsHelper(ObjectFactory<SqlSession> factory) {
        TalonsHelper talonsHelper = new TalonsHelper();
        talonsHelper.setTalonsAssociationQuery(talonsAssociationQuery(factory));
        talonsHelper.setTalonsAssociationService(talonsAssociationService(factory));
        return talonsHelper;
    }
}
