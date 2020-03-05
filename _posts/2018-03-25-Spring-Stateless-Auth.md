---
layout: post
title: Spring Stateless Authentication
---

Spring is a popular collection of frameworks that facilitate server side application development in Java. In the old
days you had to string together a bunch of their frameworks together to build a single server side application. You
needed Spring Core for dependency injection and management Spring MVC for the web framework, and then Spring Data for
quick integration with your preferred ORM access, etc.

Now however you can just use [Spring Boot](https://projects.spring.io/spring-boot/) now. It's basically a package deal
of Spring frameworks with sensible defaults.

Spring ecosystem is thriving and full of libraries to handle most things, however from time to time a few features
are missing and you will need to roll your own.

This article doesn't try to get you started with Spring Boot, rather it assumes that you are already familiar with
Spring basics.

# Basic Authentication
We will use Spring Security framework and JWT to secure our application.

First we need to add Spring Security to our project. We can do this by adding the Spring Security dependency to
our `build.gradle` file.

```
compile 'org.springframework.security:spring-security-web:4.2.5.RELEASE'
```

Now the dependency has been declared we can create a configuration file. We will use a Java file to configure our
security.

```Java
package com.karma.conf;

import com.karma.filters.JWTAuthenticationFilter;
import com.karma.services.TokenAuthenticationService;
import com.karma.services.UserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Created by thihara
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private TokenAuthenticationService tokenAuthenticationService;

    @Autowired
    private UserAuthService userService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.POST, "/users/signin").permitAll()
                .antMatchers(HttpMethod.POST, "/users/signup").permitAll()
                .antMatchers(HttpMethod.GET, "/connect/**").permitAll()
                .antMatchers(HttpMethod.POST, "/connect/**").permitAll()
                .antMatchers(HttpMethod.GET, "/auth/**").permitAll()
                .antMatchers(HttpMethod.POST, "/auth/**").permitAll()
                .antMatchers(HttpMethod.GET, "/signin/**").permitAll()
                .antMatchers(HttpMethod.POST, "/signin/**").permitAll()
                .antMatchers(HttpMethod.GET, "/signup/**").permitAll()
                .antMatchers(HttpMethod.POST, "/signup/**").permitAll()
                .anyRequest().authenticated()
                .and()
                // And filter other requests to check the presence of JWT in header
                .addFilterBefore(new JWTAuthenticationFilter(tokenAuthenticationService),
                        UsernamePasswordAuthenticationFilter.class);
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder;
    }
}
```

Here we are defining BCryptPasswordEncoder as our password encoder which uses the [Bcrypt Algorithm](https://en.wikipedia.org/wiki/Bcrypt)
to hash our passwords.

