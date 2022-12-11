package pt.ulisboa.tecnico.transparency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import pt.ulisboa.tecnico.transparency.verifier.JWTAuthorizationFilter;

import java.util.List;

@SpringBootApplication
public class VerifierApplication {

  public static void main(String[] args) {
    SpringApplication.run(VerifierApplication.class, args);
  }

  @Bean
  ProtobufHttpMessageConverter protobufHttpMessageConverter() {
    return new ProtobufHttpMessageConverter();
  }

  @Bean
  RestTemplate restTemplate(ProtobufHttpMessageConverter hmc) {
    return new RestTemplateBuilder()
        .errorHandler(new RestTemplateErrorHandler())
        .messageConverters(List.of(hmc))
        .build();
  }

  @EnableWebSecurity
  @Configuration
  static class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf()
          .disable()
          .addFilterAfter(new JWTAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
              .authorizeRequests()
          .antMatchers(HttpMethod.POST, "/v2/user/**")
          .permitAll()
          .anyRequest()
          .authenticated();
    }
  }
}
