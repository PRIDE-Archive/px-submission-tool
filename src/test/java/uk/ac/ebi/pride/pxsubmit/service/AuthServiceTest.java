package uk.ac.ebi.pride.pxsubmit.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {

    @Test
    void detectsLegacyIncomingTicketPendingMessage() {
        assertThat(AuthService.isPendingIncomingTicketMessage("incoming ticket pending")).isTrue();
    }

    @Test
    void detectsPendingIncomingTicketMessage() {
        assertThat(AuthService.isPendingIncomingTicketMessage("Pending incoming ticket available")).isTrue();
    }

    @Test
    void detectsAlreadyInProgressTicketMessage() {
        assertThat(AuthService.isPendingIncomingTicketMessage("You already have an in-progress ticket")).isTrue();
    }

    @Test
    void detectsPendingTicketMessageInsideHttpErrorText() {
        assertThat(AuthService.isPendingIncomingTicketMessage("400 Bad Request: \"incoming ticket pending\"")).isTrue();
    }

    @Test
    void ignoresSuccessfulLoginInfoMessage() {
        assertThat(AuthService.isPendingIncomingTicketMessage("Login successful")).isFalse();
    }

    @Test
    void ignoresExplicitNoPendingTicketMessage() {
        assertThat(AuthService.isPendingIncomingTicketMessage("No pending incoming ticket")).isFalse();
    }
}
