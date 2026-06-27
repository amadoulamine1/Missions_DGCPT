package sn.dgcpt.missionsparc.compte;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Verrou anti-force-brute : seuil d'échecs, réinitialisation au succès, clé insensible à la casse. */
class LoginAttemptServiceTest {

    @Test
    void verrouille_apres_le_seuil_d_echecs() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_ECHECS; i++) {
            assertThat(s.estVerrouille("admin")).as("non verrouillé sous le seuil").isFalse();
            s.echec("admin");
        }
        assertThat(s.estVerrouille("admin")).as("verrouillé au seuil").isTrue();
    }

    @Test
    void un_succes_reinitialise_le_compteur() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_ECHECS; i++) s.echec("u1");
        assertThat(s.estVerrouille("u1")).isTrue();

        s.succes("u1");

        assertThat(s.estVerrouille("u1")).isFalse();
    }

    @Test
    void la_cle_est_insensible_a_la_casse_et_aux_espaces() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_ECHECS; i++) s.echec("Admin");
        assertThat(s.estVerrouille("  admin ")).isTrue();
    }
}
