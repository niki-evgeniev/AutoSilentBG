package nevg.autosilent.AppConfiguration;


import nevg.autosilent.Repository.UserRepository;
import nevg.autosilent.Service.Impl.ShopUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SpringSecurity {

    private final String rememberMeKey;

    public SpringSecurity(@Value("${nirton.remember.me.key}")
                          String rememberMeKey) {
        this.rememberMeKey = rememberMeKey;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity.authorizeHttpRequests(
                authorizeRequest -> authorizeRequest
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/css/**", "/fonts/**", "/images/**", "/js/**").permitAll()
                        .requestMatchers("/ProductImages/**").permitAll()
                        .requestMatchers("/robots.txt").permitAll()
                        .requestMatchers("/admin/orders/**").hasAnyRole("ADMIN", "MODERATOR")
                        .requestMatchers("/admin/inquiries/**").hasAnyRole("ADMIN", "MODERATOR")
                        .requestMatchers("/admin/ip-addresses/**").hasRole("ADMIN")
                        .requestMatchers("/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/favorites/**").authenticated()
                        .requestMatchers("/products/add", "/products/*/edit", "/products/*/seo").hasAnyRole("ADMIN", "MODERATOR")
                        .requestMatchers("/products/*/delete").hasRole("ADMIN")
                        .requestMatchers("/", "/cart", "/contact", "/contact/success", "/user/sign_in", "/orders/checkout", "/orders/cart",
                                "/orders/quick", "/orders/success/*",
                                "/api/***", "/user/sign_up",
                                "/users/login-error", "/users/logout",
                                "/products", "/products/*").permitAll()
                        .requestMatchers("/imagesApp/**").permitAll()
                        .anyRequest().authenticated()
        ).formLogin(
                formLogin -> {
                    formLogin
                            .loginPage("/user/sign_in")
                            .loginProcessingUrl("/users/login")
                            .usernameParameter("email")
                            .passwordParameter("password")
                            .defaultSuccessUrl("/", true)
                            .failureUrl("/user/sign_in?error");
                }
        ).logout(logout -> logout
                .logoutUrl("/users/logout")
                .logoutSuccessUrl("/user/sign_in?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        ).rememberMe(
                rememberMe -> {
                    rememberMe
                            .key(rememberMeKey)
                            .rememberMeParameter("remember-me")
                            .rememberMeCookieName("remember-me");
                }
        );


        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new ShopUserService(userRepository);
    }

}
