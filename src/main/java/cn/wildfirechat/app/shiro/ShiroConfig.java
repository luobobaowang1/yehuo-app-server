package cn.wildfirechat.app.shiro;


import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.ShiroHttpSession;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    @Autowired
    DBSessionDao dbSessionDao;

    @Value("${wfc.all_client_support_ssl}")
    private boolean All_Client_Support_SSL;

    @Bean(name = "shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        shiroFilterFactoryBean.setLoginUrl("/login");
        shiroFilterFactoryBean.setUnauthorizedUrl("/notRole");
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        // <!-- authc:所有url都必须认证通过才可以访问; anon:所有url都都可以匿名访问-->
        filterChainDefinitionMap.put("/send_code", "anon");
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/login1", "anon");
        filterChainDefinitionMap.put("/daksjh312k3jhk12j/3kjh12k3681273/312kjh3","anon");
        filterChainDefinitionMap.put("/dajs1hkdhj/daj1hsgdsa/dajshkdashjd1","anon");
        filterChainDefinitionMap.put("/3kjh12k3gk12/1kjh312k3u17/kj3h12kh3","anon");
        filterChainDefinitionMap.put("/dsakjh123987jdashg38167/xdasj123786s/1h1", "anon");
        filterChainDefinitionMap.put("/da13kjh12k3haksdjl/dkajshd/131ng31", "anon");
        filterChainDefinitionMap.put("/dalskjdl/312o3ijkjek123/31o23kjj12h3","anon");
        filterChainDefinitionMap.put("/312kj3h312kl/1243kjhk/3jk12h3k1","anon");
        filterChainDefinitionMap.put("/381923hg1j23jg1/assd1j23hg/dan13","anon");
        filterChainDefinitionMap.put("/da13jh1/31k2j3hk1/31j2g312g","anon");
        filterChainDefinitionMap.put("/dasdjkhajsjkd/3j1h2kj3k1/b31jhg2","anon");
        filterChainDefinitionMap.put("/da13jh1/ldakhsd371/31j2312j3gj1g3jg312g","anon");
        filterChainDefinitionMap.put("/register", "anon");
        filterChainDefinitionMap.put("/pc_session", "anon");
        filterChainDefinitionMap.put("/amr2mp3", "anon");

        filterChainDefinitionMap.put("/session_login/**", "anon");
        filterChainDefinitionMap.put("/user/online_event", "anon");
        filterChainDefinitionMap.put("/logs/**", "anon");
        filterChainDefinitionMap.put("/im_event/**", "anon");
        filterChainDefinitionMap.put("/", "anon");

        filterChainDefinitionMap.put("/confirm_pc", "login");
        filterChainDefinitionMap.put("/cancel_pc", "login");
        filterChainDefinitionMap.put("/scan_pc/**", "login");
        filterChainDefinitionMap.put("/put_group_announcement", "login");
        filterChainDefinitionMap.put("/get_group_announcement", "login");
        filterChainDefinitionMap.put("/things/add_device", "login");
        filterChainDefinitionMap.put("/things/list_device", "login");


        //主要这行代码必须放在所有权限设置的最后，不然会导致所有 url 都被拦截 剩余的都需要认证
        filterChainDefinitionMap.put("/**", "login");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        shiroFilterFactoryBean.getFilters().put("login", new JsonAuthLoginFilter());
        return shiroFilterFactoryBean;

    }

    @Bean
    public SecurityManager securityManager() {
        DefaultWebSecurityManager defaultSecurityManager = new DefaultWebSecurityManager();
        defaultSecurityManager.setRealms(Arrays.asList(phoneCodeRealm, scanCodeRealm));
        ShiroSessionManager sessionManager = new ShiroSessionManager();
        sessionManager.setGlobalSessionTimeout(Long.MAX_VALUE);
        sessionManager.setSessionDAO(dbSessionDao);

        Cookie cookie = new SimpleCookie(ShiroHttpSession.DEFAULT_SESSION_ID_NAME);
        if (All_Client_Support_SSL) {
            cookie.setSameSite(Cookie.SameSiteOptions.NONE);
            cookie.setSecure(true);
        } else {
            cookie.setSameSite(null);
        }
        cookie.setMaxAge(Integer.MAX_VALUE);
        sessionManager.setSessionIdCookie(cookie);
        sessionManager.setSessionIdCookieEnabled(true);
        sessionManager.setSessionIdUrlRewritingEnabled(true);

        defaultSecurityManager.setSessionManager(sessionManager);
        SecurityUtils.setSecurityManager(defaultSecurityManager);
        return defaultSecurityManager;
    }


    @Autowired
    private PhoneCodeRealm phoneCodeRealm;

    @Autowired
    private ScanCodeRealm scanCodeRealm;

}