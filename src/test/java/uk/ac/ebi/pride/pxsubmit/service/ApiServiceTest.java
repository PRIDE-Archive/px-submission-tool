package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ApiServiceTest {

    @Test
    void basicAuthHeaderEncoding() throws Exception {
        String header = invokeCreateBasicAuthHeader("user", "pass");
        String expected = "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes());

        assertThat(header).isEqualTo(expected);
    }

    @Test
    void basicAuthHeaderWithSpecialChars() throws Exception {
        String header = invokeCreateBasicAuthHeader("user@example.com", "p@ss!w0rd#$%");
        String expected = "Basic " + Base64.getEncoder().encodeToString("user@example.com:p@ss!w0rd#$%".getBytes());

        assertThat(header).isEqualTo(expected);
    }

    @Test
    void basicAuthHeaderWithEmptyPassword() throws Exception {
        String header = invokeCreateBasicAuthHeader("user", "");
        String expected = "Basic " + Base64.getEncoder().encodeToString("user:".getBytes());

        assertThat(header).isEqualTo(expected);
    }

    @Test
    void apiExceptionWithMessage() {
        ApiService.ApiException ex = new ApiService.ApiException("test error");

        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void apiExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root cause");
        ApiService.ApiException ex = new ApiService.ApiException("wrapped", cause);

        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void constructorDoesNotThrow() {
        assertThatNoException().isThrownBy(() -> {
            ApiService service = new ApiService("user", "pass");
            service.shutdown();
        });
    }

    @Test
    void shutdownDoesNotThrow() {
        ApiService service = new ApiService("user", "pass");
        assertThatNoException().isThrownBy(service::shutdown);
    }

    private String invokeCreateBasicAuthHeader(String username, String password) throws Exception {
        Method method = ApiService.class.getDeclaredMethod("createBasicAuthHeader", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, username, password);
    }
}
