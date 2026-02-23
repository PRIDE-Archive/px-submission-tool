package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceFactoryTest {

    @AfterEach
    void tearDown() {
        ServiceFactory.resetInstance();
    }

    @Test
    void getInstanceReturnsNonNull() {
        assertThat(ServiceFactory.getInstance()).isNotNull();
    }

    @Test
    void getInstanceReturnsSameInstance() {
        ServiceFactory first = ServiceFactory.getInstance();
        ServiceFactory second = ServiceFactory.getInstance();

        assertThat(first).isSameAs(second);
    }

    @Test
    void setInstanceReplacesDefault() {
        ServiceFactory original = ServiceFactory.getInstance();
        ServiceFactory custom = new ServiceFactory();

        ServiceFactory.setInstance(custom);

        assertThat(ServiceFactory.getInstance()).isSameAs(custom);
        assertThat(ServiceFactory.getInstance()).isNotSameAs(original);
    }

    @Test
    void resetInstanceRestoresFreshInstance() {
        ServiceFactory original = ServiceFactory.getInstance();
        ServiceFactory.setInstance(new ServiceFactory());

        ServiceFactory.resetInstance();

        assertThat(ServiceFactory.getInstance()).isNotSameAs(original);
    }

    @Test
    void createApiServiceReturnsNonNull() {
        ApiService service = ServiceFactory.getInstance().createApiService("user", "pass");

        assertThat(service).isNotNull();
        service.shutdown();
    }

    @Test
    void createApiServiceWithDifferentCredentials() {
        ApiService service1 = ServiceFactory.getInstance().createApiService("user1", "pass1");
        ApiService service2 = ServiceFactory.getInstance().createApiService("user2", "pass2");

        assertThat(service1).isNotSameAs(service2);
        service1.shutdown();
        service2.shutdown();
    }

    @Test
    void resetInstanceCreatesNewInstance() {
        ServiceFactory first = ServiceFactory.getInstance();
        ServiceFactory.resetInstance();
        ServiceFactory afterReset = ServiceFactory.getInstance();

        assertThat(afterReset).isNotSameAs(first);
    }
}
