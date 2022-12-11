package pt.ulisboa.tecnico.transparency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import pt.ulisboa.tecnico.transparency.certificaterequester.RequesterCommandLineRunner;

import java.util.List;

@SpringBootApplication
public class CertificateRequesterApplication {

  public static void main(String[] args) {
    SpringApplication.run(CertificateRequesterApplication.class, args);
  }

  @Bean
  RestTemplate restTemplate(ProtobufHttpMessageConverter hmc) {
    return new RestTemplate(List.of(hmc));
  }

  @Bean
  ProtobufHttpMessageConverter protobufHttpMessageConverter() {
    return new ProtobufHttpMessageConverter();
  }

  @Bean
  public RequesterCommandLineRunner schedulerRunner(RestTemplate restTemplate) {
    return new RequesterCommandLineRunner(restTemplate);
  }
}
